# Test case mapping

This maps every automated test to the test case in the original Excel sheets. It's generated from the `@Test(description = ...)` annotations, so the TC IDs here are exactly what appears in the Extent report.

Totals: **74 test methods** (16 in smoke). The data-driven test `wrongFormatRejected` runs 4 times (TC-007 to TC-010), so a regression run executes 76 test instances excluding the demo.

## BatchReview_QA_TestCases.xlsx

| TC ID and title | Class.method | Groups |
|---|---|---|
| TC-027 Detail popup opens and shows patient, provider, payer, diagnosis and service line | `ClaimDetailTest.detailPopupOpens` | smoke, regression, detail |
| TC-028 Detail view shows correct extracted fields (matches source EDI) | `ClaimDetailTest.detailFieldsMatchSource` | regression, detail |
| TC-029 Rejected fields are highlighted; clean claims have no highlights | `ClaimDetailTest.rejectedFieldsHighlighted` | regression, detail |
| TC-030 Detail view shows valid, readable raw JSON | `ClaimDetailTest.rawJsonIsValid` | regression, detail |
| TC-031 Detail view closes cleanly and the grid is unchanged | `ClaimDetailTest.detailClosesCleanly` | regression, detail |
| TC-021 All 50 claims appear as rows with no duplicates or missing rows | `ClaimGridTest.all50ClaimsShown` | smoke, regression, grid |
| TC-022 Each row shows correct patient data (name, DOB, member ID) | `ClaimGridTest.patientDataMatchesSource` | smoke, regression, grid |
| TC-023 Each row shows correct provider data (NPI, name) | `ClaimGridTest.providerDataMatchesSource` | regression, grid |
| TC-024 Each row shows correct payer data (ID, name) | `ClaimGridTest.payerDataMatchesSource` | regression, grid |
| TC-025 Each row shows correct ICD-10 diagnosis codes | `ClaimGridTest.diagnosisCodesMatchSource` | regression, grid |
| TC-026 Each row shows correct service line details (CPT, units, charge) | `ClaimGridTest.serviceLinesMatchSource` | regression, grid |
| TC-047 App loads correctly on the configured URL | `EnvironmentTest.appLoads` | smoke, regression, environment |
| TC-048 Test run does not point to production | `EnvironmentTest.notPointingToProduction` | smoke, regression, environment |
| TC-037 Export button is available after review | `ExportTest.exportEnabledAfterReview` | regression, export |
| TC-038 Export downloads a CSV with a meaningful file name | `ExportTest.exportDownloadsCsv` | regression, export |
| TC-039 Exported CSV contains all 50 claims (51 lines with header) | `ExportTest.exportContainsAllClaims` | smoke, regression, export |
| TC-040 Exported CSV contains all expected columns | `ExportTest.exportHasExpectedColumns` | regression, export |
| TC-041 Exported CSV data matches the UI grid | `ExportTest.exportMatchesGrid` | regression, export |
| TC-042 Export is disabled before a review is run | `ExportTest.exportDisabledBeforeReview` | regression, export |
| TC-001 Upload valid .edi file with exactly 50 claims | `FileUploadTest.uploadExactly50Claims` | smoke, regression, upload |
| TC-002 Upload .edi file with 51 claims (boundary +1) is rejected | `FileUploadTest.upload51ClaimsRejected` | smoke, regression, upload |
| TC-003 Upload .edi file with 100 claims is rejected | `FileUploadTest.upload100ClaimsRejected` | regression, upload |
| TC-004 Upload .edi file with 49 claims (boundary -1) and review shows 49 rows | `FileUploadTest.upload49ClaimsAccepted` | regression, upload |
| TC-005 Upload .edi file with 1 claim (minimum) shows a single row | `FileUploadTest.uploadSingleClaim` | regression, upload |
| TC-006 Upload empty .edi file (0 claims) is rejected | `FileUploadTest.uploadEmptyFileRejected` | regression, upload |
| TC-007 to TC-010 Wrong file formats (csv, pdf, docx, txt) are rejected | `FileUploadTest.wrongFormatRejected` | regression, upload |
| TC-011 Upload a corrupted .edi file shows a validation error | `FileUploadTest.uploadCorruptedEdi` | regression, upload |
| TC-012 Upload without selecting any file | `FileUploadTest.uploadWithoutFile` | regression, upload |
| TC-013 PDF renamed to .edi is rejected (content is validated, not just extension) | `FileUploadTest.renamedExtensionRejected` | regression, upload |
| TC-032 Every reviewed claim shows a prediction score | `PredictionTest.everyClaimHasScore` | regression, prediction |
| TC-033 Prediction scores are between 0% and 100% | `PredictionTest.scoresWithinRange` | regression, prediction |
| TC-034 Denied claims show a denial reason next to status and score | `PredictionTest.deniedClaimsShowReason` | regression, prediction |
| TC-035 Prediction is logically consistent: risky claims score higher than clean ones | `PredictionTest.predictionConsistentWithData` | regression, prediction |
| TC-036 Only genuinely invalid fields are flagged | `PredictionTest.onlyInvalidFieldsFlagged` | regression, prediction |
| TC-043 / TC-044 Corrected EDI re-validates and previously rejected claims now pass | `RevalidateTest.correctedFilePassesValidation` | regression, revalidate |
| TC-014 Run review button is disabled before upload | `RunReviewTest.runReviewDisabledBeforeUpload` | regression, review |
| TC-015 Run review completes with a valid 50-claim file | `RunReviewTest.runReviewWith50Claims` | smoke, regression, review |
| TC-016 Progress indicator shows and button is disabled while review runs | `RunReviewTest.progressIndicatorDuringReview` | regression, review |
| TC-017 Run review stays disabled after a 51-claim file is rejected | `RunReviewTest.runReviewDisabledAfterLimitError` | regression, review |
| TC-018 Double-clicking Run review processes the batch only once | `RunReviewTest.doubleClickRunsOnce` | regression, review |

