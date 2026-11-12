/*
 * TRCM Batch Review - MOCK application logic (learning project only).
 *
 * Simulates:
 *   - EDI upload validation (format, content, empty file, 50-claim limit)
 *   - "Run review": TPS Action Edits validation + PCO denial prediction (fake, deterministic)
 *   - Claim grid, detail popup, raw JSON, CSV export, Clear
 *   - A rule-based "AI" chatbot with Bulk chat and Claim-level chat
 *
 * Everything is deterministic so the automated tests can assert exact values.
 */
(function () {
  "use strict";

  const MAX_CLAIMS = 50;
  const REVIEW_DELAY_MS = 1500;
  const BOT_DELAY_MS = 400;

  const state = {
    fileName: null,
    claims: [],        // parsed claims waiting for review
    reviewed: [],      // claims after review
    hasReviewed: false,
    cleared: false,
    reviewRunning: false,
    chatMode: "bulk",  // "bulk" | "claim"
    activeClaimId: null,
    bulkHistory: [],
    claimHistory: {}
  };

  const $ = (id) => document.getElementById(id);

  // ---------------------------------------------------------------- EDI parsing
  function parseEdi(text) {
    const segments = text.split("~").map((s) => s.trim()).filter(Boolean);
    const claims = [];
    let current = null;
    segments.forEach((seg) => {
      const el = seg.split("*");
      switch (el[0]) {
        case "CLM":
          current = { claimId: el[1], charge: el[2], seq: claims.length + 1 };
          claims.push(current);
          break;
        case "NM1":
          if (!current) break;
          if (el[1] === "IL") { current.lastName = el[3]; current.firstName = el[4]; current.memberId = el[9]; }
          if (el[1] === "85") { current.providerName = el[3]; current.npi = el[9]; }
          if (el[1] === "PR") { current.payerName = el[3]; current.payerId = el[9]; }
          break;
        case "DMG":
          if (current) current.dob = formatDate(el[2]);
          break;
        case "HI":
          if (current) current.icd = (el[1] || "").split(":")[1] || "";
          break;
        case "SV1":
          if (current) {
            current.cpt = (el[1] || "").split(":")[1] || "";
            current.units = el[4];
          }
          break;
        default:
          break;
      }
    });
    return claims;
  }

  function formatDate(d8) {
    if (!d8 || d8.length !== 8) return d8 || "";
    return d8.slice(0, 4) + "-" + d8.slice(4, 6) + "-" + d8.slice(6, 8);
  }

  function isValidEdiStructure(text) {
    return text.trimStart().startsWith("ISA*") && text.includes("ST*837");
  }

  // ------------------------------------------- Fake "TPS Action Edits" + "PCO"
  const EDIT_RULES = [
    { code: "E101", field: "icd", test: (c) => !c.icd,
      reason: "E101: Missing primary diagnosis code (HI segment)",
      fix: "Add the primary ICD-10 diagnosis code in the HI segment." },
    { code: "E205", field: "cpt", test: (c) => !/^\d{5}$/.test(c.cpt || ""),
      reason: "E205: Invalid procedure code in SV1",
      fix: "Correct the CPT code in SV1 to a valid 5-digit code." },
    { code: "E310", field: "npi", test: (c) => !/^\d{10}$/.test(c.npi || ""),
      reason: "E310: Invalid rendering provider NPI",
      fix: "Correct the rendering provider NPI to a valid 10-digit number." }
  ];

  function reviewClaim(c) {
    const failed = EDIT_RULES.filter((r) => r.test(c));
    const rejected = failed.length > 0;
    // Deterministic fake denial-probability score (PCO model stand-in)
    const score = rejected ? 90 + (c.seq % 8) : 5 + (c.seq % 15);
    return Object.assign({}, c, {
      patientName: c.firstName + " " + c.lastName,
      status: rejected ? "Rejected" : "Accepted",
      reason: failed.map((r) => r.reason).join("; "),
      rejectedFields: failed.map((r) => r.field),
      fixes: failed.map((r) => r.fix),
      editCodes: failed.map((r) => r.code),
      score: score
    });
  }

  // ------------------------------------------------------------------- Upload
  function setMessage(text, kind) {
    const m = $("uploadMessage");
    m.textContent = text;
    m.className = "message " + (kind || "");
  }

  function resetUploadState() {
    state.claims = [];
    state.fileName = null;
    $("runReviewBtn").disabled = true;
  }

  function onUpload() {
    const input = $("fileInput");
    resetUploadState();
    setMessage("", ""); // clear the previous result so tests can wait for the new one
    if (!input.files || input.files.length === 0) {
      setMessage("No file selected. Please choose an EDI file.", "error");
      return;
    }
    const file = input.files[0];
    if (!file.name.toLowerCase().endsWith(".edi")) {
      setMessage("Invalid file format. Only .edi files are accepted.", "error");
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      const text = String(reader.result || "");
      if (text.trim().length === 0) {
        setMessage("File is empty or contains no valid claims.", "error");
        return;
      }
      if (!isValidEdiStructure(text)) {
        setMessage("File is not a valid EDI format.", "error");
        return;
      }
      const claims = parseEdi(text);
      if (claims.length === 0) {
        setMessage("File is empty or contains no valid claims.", "error");
        return;
      }
      if (claims.length > MAX_CLAIMS) {
        setMessage("Exceeds the 50 claim limit. Please remove claims and re-upload.", "error");
        return;
      }
      state.claims = claims;
      state.fileName = file.name;
      setMessage("File uploaded successfully: " + claims.length + " claims found.", "success");
      $("runReviewBtn").disabled = false;
    };
    reader.onerror = () => setMessage("File could not be read. Please try again.", "error");
    reader.readAsText(file);
  }

  // --------------------------------------------------------------- Run review
  function onRunReview() {
    if (state.reviewRunning || state.claims.length === 0) return;
    state.reviewRunning = true;
    $("runReviewBtn").disabled = true;
    $("exportBtn").disabled = true;
    $("spinner").hidden = false;
    document.body.dataset.reviewCount = String(Number(document.body.dataset.reviewCount) + 1);

    setTimeout(() => {
      state.reviewed = state.claims.map(reviewClaim);
      state.hasReviewed = true;
      state.cleared = false;
      state.reviewRunning = false;
      $("spinner").hidden = true;
      $("runReviewBtn").disabled = false;
      $("exportBtn").disabled = false;
      renderGrid();
      renderStats();
    }, REVIEW_DELAY_MS);
  }

  // --------------------------------------------------------------------- Grid
  const COLUMNS = ["claimId", "patientName", "dob", "memberId", "npi", "providerName",
    "payerId", "payerName", "icd", "cpt", "units", "charge", "score", "status", "reason"];

  function renderGrid() {
    const body = $("claimsBody");
    body.innerHTML = "";
    if (state.reviewed.length === 0) {
      const tr = document.createElement("tr");
      tr.id = "emptyRow";
      const td = document.createElement("td");
      td.colSpan = 16;
      td.className = "empty";
      td.textContent = "No results yet. Upload a file and run a review.";
      tr.appendChild(td);
      body.appendChild(tr);
      return;
    }
    state.reviewed.forEach((c) => {
      const tr = document.createElement("tr");
      tr.className = "claim-row";
      tr.dataset.claimId = c.claimId;
      COLUMNS.forEach((col) => {
        const td = document.createElement("td");
        td.dataset.col = col;
        td.textContent = col === "score" ? c.score + "%" : (c[col] == null ? "" : c[col]);
        if (col === "status") td.className = "status-" + c.status;
        tr.appendChild(td);
      });
      const actions = document.createElement("td");
      actions.dataset.col = "actions";
      const details = document.createElement("button");
      details.className = "btn btn-small details-btn";
      details.textContent = "Details";
      details.addEventListener("click", () => openDetail(c.claimId));
      const ask = document.createElement("button");
      ask.className = "btn btn-small ask-btn";
      ask.textContent = "Ask";
      ask.addEventListener("click", () => switchToClaimChat(c.claimId));
      actions.append(details, " ", ask);
      tr.appendChild(actions);
      body.appendChild(tr);
    });
  }

  function renderStats() {
    const total = state.reviewed.length;
    const rejected = state.reviewed.filter((c) => c.status === "Rejected").length;
    $("statsTotal").textContent = total;
    $("statsRejected").textContent = rejected;
    $("statsAccepted").textContent = total - rejected;
  }

  // ------------------------------------------------------------ Detail modal
  const DETAIL_FIELDS = [
    ["claimId", "Claim ID"], ["patientName", "Patient"], ["dob", "Date of birth"],
    ["memberId", "Member ID"], ["providerName", "Provider"], ["npi", "Provider NPI"],
    ["payerName", "Payer"], ["payerId", "Payer ID"], ["icd", "Diagnosis (ICD-10)"],
    ["cpt", "Procedure (CPT)"], ["units", "Units"], ["charge", "Charge"],
    ["status", "Status"], ["reason", "Rejection reason"]
  ];

  function findClaim(id) {
    return state.reviewed.find((c) => c.claimId.toUpperCase() === String(id).toUpperCase());
  }

  function openDetail(claimId) {
    const c = findClaim(claimId);
    if (!c) return;
    $("detailTitle").textContent = "Claim detail: " + c.claimId;
    const dl = $("detailFields");
    dl.innerHTML = "";
    DETAIL_FIELDS.forEach(([key, label]) => {
      const dt = document.createElement("dt");
      dt.textContent = label;
      const dd = document.createElement("dd");
      dd.dataset.field = key;
      dd.textContent = c[key] === "" || c[key] == null ? "(missing)" : c[key];
      if (c.rejectedFields.includes(key)) dd.classList.add("rejected-field");
      dl.append(dt, dd);
    });
    const raw = {
      claimId: c.claimId,
      patient: { name: c.patientName, dob: c.dob, memberId: c.memberId },
      provider: { name: c.providerName, npi: c.npi },
      payer: { name: c.payerName, id: c.payerId },
      diagnosisCodes: [c.icd],
      serviceLines: [{ cpt: c.cpt, units: Number(c.units), charge: Number(c.charge) }],
      actionEdits: c.editCodes,
      prediction: { denialProbability: c.score / 100, status: c.status }
    };
    $("rawJson").textContent = JSON.stringify(raw, null, 2);
    $("rawJson").hidden = true;
    $("toggleJsonBtn").textContent = "View raw JSON";
    $("detailModal").hidden = false;
  }

  function closeDetail() { $("detailModal").hidden = true; }

  function toggleJson() {
    const pre = $("rawJson");
    pre.hidden = !pre.hidden;
    $("toggleJsonBtn").textContent = pre.hidden ? "View raw JSON" : "Hide raw JSON";
  }

  // ------------------------------------------------------------------- Export
  function csvCell(v) { return '"' + String(v == null ? "" : v).replace(/"/g, '""') + '"'; }

  function onExport() {
    if (!state.hasReviewed || state.reviewed.length === 0) return;
    const header = ["Claim ID", "Patient Name", "DOB", "Member ID", "Provider NPI", "Provider Name",
      "Payer ID", "Payer Name", "ICD-10", "CPT", "Units", "Charge", "Denial Score", "Status", "Rejection Reason"];
    const rows = state.reviewed.map((c) => [c.claimId, c.patientName, c.dob, c.memberId, c.npi,
      c.providerName, c.payerId, c.payerName, c.icd, c.cpt, c.units, c.charge, c.score + "%",
      c.status, c.reason].map(csvCell).join(","));
    const csv = [header.map(csvCell).join(",")].concat(rows).join("\r\n");
    const d = new Date();
    const stamp = d.getFullYear() + String(d.getMonth() + 1).padStart(2, "0") + String(d.getDate()).padStart(2, "0");
    const blob = new Blob([csv], { type: "text/csv" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = "batch_review_export_" + stamp + ".csv";
    document.body.appendChild(a);
    a.click();
    a.remove();
  }

  // -------------------------------------------------------------------- Clear
  function onClear() {
    state.claims = [];
    state.reviewed = [];
    state.hasReviewed = false;
    state.cleared = true;
    state.fileName = null;
    $("fileInput").value = "";
    $("runReviewBtn").disabled = true;
    $("exportBtn").disabled = true;
    setMessage("", "");
    renderGrid();
    renderStats();
    switchToBulkChat();
  }

  // --------------------------------------------------------------------- Chat
  const BULK_SAMPLES = ["Give an overview of this batch execution", "Which claims were accepted?",
    "Which claims got rejected and why?", "What is the overall acceptance rate?"];
  const CLAIM_SAMPLES = ["Why was this claim rejected?", "Summarize this claim in plain English",
    "List the recommended fixes from Action Edits"];

  function renderSamples() {
    const box = $("sampleQuestions");
    box.innerHTML = "";
    (state.chatMode === "bulk" ? BULK_SAMPLES : CLAIM_SAMPLES).forEach((q) => {
      const b = document.createElement("button");
      b.className = "btn btn-small sample-q";
      b.textContent = q;
      b.addEventListener("click", () => sendMessage(q));
      box.appendChild(b);
    });
  }

  function history() {
    if (state.chatMode === "bulk") return state.bulkHistory;
    if (!state.claimHistory[state.activeClaimId]) state.claimHistory[state.activeClaimId] = [];
    return state.claimHistory[state.activeClaimId];
  }

  function renderMessages() {
    const box = $("chatMessages");
    box.innerHTML = "";
    history().forEach((m) => {
      const div = document.createElement("div");
      div.className = "msg " + m.role;
      div.textContent = m.text; // textContent = no HTML/script injection possible
      box.appendChild(div);
    });
    box.scrollTop = box.scrollHeight;
  }

  function addMessage(role, text) {
    history().push({ role: role, text: text });
    renderMessages();
  }

  function sendMessage(raw) {
    const text = String(raw == null ? "" : raw);
    if (text.trim().length === 0) return; // TC-GEN-05/06: ignore blank input
    addMessage("user", text);
    $("chatInput").value = "";
    const modeAtSend = state.chatMode;
    const claimAtSend = state.activeClaimId;
    $("typingIndicator").hidden = false;
    setTimeout(() => {
      $("typingIndicator").hidden = true;
      // Discard the reply if the user switched context mid-response (TC-BC58)
      if (state.chatMode !== modeAtSend || state.activeClaimId !== claimAtSend) return;
      addMessage("bot", botReply(text));
    }, BOT_DELAY_MS);
  }

  function switchToClaimChat(claimId) {
    state.chatMode = "claim";
    state.activeClaimId = claimId;
    const c = findClaim(claimId);
    $("chatHeader").textContent = "CLM " + claimId + (state.fileName ? " (" + state.fileName + ")" : "");
    $("chatInput").placeholder = "Ask about claim " + claimId;
    $("batchBtn").hidden = false;
    renderSamples();
    if (history().length === 0) {
      addMessage("bot", "You're now asking about claim " + claimId + ". Status: " + (c ? c.status : "unknown") + ".");
    } else {
      renderMessages();
    }
  }

  function switchToBulkChat() {
    state.chatMode = "bulk";
    state.activeClaimId = null;
    $("chatHeader").textContent = "Bulk chat";
    $("chatInput").placeholder = "Ask about this batch";
    $("batchBtn").hidden = true;
    renderSamples();
    renderMessages();
  }

  // ---- Rule-based bot. Deterministic so tests can assert exact answers.
  function botReply(input) {
    const q = input.toLowerCase().replace(/\s+/g, " ").trim();
    const idMatch = input.toUpperCase().match(/STRESS\d{5}/);

    if (/^(hi|hello|hey)\b/.test(q)) {
      return "Hello! I can answer questions about the claims in your reviewed batch.";
    }
    if (/\b(bye|thanks|thank you)\b/.test(q)) {
      return "You're welcome. Come back any time you need help with a batch.";
    }
    if (/\b(weather|joke|cricket|movie|stock)\b/.test(q)) {
      return "I can only help with claim batch review questions, such as rejections, acceptance rate, or a specific claim.";
    }
    if (/what is a clm segment/.test(q)) {
      return "In an 837 EDI file, the CLM segment starts a claim. It holds the claim ID (patient control number) and the total charge.";
    }

    if (state.chatMode === "claim") return claimReply(q, idMatch);

    if (!state.hasReviewed) {
      return state.cleared
        ? "No batch data is currently loaded. Upload a file and run a review first."
        : "No review results are available yet. Upload a file and click Run review.";
    }

    const all = state.reviewed;
    const rejected = all.filter((c) => c.status === "Rejected");
    const accepted = all.filter((c) => c.status === "Accepted");

    if (idMatch) {
      const c = findClaim(idMatch[0]);
      if (!c) return "Claim " + idMatch[0] + " was not found in the current batch.";
      return c.status === "Rejected"
        ? c.claimId + " was rejected: " + c.reason + ". Click Ask on that row for per-claim detail."
        : c.claimId + " was accepted. Click Ask on that row for per-claim detail.";
    }
    if (/\b(trend|over time)\b/.test(q)) {
      return "Rejection trends over time aren't tracked. I only have data for the batch that's currently loaded.";
    }
    if (/last week|previous batch|yesterday/.test(q)) {
      return "I only have visibility into the currently loaded batch, so I can't compare it with earlier batches.";
    }
    if (/overview|summary|summarize/.test(q)) {
      return "Batch overview: 1 file (" + state.fileName + "), " + all.length + " claims, " +
        rejected.length + " rejected, " + accepted.length + " accepted.";
    }
    if (/acceptance rate/.test(q)) {
      const pct = all.length === 0 ? 0 : (accepted.length * 100) / all.length;
      return "Overall acceptance rate is " + pct.toFixed(1) + "% (" + accepted.length + " of " + all.length + " claims accepted).";
    }
    if (/rej(e)?ct|wrong|denied|fail/.test(q)) { // loose match so typos like "rejcted" still work
      if (rejected.length === 0) return "No claims were rejected in this batch.";
      return rejected.length + " claims were rejected:\n" +
        rejected.map((c) => c.claimId + " - " + c.reason).join("\n");
    }
    if (/accept/.test(q)) {
      return accepted.length + " claims were accepted: " + accepted.map((c) => c.claimId).join(", ");
    }
    return "Sorry, I didn't understand that. Try asking for an overview, rejected claims, or the acceptance rate.";
  }

  function claimReply(q, idMatch) {
    const c = findClaim(state.activeClaimId);
    if (!c) return "This claim is no longer available. Return to Batch to continue.";
    if (idMatch && idMatch[0] !== c.claimId) {
      return "This chat is scoped to CLM " + c.claimId + " only. Click Ask on " + idMatch[0] + "'s row to ask about it.";
    }
    if (/fix|recommend/.test(q)) {
      return c.fixes.length ? "Recommended fixes:\n" + c.fixes.join("\n") : "No fixes needed. This claim passed all Action Edits.";
    }
    if (/summar|plain english/.test(q)) {
      return "Claim " + c.claimId + " is for " + c.patientName + ", billed by " + c.providerName + " to " +
        c.payerName + " for procedure " + c.cpt + " at $" + c.charge + ". It was " + c.status.toLowerCase() + ".";
    }
    if (/why|reject|status/.test(q)) {
      return c.status === "Rejected"
        ? "Claim " + c.claimId + " was rejected because of " + c.reason + "."
        : "Claim " + c.claimId + " was accepted. It passed all Action Edits.";
    }
    return "I can explain this claim's status, summarize it, or list recommended fixes.";
  }

  // --------------------------------------------------------------------- Init
  document.addEventListener("DOMContentLoaded", () => {
    $("uploadBtn").addEventListener("click", onUpload);
    $("runReviewBtn").addEventListener("click", onRunReview);
    $("exportBtn").addEventListener("click", onExport);
    $("clearBtn").addEventListener("click", onClear);
    $("closeDetailBtn").addEventListener("click", closeDetail);
    $("toggleJsonBtn").addEventListener("click", toggleJson);
    $("batchBtn").addEventListener("click", switchToBulkChat);
    $("chatSendBtn").addEventListener("click", () => sendMessage($("chatInput").value));
    $("chatInput").addEventListener("keydown", (e) => { if (e.key === "Enter") sendMessage($("chatInput").value); });
    renderSamples();
    addMessage("bot", "Hi! I'm the Batch Review assistant. Upload an EDI file and run a review, then ask me about accepted claims, rejections, or the acceptance rate.");
  });
})();
