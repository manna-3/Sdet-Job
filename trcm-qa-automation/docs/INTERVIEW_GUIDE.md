# Interview guide: TRCM QA project

Read this top to bottom once, then use the question sections for revision. Every answer points to a real file in this project so you can open it and explain it confidently.

## A note on honesty first

The manual testing work (the three Excel sheets, the defects, and the critical EDI bug) is your real project experience. This automation framework is a practice build modelled on that project, running against a mock app.

That's a strength, not a weakness, if you say it plainly: *"On the project I did the manual and API testing. To build my automation skills, I designed a Selenium framework around the same test cases and set it up in Jenkins and Azure DevOps."* Interviewers ask follow-up questions, and you can answer every one of them about this framework because you can open the code. Claiming it ran in production at your company is the one thing that can fall apart under questioning.

---

## 1. The 60-second project pitch

> "I worked on TRCM, a healthcare claims review application. Users upload 837 EDI files with up to 50 claims. The system extracts claim data, validates it through the TPS API, which we call Action Edits, and runs a denial prediction model called PCO that gives each claim a denial probability. Results show in a grid with a detail view and CSV export. There's also an AI chatbot that answers questions about the whole batch or a single claim. All the APIs sit behind Azure API Management for security.
>
> I tested three areas: the batch review UI and its data, the AI chatbot, and the APIM security layer. Then I automated those test cases in a Java Selenium framework with TestNG and the Page Object Model, using RestAssured for the API tests, Extent Reports and Log4j2 for reporting and logging, and pipelines in both Jenkins and Azure DevOps."

## 2. Domain words you must know

| Term | Meaning in one line |
|---|---|
| EDI 837 | The standard electronic file format for submitting healthcare claims. |
| Segment / element | An EDI file is made of segments (ending in `~`) made of elements (separated by `*`). |
| ISA / GS / ST | Envelope headers: interchange, functional group, and transaction set. `ST*837` marks a claim transaction. |
| CLM | The segment that starts a claim: claim ID and total charge. |
| NM1 | A name segment. `IL` = patient/subscriber, `85` = rendering provider, `PR` = payer. |
| ICD-10 | Diagnosis code: *why* the patient was treated (for example E119, type 2 diabetes). |
| CPT | Procedure code: *what* was done (for example 99213, an office visit). |
| NPI | 10-digit National Provider Identifier for the doctor or clinic. |
| Payer | The insurance company that pays the claim. |
| Denial | The payer refuses to pay. Predicting denials before submission saves money and time. |
| Action Edits (TPS) | Rule-based validation that rejects claims with bad or missing fields. |
| PCO | The prediction model that scores the denial probability for each claim. |
| ISA15 = T | Test-mode indicator in the ISA header (`P` = production). |

## 3. What was tested, and with which techniques

**Batch Review UI (BatchReview sheet)**

- **Upload:** boundary value analysis on the 50-claim limit (1, 49, 50, 51, 100, and 0 claims) and equivalence partitioning on file type (.edi valid; csv, pdf, docx, and txt invalid). There's also a content check: a PDF renamed to `.edi` must be rejected, which proves the app reads the file and doesn't just look at the extension.
- **Data validation:** every grid value is compared against the source EDI file.
- **Prediction:** scores must be within range, logically consistent with the data, and denied claims must show a reason.
- **Export:** the actual downloaded CSV is opened and compared with the grid.
- **Negative and resilience cases:** double-click, export before review, and stale data after Clear.

**AI chatbot (Chatbot sheet)**

- **Grounding:** every number the bot says must match the table.
- **Hallucination:** for an unknown claim, untracked data, or "last week's batch", the bot must say "not available" instead of inventing an answer.
- **Context isolation:** claim chat must not leak into batch chat, and one claim's answer must never appear under another claim's header ("context bleed").
- **Determinism:** the same question on the same data gives the same answer.
- **Security:** script and SQL injection are rendered as plain text, never executed.