## Master_Chatbot_TestCases_Sheet.xlsx

| TC ID and title | Class.method | Groups |
|---|---|---|
| TC-GEN-01 Chatbot loads with a greeting message | `BulkChatTest.chatLoadsWithGreeting` | smoke, regression, chatbot |
| TC-GEN-02 Sample questions match the claim batch capabilities | `BulkChatTest.sampleQuestionsCorrect` | regression, chatbot |
| TC-GEN-04 Bot responds to a greeting | `BulkChatTest.respondsToGreeting` | regression, chatbot |
| TC-GEN-05 / TC-GEN-06 Blank or whitespace-only input is ignored | `BulkChatTest.blankInputIgnored` | regression, chatbot |
| TC-GEN-09 Out-of-scope question is politely redirected | `BulkChatTest.outOfScopeRedirected` | regression, chatbot |
| TC-GEN-26 Script / SQL injection input is sanitized | `BulkChatTest.injectionInputSanitized` | regression, chatbot, security |
| TC-BC24 Asking before review: bot says no results instead of fabricating a number | `BulkChatTest.askBeforeReview` | regression, chatbot, hallucination |
| TC-BC17 Overview sample question matches the table stats exactly | `BulkChatTest.overviewMatchesStats` | smoke, regression, chatbot |
| TC-BC18 Accepted claims list matches the table | `BulkChatTest.acceptedClaimsMatchTable` | regression, chatbot |
| TC-BC19 Rejected claims are listed with their actual edit-reason codes | `BulkChatTest.rejectedClaimsWithReasons` | regression, chatbot |
| TC-BC23 Acceptance rate equals accepted / total from the table | `BulkChatTest.acceptanceRateIsCorrect` | smoke, regression, chatbot |
| TC-BC52 Same question gives the exact same answer every time (deterministic) | `BulkChatTest.answersAreDeterministic` | regression, chatbot |
| TC-BC46 Unknown claim ID: bot says not found instead of inventing an answer | `BulkChatTest.unknownClaimNotInvented` | regression, chatbot, hallucination |
| TC-BC47 Untracked data (trend over time): bot says it is not available | `BulkChatTest.untrackedDataNotFabricated` | regression, chatbot, hallucination |
| TC-BC48 Comparison with an inaccessible batch: bot states its limits | `BulkChatTest.noGuessingAboutOtherBatches` | regression, chatbot, hallucination |
| TC-BC50 Typos and informal phrasing are still understood | `BulkChatTest.typosUnderstood` | regression, chatbot |
| TC-BC39 After Clear, bot does not answer from stale batch data | `BulkChatTest.noStaleDataAfterClear` | regression, chatbot, clear |
| TC-BC31 Clicking Ask opens claim-level chat with the claim header and claim questions | `ClaimChatTest.askOpensClaimChat` | smoke, regression, chatbot |
| TC-BC32 Batch button returns to Bulk chat with batch questions restored | `ClaimChatTest.backToBatch` | regression, chatbot |
| Claim chat explains the real rejection reason for that claim | `ClaimChatTest.claimRejectionReason` | regression, chatbot |
| TC-BC55 Recommended fixes are tied to the claim's actual edit codes | `ClaimChatTest.recommendedFixesSpecific` | regression, chatbot |
| TC-BC56 Plain-English summary matches the claim's real data | `ClaimChatTest.plainEnglishSummaryAccurate` | regression, chatbot |
| TC-BC57 Asking about another claim inside claim chat stays scoped | `ClaimChatTest.otherClaimStaysScoped` | regression, chatbot |
| TC-BC36 Claim chat does not carry over batch chat history | `ClaimChatTest.claimChatIsolatedFromBatchChat` | regression, chatbot |
| TC-BC61 Bulk -> claim -> bulk round trip keeps the batch chat history | `ClaimChatTest.roundTripKeepsHistory` | regression, chatbot |

