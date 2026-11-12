# TRCM QA Automation (learning project)

A practice Selenium + TestNG + RestAssured framework built around the TRCM healthcare claims review app: EDI upload and batch review, the AI chatbot, and the APIM security layer. The tests are based on three manual test case sheets: BatchReview_QA_TestCases, Master_Chatbot_TestCases_Sheet, and APIM_Integration_Testing_1.

The real application isn't reachable, so the project ships with a **local mock app and mock APIs**. `mvn test` starts them automatically, which means every test really runs and you can watch the browser do the work.

> Preparing for an interview? Read [`docs/INTERVIEW_GUIDE.md`](docs/INTERVIEW_GUIDE.md).

## Tech stack

| Area | Tool |
|---|---|
| Language and build | Java 17, Maven |
| UI automation | Selenium 4 (Selenium Manager handles drivers) |
| Test runner | TestNG (groups, data providers, parallel runs, listeners, retry) |
| Design pattern | Page Object Model |
| API testing | RestAssured |
| Reporting | Extent Reports 5 (HTML with embedded screenshots) |
| Logging | Log4j2 (console and file) |
| CI/CD | Jenkins (`ci/jenkins`) and Azure DevOps (`ci/azure-devops`), set up independently |

## Prerequisites

- JDK 17 or newer (`java -version`)
- Maven 3.8 or newer (`mvn -version`)
- Google Chrome (Firefox and Edge also work with `-Dbrowser=firefox` or `-Dbrowser=edge`)
- An IDE such as IntelliJ IDEA (Community edition is fine) or Eclipse

You don't need to download ChromeDriver. Selenium Manager does it on the first run, so that run needs internet access.

## Quick start

```bash
# 1. Run the smoke suite (about 16 tests) and watch the browser
mvn clean test -Dsuite=src/test/resources/suites/smoke.xml

# 2. Open the report
#    target/reports/ExtentReport.html   (open in a browser)
#    target/logs/automation.log         (full log)
```

Want to click around the mock app yourself?

```bash
mvn compile exec:java
# then open http://localhost:8089/app/batch-review.html
# test files to upload are in src/test/resources/testdata/
```

## Run options

| What | Command |
|---|---|
| Full regression (default, runs in parallel) | `mvn clean test` |
| Smoke only | `mvn clean test -Dsuite=src/test/resources/suites/smoke.xml` |
| APIM API tests only (no browser) | `mvn clean test -Dsuite=src/test/resources/suites/api.xml` |
| See what a failure looks like | `mvn clean test -Dsuite=src/test/resources/suites/defect-demo.xml` |
| Headless (as in CI) | add `-Dheadless=true` |
| Another browser | add `-Dbrowser=firefox` or `-Dbrowser=edge` |
| Retry failed tests once | add `-Dretry.count=1` |
| Another environment | add `-Denv=dev` or `-Denv=cert` (**fake URLs**: these tests will fail, which shows how environment switching works) |

Any key in `src/main/resources/config/*.properties` can be overridden with `-Dkey=value` or an environment variable (for example, `apim.subscription.key` becomes `APIM_SUBSCRIPTION_KEY`).

## Project structure

```
trcm-qa-automation
├── pom.xml                         dependencies, Surefire (runs TestNG suites), exec plugin
├── ci
│   ├── jenkins/                    Jenkinsfile + setup guide
│   └── azure-devops/               azure-pipelines.yml + setup guide
├── docs
│   ├── INTERVIEW_GUIDE.md          read this before interviews
│   └── TEST_CASE_MAPPING.md        sheet TC ID -> automated test method
└── src
    ├── main/java/com/trcm
    │   ├── mock/MockTrcmServer     fake app + fake APIM + fake backend (JDK HttpServer)
    │   └── qa
    │       ├── config/             ConfigReader (env files, -D overrides, env vars)
    │       ├── driver/             DriverFactory (browser setup), DriverManager (ThreadLocal)
    │       ├── pages/              Page Objects: BatchReviewPage, ClaimDetailModal, ChatPanel
    │       ├── api/                RestAssured request specs
    │       ├── reports/            Extent report setup + ReportLogger (log + report in one call)
    │       ├── listeners/          TestListener (report, screenshots), Retry analyzer/transformer
    │       └── utils/              EdiParser, CsvReader, screenshots, download wait, test data paths
    ├── main/resources
    │   ├── config/                 local / dev / cert properties
    │   ├── log4j2.xml
    │   └── mockapp/                HTML/JS/CSS of the mock Batch Review app
    ├── test/java/com/trcm/qa
    │   ├── base/                   BaseTest (browser lifecycle), BaseApiTest
    │   └── tests/ui, tests/api     the test classes
    └── test/resources
        ├── suites/                 smoke, regression, api, defect-demo TestNG XML
        └── testdata/               EDI files: 1/49/50/51/100 claims, empty, corrupt, wrong formats
```

## Suggested learning path

1. Run the smoke suite and open the Extent report.
2. Read one test end to end: `FileUploadTest.uploadExactly50Claims` → `BatchReviewPage.upload` → `BasePage`.
3. Read `ClaimGridTest`. It's the data-validation idea behind the "structure correct, data wrong" bug.
4. Run `defect-demo.xml` and look at the failure screenshot in the report.
5. Run `api.xml` and compare each test with the APIM sheet.
6. Set up **one** CI tool by following its README, then the other.
7. Try these exercises:
   - Automate TC-020 by adding a "TPS down" switch to the mock server.
   - Add Allure reporting alongside Extent.
   - Run on Firefox.
   - Change `thread-count` in `regression.xml` and compare run times.

## Troubleshooting

| Problem | Fix |
|---|---|
| `Could not start mock server ... port 8089` | Another run (or `exec:java`) is still running. Stop it, or use `-Dmock.server.port=8099` together with matching `-Dapp.url`, `-Dapim.base.url`, `-Dservice.base.url`, and `-Dadmin.base.url`. |
| Browser doesn't start | Update Chrome. Behind a corporate proxy, Selenium Manager may not be able to download the driver; set `SE_CACHE_PATH` or point to a local driver. |
| Export tests can't find the CSV | Check `target/downloads/<thread>`. Some managed browsers block downloads by policy. |
| The Extent report in Jenkins looks blank | It's Jenkins' Content Security Policy; see `ci/jenkins/README.md`. |

## Disclaimer

The mock app, URLs, keys, patient names, and claim data are all fake and exist only for learning.