**APIM (APIM sheet)**

- Valid key → 200, missing or invalid key → 401, bypassing APIM without a key → 401, wrong backend key → 403, and the health endpoint is public.
- The main idea is **defense in depth**: the gateway checks the subscription key, and the backend separately checks its own key, so going around APIM doesn't help an attacker.

Testing types you can name: functional, negative, boundary, equivalence partitioning, data validation, integration, API, security, AI/LLM, end-to-end, smoke, and regression.

## 4. The framework, layer by layer

```
 Test classes (tests/ui, tests/api)      WHAT to test: steps + assertions only
        │ use
 Page Objects (pages/)                   HOW to interact with each screen: locators + actions
        │ use
 BasePage + DriverManager (ThreadLocal)  explicit waits, one browser per thread
        │ created by
 DriverFactory  ← ConfigReader           browser and headless settings from config/env/-D
        │
 Listeners (TestListener, Retry)  ──►  ExtentReports + Log4j2 + screenshots
```

**How one test run flows (say this in order):**

1. Maven Surefire starts TestNG with the suite XML (`-Dsuite=...`).
2. `@BeforeSuite` starts the mock server, but only when `env=local`.
3. For each test, `@BeforeMethod` in `BaseTest` creates a fresh browser through `DriverFactory`, stores it in a `ThreadLocal`, and opens the app.
4. `TestListener.onTestStart` creates the Extent entry for the test.
5. The test calls page object methods. Each step goes to the log and to the report through `ReportLogger.step()`.
6. If the test fails, `onTestFailure` attaches the stack trace and a screenshot. This happens *before* `@AfterMethod`, so the browser is still open.
7. `@AfterMethod` quits the browser.
8. When the suite ends, the listener flushes the report to `target/reports/ExtentReport.html`.

## 5. Design decisions and why (interviewers love "why")

| Decision | Why | Where |
|---|---|---|
| Page Object Model | When a locator changes, I fix it in one place instead of every test. Tests read like business steps. | `pages/` |
| `By` locators, not PageFactory | `@FindBy` with PageFactory is older and can throw stale element errors on dynamic pages. Plain `By` plus explicit waits is simpler and more reliable. | `BatchReviewPage` |
| Stable `id` / `data-*` locators | They don't break when the CSS or layout changes, unlike long XPaths. | `batch-review.html` |
| Explicit waits only, implicit wait 0 | Mixing implicit and explicit waits causes unpredictable timeouts. No `Thread.sleep` anywhere. | `BasePage`, `ChatPanel.ask` |
| `ThreadLocal<WebDriver>` | Parallel tests each get their own browser without interfering. | `DriverManager` |
| Fresh browser per test | Tests are independent, so they can run in any order or in parallel, and one failure can't break the next test. | `BaseTest` |
| Config priority: `-D` > env var > file | The same code runs locally and in CI. Secrets come from CI, never from Git. | `ConfigReader` |
| Source-of-truth parser | The test parses the EDI file itself and compares every field, so a wrong value can't hide behind a correct layout. | `EdiParser`, `ClaimGridTest` |
| SoftAssert for data checks | One run reports *all* mismatched fields instead of stopping at the first one. | `ClaimGridTest` |
| Read the grid with one JS call | 50 rows × 15 cells would be 750 WebDriver round trips; one `executeScript` call returns everything. | `BatchReviewPage.getGridData` |
| Groups + suite XMLs | The same tests can be selected as smoke, regression, or api without duplicating code. | `suites/` |
| Retry through `IAnnotationTransformer` | Applies to every test automatically, and is configurable (0 locally, 1 in CI). Retries are for environment flakiness; flaky tests still get investigated. | `listeners/` |
| Base64 screenshots | The report stays a single file that still shows images after being downloaded from CI. | `ScreenshotUtil` |
| Verifying the downloaded file | Testing that the Export button is clickable isn't enough; the test opens the CSV and checks its content. | `ExportTest` |

