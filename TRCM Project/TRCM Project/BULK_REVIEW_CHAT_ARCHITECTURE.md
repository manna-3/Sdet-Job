# Bulk Review Chat — Architecture

## Overview

The bulk review chat feature provides two purpose-built chat modes for interrogating
the results of a `/review-claims-bulk` run. Each mode carries a context payload sized
exactly for the questions it needs to answer — no server-side storage, no vector DB,
no Redis. The full bulk response (including parsed EDI fields) already lives in React
state after the review completes; the frontend selects and shapes the right subset
before each chat turn.

---

## Two Chat Modes

### Mode 1 — Per-Claim Chat

| | |
|---|---|
| **Triggered by** | "Ask about this claim" button on a specific row, or claim picker via context chooser |
| **Thread ID** | `{batch_id}_{clm_id}` |
| **Context size** | ~2–9 KB |

**Context shape — single raw row, no slimming:**

```json
{
  "filename": "claims_jan.edi",
  "clm_id": "CLM001",
  "edi_fields": {
    "billing_npi": "...",
    "payer_id": "...",
    "place_of_service": "11",
    "claim_dos": "2024-01-15",
    "diagnosis_codes": ["Z00000", "J0001"],
    "service_lines": [
      { "procedure_code": "99213", "modifiers": ["25"], "charge": 150.00, "units": 1, "date_of_service": "2024-01-15" }
    ]
  },
  "action_edits": {
    "status": "Rejected",
    "rejections": [{ "edit_id": "A001", "message": "Billing Provider NPI is required" }],
    "warnings": []
  },
  "pco_prediction": {
    "result": [{ "serviceLineId": "1", "paid": 0.85, "denied": 0.15, "status": "paid" }]
  },
  "preflight_errors": []
}
```

**Answers questions like:**
- Why was this claim rejected?
- What NPI was used for billing?
- Which CPT codes were billed and what were the charges?
- What did PCO predict for each service line?
- What diagnosis codes are on this claim?

---

### Mode 2 — Batch-Level Chat

| | |
|---|---|
| **Triggered by** | Batch-level chat button at the top of the results page, or "Batch Run" option via context chooser |
| **Thread ID** | `{batch_id}_batch` |
| **Context size** | ~1–4 KB |

**Context shape — file-grouped execution summary:**

```json
{
  "batch_id": "bulk_1737394012",
  "status": "success",
  "summary": {
    "total_files": 10,
    "total_claims": 47,
    "accepted": 20,
    "warned": 12,
    "rejected": 15
  },
  "files": [
    {
      "filename": "claims_jan.edi",
      "claim_count": 5,
      "accepted": 3,
      "warned": 1,
      "rejected": 1,
      "clm_ids": ["CLM001", "CLM002", "CLM003", "CLM004", "CLM005"]
    }
  ],
  "file_errors": [
    { "filename": "bad_file.edi", "error": "Parse error: missing ISA segment" }
  ]
}
```

**Answers questions like:**
- Which files had the most rejections?
- How many claims were accepted overall?
- List all rejected claim IDs across the batch.
- Which files errored out and why?
- How many claims were submitted in test mode?

---

## Context Chooser — Uncontextualized Chat Entry

When a user types directly into the general chat pane (without clicking a row-level or batch-level button), the chat pane intercepts the first message and presents a context chooser before sending anything to the agent.

**Chooser prompt shown in the chat pane:**

> _What would you like to ask about?_
> - **Batch Run** — overall results, file summaries, acceptance rates
> - **A Specific Claim** — EDI fields, rejection detail, PCO prediction for one claim

**If "Batch Run" is selected:**
Context is built in batch mode and the original question is sent immediately.

**If "A Specific Claim" is selected:**
A claim picker appears (searchable by CLM ID or filename). Once the user selects a claim, context is built in per-claim mode and the question is sent.

After the first selection the context is locked for that chat session. The chooser does not re-appear on subsequent turns in the same thread.

---

## Data Flow

```
Bulk Review Response (in React state after /review-claims-bulk)
│
├── rows[]
│    ├── row[0]: { edi_fields, action_edits, pco_prediction }  ──┐
│    ├── row[1]: { edi_fields, action_edits, pco_prediction }    │  Per-claim chat
│    └── row[N]: { edi_fields, action_edits, pco_prediction }  ──┘  sends one row as-is
│
└── Derived batch summary  ─────────────────────────────────────────  Batch chat
     ├── files[]: { filename, counts, clm_ids[] }                     sends this object
     └── file_errors[]


Chat Entry Points
│
├── Per-row button  ──────────────────────────────────────────────  claim context (immediate)
├── Batch-level button  ──────────────────────────────────────────  batch context (immediate)
└── Direct chat pane  ────────────→  Context chooser prompt
                                          ├── Batch Run  ─────────  batch context
                                          └── Specific Claim  ────  claim picker → claim context
```

---

## What Changed vs. Previous Approach

| | Before | After |
|---|---|---|
| EDI fields in chat context | Not included | Included (per-claim mode) |
| Context per turn | All rows slimmed + truncated at 20K chars | One shaped object per mode, no truncation |
| Server-side storage | None (workaround: re-send all rows every turn) | None (frontend holds full response in state) |
| Max batch size | ~25–40 claims before truncation | 500 claims — context size is independent of batch size |
| Thread isolation | Single thread per batch run | Per-claim threads + one batch thread, histories isolated |
| Uncontextualized chat entry | Not handled | Context chooser prompt with claim picker |
| `slimBulkRowForChat()` | Used for all chat turns | Not used in per-claim mode — full row sent as-is |
| `_MAX_CONTEXT_CHARS` cap | 20,000 chars (hard truncation) | Removed — both context shapes are small by design |

---

## What Was Ruled Out

| Approach | Reason discarded |
|---|---|
| RAG / vector search | Over-engineered for structured tabular data with known keys |
| Redis cache | No multi-replica problem to solve if context is stateless per turn |
| Azure Blob / TRCM Tracker storage | Adds infra and PHI handling overhead; unnecessary given frontend already holds the data |
| Larger context window | Doesn't fix re-sending 500 claims × N turns = large token cost |
| Question-aware frontend routing | Fragile heuristic; breaks for compound or ambiguous questions |
| Agent-guided context switching | Adds a round-trip to the LLM before the real question is answered |

---

## Key Files

| File | Role |
|---|---|
| `Claim-Engine/src/agents/batch_claim_agent/agent.py` | `BatchClaimConversationAgent`, `_augment_user_message`, system prompt |
| `TRCM-app/src/services/claimReviewService.js` | `buildBatchChatContext()`, `postBatchAgentChat()` |
| `Claim-Engine/src/agents/tps_agent/edi_to_pco.py` | `ParsedClaim` dataclass, `parse_edi_837p()` — source of `edi_fields` |
| `Claim-Engine/src/api/main.py` | `/review-claims-bulk` endpoint — stitches `edi_fields + action_edits + pco_prediction` per row |
