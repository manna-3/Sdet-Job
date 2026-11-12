# Running this project in Jenkins

This folder is self-contained: everything Jenkins needs is in `Jenkinsfile`. You don't need Azure DevOps to follow this guide.

## 1. Install Jenkins locally (easiest way to practise)

With Docker:

```bash
docker run -d --name jenkins -p 8080:8080 -p 50000:50000 -v jenkins_home:/var/jenkins_home jenkins/jenkins:lts-jdk17
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Open http://localhost:8080, paste the password, and choose "Install suggested plugins".

The UI tests need a browser on the agent. The plain Jenkins image doesn't include Chrome, so either install it inside the container (`apt-get install chromium`) or start with the API suite (`SUITE=api`), which needs no browser.

## 2. Install the plugins

Go to Manage Jenkins > Plugins > Available and install the ones the suggested set doesn't already include: HTML Publisher, JUnit, Credentials Binding, Timestamper, and Pipeline.

## 3. Configure tools

Under Manage Jenkins > Tools:

- Add a JDK named exactly `JDK17` (pointing at an installed JDK 17, or use the auto-installer).
- Add a Maven installation named exactly `Maven3` (tick "Install automatically").

These names must match the `tools { }` block in the Jenkinsfile.

## 4. Add secrets (only needed for ENV=dev or ENV=cert)

Under Manage Jenkins > Credentials > System > Global > Add credentials, create two credentials of kind **Secret text**:

| ID | Value |
|---|---|
| `trcm-apim-subscription-key` | your APIM subscription key |
| `trcm-service-api-key` | your backend service key |

For `ENV=local` you can skip this, because the mock uses built-in fake keys.

## 5. Create the pipeline job

1. Go to New Item, give it the name `trcm-qa-automation`, choose **Pipeline**, and click OK.
2. Under Pipeline > Definition, choose **Pipeline script from SCM**, set SCM to Git, and enter your repository URL and branch (`*/main`).
3. Set **Script Path** to `ci/jenkins/Jenkinsfile`.
4. Save, then click **Build Now** once. The first run registers the parameters.
5. From now on, use **Build with Parameters** to choose ENV, SUITE, and BROWSER.

## 6. Where to find the results

| What | Where |
|---|---|
| Pass/fail counts and trend graph | "Test Result" link on the build page (from the `junit` step) |
| Extent HTML report | "Extent Report" link in the left menu (from `publishHTML`) |
| Logs and failure screenshots | "Build Artifacts" (from `archiveArtifacts`) |
| Console output | "Console Output" (timestamps on every line) |

## 7. Fix: the Extent report looks blank or unstyled in Jenkins

Jenkins serves published HTML with a strict Content Security Policy that blocks the CSS and JavaScript the Extent report loads. To relax it for learning, open Manage Jenkins > Script Console and run:

```groovy
System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "")
```

This resets when Jenkins restarts. For a permanent setting, add `-Dhudson.model.DirectoryBrowserSupport.CSP=` to the Jenkins startup options. In a company setting, ask the Jenkins admin rather than disabling CSP globally.

## Stages in this pipeline

1. **Checkout**: pulls the code (`checkout scm`).
2. **Build**: `mvn clean test-compile`. A compile error fails fast here, before any browser opens.
3. **Run tests**: `mvn test` with the chosen suite, environment, and browser. Secrets are injected with `withCredentials` only for dev and cert.
4. **post / always**: publishes JUnit results, the Extent report, and artifacts, even when tests fail.

The nightly `cron('H 2 * * 1-5')` trigger uses the default parameters (the first item in each list), which are `local` and `regression`.