## 6. Reporting and logging

- **Extent Reports:** one HTML file with pass/fail/skip status, categories from TestNG groups, system info (environment, browser, CI build number), step logs, and failure screenshots. Each thread writes to its own `ExtentTest` through a `ThreadLocal`, and `createTest` is synchronized for parallel runs.
- **Log4j2:** console output plus a rolling file at `target/logs/automation.log`. Every line includes the thread name, which matters in parallel runs. Noisy libraries like Selenium are set to WARN.
- **`ReportLogger.step()`:** a single call that writes to both the log and the report, so they never drift apart.
- **In CI:** JUnit XML from Surefire feeds the native test tabs (Jenkins "Test Result", Azure "Tests" tab), and the Extent report, logs, and screenshots are published as artifacts.

## 7. CI/CD in plain words

**What the pipeline does (same in both tools):** check out the code, compile it (fail fast on build errors), run the chosen suite headless, and then *always* publish results, even when tests fail, because a failed run is when you need the report most.

**Triggers:** every push to main (CI), every pull request into main (a quality gate before merging), and a nightly regression run. You can also run it manually and choose the environment, suite, and browser.

**Secrets:** keys are never in Git. Jenkins uses Credentials with `withCredentials`; Azure DevOps uses a secret variable group (optionally backed by Key Vault) mapped in `env:`. Both become environment variables that `ConfigReader` reads, and both tools mask them in the logs.

| Concept | Jenkins | Azure DevOps |
|---|---|---|
| File | `ci/jenkins/Jenkinsfile` (Groovy) | `ci/azure-devops/azure-pipelines.yml` (YAML) |
| Inputs | `parameters { choice }` | `parameters:` |
| Nightly | `triggers { cron }` | `schedules:` |
| Secrets | Credentials + `withCredentials` | Variable group + `env:` mapping |
| Test results | `junit` step | `publishJUnitResults` |
| Files | `archiveArtifacts`, `publishHTML` | `PublishPipelineArtifact@1` |
| Always run | `post { always }` | `condition: always()` |

**How it fits your team's release process:** Dev → Cert → UAT → Prod. Smoke runs on every pull request and Dev deployment. Full regression runs after each Cert deployment, whether oncycle (planned for the Tuesday Cert review) or offcycle (with email approval). The environment is just `-Denv=cert`. Stories and linked dev bugs are approved together only when regression passes, and a smoke run after the Prod deployment backs up the manual verification.

## 8. Defect stories (use the STAR format)

**The critical bug: the converter output had the right structure but the wrong data**

- **Situation:** I was testing the EDI converter output. Every segment and the layout matched the expected format.
- **Task:** confirm the converted data was correct before the story moved to Cert.
- **Action:** I compared field values against the source file, not just the structure. Several values were missing or mapped from the wrong fields. I logged a dev bug linked to the story, with the input file, expected and actual output, and the exact fields.
- **Result:** the developer fixed the mapping, I retested it along with related regression cases, and I added field-level data comparison to our test cases. In the automation, `ClaimGridTest` does this for every claim and every field.
- **Lesson:** structural validation isn't enough. Data must be checked against the source.

**DEF-002: denied claims showed no denial reason (P1)**

The status and percentage appeared, but the reason was missing, so users couldn't act on the prediction. In the automation, `PredictionTest.deniedClaimsShowReason` checks this for every rejected row.

**A good "process" answer:** when a bug is found in Cert regression, I create a dev bug linked to the story (no points), it's fixed and tested in Dev, then it goes through the Cert deployment again, and the story and its bugs are approved together.

## 9. Likely questions with short answers

### Selenium

**What's the difference between `findElement` and `findElements`?**
`findElement` returns the first match or throws `NoSuchElementException`. `findElements` returns a list, which is empty if nothing matches. I use `findElements(...).isEmpty()` to check whether something exists without an exception (see `BasePage.isDisplayed`).

