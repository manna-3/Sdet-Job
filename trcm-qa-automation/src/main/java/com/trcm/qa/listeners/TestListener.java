package com.trcm.qa.listeners;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.trcm.qa.driver.DriverManager;
import com.trcm.qa.reports.ExtentManager;
import com.trcm.qa.reports.ExtentTestManager;
import com.trcm.qa.utils.ScreenshotUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG listener = hooks that fire on test events. Registered in the suite XML files.
 * - Creates an Extent entry when each test starts
 * - Marks pass / fail / skip
 * - On failure: logs the exception and attaches a screenshot (UI tests only)
 * - Flushes the report when the suite finishes
 */
public class TestListener implements ITestListener, ISuiteListener {

    private static final Logger LOG = LogManager.getLogger(TestListener.class);

    @Override
    public void onStart(ISuite suite) {
        LOG.info("========== Suite started: {} ==========", suite.getName());
    }

    @Override
    public void onFinish(ISuite suite) {
        ExtentManager.flush();
        LOG.info("========== Suite finished: {}. Report: {} ==========", suite.getName(), ExtentManager.REPORT_PATH);
    }

    @Override
    public void onTestStart(ITestResult result) {
        String description = result.getMethod().getDescription();
        String name = description != null && !description.isBlank() ? description : result.getMethod().getMethodName();
        ExtentTest test;
        synchronized (ExtentManager.class) { // createTest is called from several threads in parallel runs
            test = ExtentManager.getInstance()
                    .createTest(name, result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName());
        }
        for (String group : result.getMethod().getGroups()) {
            test.assignCategory(group);
        }
        ExtentTestManager.setTest(test);
        LOG.info(">>> START {}", name);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTestManager.getTest().log(Status.PASS, "Test passed");
        LOG.info("<<< PASS {}", result.getMethod().getMethodName());
        ExtentTestManager.remove();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = ExtentTestManager.getTest();
        LOG.error("<<< FAIL {}", result.getMethod().getMethodName(), result.getThrowable());
        test.log(Status.FAIL, result.getThrowable());
        // The listener runs BEFORE @AfterMethod, so the browser is still open here.
        if (DriverManager.hasDriver()) {
            try {
                String base64 = ScreenshotUtil.captureBase64AndSave(result.getMethod().getMethodName());
                test.fail("Screenshot at failure",
                        MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
            } catch (Exception e) {
                LOG.warn("Could not capture screenshot: {}", e.getMessage());
            }
        }
        ExtentTestManager.remove();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = ExtentTestManager.getTest();
        if (result.wasRetried()) {
            // A retried attempt shows up as "skipped". Drop it so the report only shows the final attempt.
            if (test != null) {
                ExtentManager.getInstance().removeTest(test);
            }
            LOG.warn("<<< RETRYING {}", result.getMethod().getMethodName());
        } else {
            if (test == null) {
                test = ExtentManager.getInstance().createTest(result.getMethod().getMethodName());
            }
            test.log(Status.SKIP, result.getThrowable() != null ? result.getThrowable().getMessage() : "Test skipped");
            LOG.warn("<<< SKIP {}", result.getMethod().getMethodName());
        }
        ExtentTestManager.remove();
    }
}