## APIM_Integration_Testing_1.xlsx

| TC ID and title | Class.method | Groups |
|---|---|---|
| TC-01 Valid subscription key: request succeeds through APIM | `ApimAuthTest.validSubscriptionKey` | smoke, regression, api |
| TC-02 Missing subscription key is rejected by APIM with 401 | `ApimAuthTest.missingSubscriptionKey` | smoke, regression, api, security |
| TC-03 Invalid subscription key is rejected by APIM with 401 | `ApimAuthTest.invalidSubscriptionKey` | regression, api, security |
| TC-04 Direct backend call without X-API-Key is rejected with 401 (APIM bypass) | `ApimAuthTest.directServiceCallWithoutKey` | regression, api, security |
| TC-05 Health endpoint is public and returns healthy | `ApimAuthTest.healthEndpointPublic` | smoke, regression, api |
| TC-06a Wrong X-API-Key on the service returns 403 Invalid API key | `ApimAuthTest.wrongServiceKeyReturns403` | regression, api, security |
| TC-06b Misconfigured APIM Named Value: valid client key still gets 403 from the service | `ApimAuthTest.misconfiguredNamedValueReturns403` | regression, api, security |
| TC-01b Valid service key directly on the backend returns 200 | `ApimAuthTest.validServiceKeyDirect` | regression, api |

## Learning demo (not from a sheet)

| TC ID and title | Class.method | Groups |
|---|---|---|
| DEMO Intentional failure: shows the screenshot and stack trace in the report | `DefectDemoTest.intentionalFailureForReportDemo` | demo |

## Test cases from the sheets that are not automated, and why

Not every manual case should be automated. These were left out on purpose:

| TC ID | Reason |
|---|---|
| TC-019 Navigate away mid-review | Expected behaviour isn't defined yet ("either completes or warns"). Automate once the product owner decides. |
| TC-020 TPS API unavailable | Needs the TPS dependency mocked as down. A good next exercise: add a `/tps` failure switch to the mock server. |
| TC-045 / TC-046 Batch Chat in grid | Feature marked "Pending" / not yet deployed in the sheet. |
| TC-049 200-claim batch screen | Conflicts with the 50-claim limit; needs clarification from the BA. |
| TC-050 / TC-052 Error message quality, smooth scrolling | Subjective ("clear", "smooth"). Better as exploratory/manual checks. |
| TC-GEN-15 Session timeout, TC-GEN-22 Accessibility, TC-GEN-23 Multi-language | Need real session config, an accessibility tool (axe-core), or language support that the mock doesn't have. |
| TC-BC-F01 to F03, TC-E2E-F01/F02 Failure cases | Need fault injection (network drop, backend errors) and load. Candidates for a separate resilience suite. |
| TC-FUT01 to TC-FUT12 Futuristic | Features don't exist yet. Kept in the sheet as regression seeds. |
| APIM TC-07 Pytest unit tests | Owned by developers and run in their own pipeline (`pytest tests/ -m "not integration"`). QA only verifies the result. |