**What's the difference between implicit, explicit, and fluent waits?**
An implicit wait is a global wait applied to every find. An explicit wait (`WebDriverWait`) waits for a specific condition on a specific element. A fluent wait is an explicit wait with a custom polling interval and ignored exceptions. I use explicit waits only, with the implicit wait set to 0, because mixing them causes unpredictable delays.

**How do you upload a file?**
Call `sendKeys` with the absolute file path on the `<input type="file">` element. No OS dialog is needed (see `BatchReviewPage.upload`). I avoid tools like AutoIT and Robot because they're fragile and don't work headless.

**How do you handle downloads?**
I set Chrome's `download.default_directory` preference to a folder for each thread, wait for the file (ignoring temporary `.crdownload` files), and then read its content (see `DriverFactory`, `FileWaitUtil`, `ExportTest`).

**What causes a `StaleElementReferenceException`, and how do you fix it?**
The element was re-rendered after you found it. Re-find the element, use `By` locators instead of stored `WebElement`s, and wait for the new state. The grid in this app re-renders after every review, which is why the page objects look elements up fresh each time.

**How do you handle alerts, frames, and windows?**
Use `driver.switchTo().alert()`, `.frame()`, and `.window(handle)`. In `BulkChatTest.injectionInputSanitized`, I use `switchTo().alert()` expecting `NoAlertPresentException`, which proves the injected script didn't run.

**When do you use JavaScriptExecutor?**
When it's much faster or the only way: reading a large grid in one call, scrolling, or reading hidden values. I don't use it to click buttons, because a JS click skips the checks a real user click goes through.

**How do you run tests headless, and why?**
Use `--headless=new` with a fixed window size. It's needed in CI agents that have no display, and it's faster.

**What's Selenium Manager?**
It's built into Selenium since 4.6 and downloads the matching browser driver automatically, which replaces WebDriverManager.

### TestNG

**What's the order of TestNG annotations?**
`@BeforeSuite` → `@BeforeTest` → `@BeforeClass` → `@BeforeMethod` → `@Test` → `@AfterMethod` → `@AfterClass` → `@AfterTest` → `@AfterSuite`.

**Why use `alwaysRun = true` on setup methods?**
When a suite filters by groups (smoke), setup methods without a group would be skipped. `alwaysRun` makes them run regardless.

**How do you pass test data?**
With a `@DataProvider`. `FileUploadTest.wrongFormatRejected` runs once for each invalid format: csv, pdf, docx, and txt.

**What's the difference between hard and soft asserts?**
A hard assert (`Assert`) stops the test at the first failure. A soft assert (`SoftAssert`) collects every failure and reports them all at `assertAll()`. I use soft asserts for field-by-field data checks.

**How do you run tests in parallel?**
Set `parallel="classes" thread-count="3"` in the suite XML, with a `ThreadLocal` driver. I chose classes rather than methods because each test class keeps its page object in an instance field.

**How do you rerun failed tests?**
With `IRetryAnalyzer`, attached to every test through `IAnnotationTransformer`. TestNG also writes `testng-failed.xml`, which you can run directly.

**What are listeners used for?**
`ITestListener` hooks test events. I use it to create report entries, attach screenshots on failure, and flush the report.

### Framework and design

**Walk me through your framework.**
Use Section 4: the layers, then the run flow.

**How do you switch environments?**
`-Denv=dev` loads `config/dev.properties`. Any key can be overridden with `-D` or an environment variable.

**How do you keep tests independent?**
Each test gets a fresh browser, sets up its own data (`uploadAndReview`), and makes no assumptions about order.

**How do you deal with flaky tests?**
Find the cause first. It's usually a missing wait or shared state. Use proper explicit waits and independent tests. Retry only absorbs real environment noise, and every retried test still gets investigated.

