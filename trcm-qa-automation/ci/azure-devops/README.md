# Running this project in Azure DevOps

This folder is self-contained: everything Azure DevOps needs is in `azure-pipelines.yml`. You don't need Jenkins to follow this guide.

## 1. Get a free Azure DevOps organisation

Go to https://dev.azure.com and sign in with a Microsoft account. Create an organisation and a project (for example `TRCM-QA`). The free tier includes Microsoft-hosted build minutes. New organisations sometimes have to request the free parallel job grant before pipelines can run; the portal shows a message with a link if that applies to you.

## 2. Push the code to Azure Repos (or connect GitHub)

```bash
git init
git add .
git commit -m "TRCM QA automation framework"
git remote add origin https://dev.azure.com/<org>/<project>/_git/trcm-qa-automation
git push -u origin main
```

## 3. Create the pipeline

1. Go to Pipelines > New pipeline.
2. Pick where your code is (Azure Repos Git or GitHub) and select the repository.
3. Choose **Existing Azure Pipelines YAML file**.
4. Select the branch `main` and the path `/ci/azure-devops/azure-pipelines.yml`.
5. Click **Run**.

## 4. Add secrets (only needed for environment=dev or cert)

1. Go to Pipelines > Library > **+ Variable group** and name it `trcm-qa-secrets`.
2. Add `apimSubscriptionKey` and `serviceApiKey`, and click the lock icon on each to mark it **secret**.
3. Under Pipeline permissions, allow this pipeline to use the group.
4. In `azure-pipelines.yml`, uncomment the line `# - group: trcm-qa-secrets`.

Secret variables are masked as `***` in logs and are not given to scripts automatically. That's why the YAML maps them explicitly in the `env:` block of the Maven task. Real teams often link the variable group to **Azure Key Vault** so secrets are managed in one place.

## 5. Running with different options

Click **Run pipeline** and pick values for Environment, TestNG suite, and Browser. These come from the `parameters:` section.

Automatic runs use the defaults (`local`, `regression`, `chrome`):

| Trigger | When it runs |
|---|---|
| `trigger:` | every push to main (CI) |
| `pr:` | every pull request into main (quality gate). For Azure Repos, this is enforced with a Branch Policy > Build validation. |
| `schedules:` | nightly at 20:30 UTC (02:00 IST), Mon-Fri |

## 6. Where to find the results

| What | Where |
|---|---|
| Pass/fail counts, history, flaky tests | the **Tests** tab of the run (from `publishJUnitResults`) |
| Extent HTML report | the run summary > **Published** artifacts > `ExtentReport-1` > download and open `ExtentReport.html` |
| Logs and screenshots | the `Logs-1` and `Screenshots-1` artifacts |

## Stages in this pipeline

1. **Build**: restores the Maven cache and runs `mvn clean test-compile`. A broken build fails here in about a minute.
2. **Test**: runs `mvn clean test` with the chosen suite, environment, and browser, and publishes JUnit results to the Tests tab.
3. Artifacts are published with `condition: always()`, so you still get the report and screenshots when tests fail.

## Jenkins vs Azure DevOps: the same concepts with different names

| Concept | Jenkins | Azure DevOps |
|---|---|---|
| Pipeline file | `Jenkinsfile` (Groovy) | `azure-pipelines.yml` (YAML) |
| Run-time inputs | `parameters { choice(...) }` | `parameters:` |
| Nightly run | `triggers { cron(...) }` | `schedules:` |
| Secrets | Credentials + `withCredentials` | Variable group / Key Vault + `env:` mapping |
| Machine that runs it | agent / node | agent pool (`vmImage: ubuntu-latest`) |
| Test results tab | `junit` step | `publishJUnitResults: true` |
| Keep files | `archiveArtifacts` | `PublishPipelineArtifact@1` |
| Always-run cleanup | `post { always { } }` | `condition: always()` |