**What would you improve next?**
- Selenium Grid or Docker for cross-browser runs
- Allure reports
- Test data created through APIs instead of static files
- An automated TPS-down test (TC-020)
- Accessibility checks with axe-core

### API / APIM

**What's the difference between 401 and 403?**
401 means "I don't know who you are" (missing or invalid credentials). 403 means "I know who you are, but you're not allowed."

**What is APIM?**
Azure API Management is a gateway in front of the APIs. It handles subscription keys, rate limits, and policies. Here, a policy injects the backend's `X-API-Key` from a Named Value.

**Why test calling the backend directly?**
To prove the service is protected on its own (TC-04). If it trusted every caller, anyone who found the backend URL could skip APIM.

**What do you validate in an API test?**
The status code, the body fields, error messages, the response time, and negative cases for auth.

**What's the difference between Postman and RestAssured?**
Postman is for exploring APIs and doing manual testing, which is what I used on the project. RestAssured is for automated tests that run in CI.

### AI chatbot testing

**How is testing a chatbot different from testing normal software?**
The output is natural language and can vary, so you test properties rather than exact strings: whether the answer is grounded in the data, whether it refuses to invent answers, whether it stays in scope, whether it keeps context, whether it's consistent, and whether it's safe against injection.

**What's a hallucination, and how did you test for it?**
It's a confident answer that isn't supported by the data. I asked about a claim ID that doesn't exist, trends with no timestamp data, and "last week's batch". The bot must say the data isn't available.

**What's context bleed?**
One claim's answer appearing under another claim's header after fast switching. It's a critical data-integrity defect, especially with healthcare data.

**Why did you calculate expected values in the test instead of hard-coding them?**
The acceptance rate is calculated from the grid at runtime. If the data changes, the test still checks what matters: that the bot agrees with the table.

### CI/CD

**What happens when a test fails in the pipeline?**
The build fails, which acts as a quality gate. The report, logs, and screenshots are still published because of `post { always }` or `condition: always()`. I open the report, reproduce the failure locally, and either log a bug or fix the test.

**How do you protect secrets?**
They live in Jenkins Credentials or an Azure variable group or Key Vault, get injected as environment variables, are masked in logs, and are never committed to Git.

**Why is there a separate Build stage?**
A compile error fails in about a minute, without starting browsers.

**Why is the nightly run set to 20:30 UTC?**
Azure cron schedules use UTC, and 20:30 UTC is 02:00 IST.

### Process and behavioural

**Walk me through your QA process.**
1. Groom the story and point it.
2. Write test cases during development and get them reviewed by the developer and PO.
3. Add them to DEVO, linked to the story and RWI.
4. Run functional testing in Dev.
5. Log dev bugs linked to the story.
6. Plan the Cert deployment (oncycle or offcycle).
7. Run regression in Cert.
8. Approve the story and its linked bugs together.
9. Hold the UAT demo.
10. Verify in Prod.

**How do you decide what to automate?**
I automate stable, repeated, high-value cases: smoke, regression, data checks, and API auth. I leave subjective or one-time cases manual, along with features that aren't built yet. `docs/TEST_CASE_MAPPING.md` lists what I left out and why.

**What do you do when a developer says "it's not a bug"?**
I go back to the acceptance criteria. If the criteria are unclear, I bring in the PO, show the evidence (steps, data, screenshots), and agree on the expected behaviour.

## 10. Before the interview

- Run `mvn clean test` once, open the report, and take a screenshot you can describe.
- Practise explaining `ClaimGridTest` and `ApimAuthTest` out loud. Those two cover data validation and API testing.
- Know your numbers:
  - 74 automated test methods, 16 of them in smoke.
  - 3 sheets: 52 Batch Review cases, the Chatbot sheet in Parts 1 and 2, and 7 APIM cases.
  - The 50-claim limit.
  - 10 seeded rejections in the 50-claim test file.
