package com.trcm.qa.base;

import com.trcm.qa.config.ConfigReader;
import com.trcm.qa.driver.DriverFactory;
import com.trcm.qa.driver.DriverManager;
import com.trcm.qa.pages.BatchReviewPage;
import com.trcm.qa.reports.ReportLogger;
import com.trcm.qa.utils.FileWaitUtil;
import com.trcm.qa.utils.TestData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Parent class for all UI tests.
 *
 * Lifecycle:
 *   @BeforeSuite  start the mock server once (local env only)
 *   @BeforeMethod fresh browser per test -> tests are independent and can run in any order
 *   @AfterMethod  quit the browser (runs AFTER the listener has taken a failure screenshot)
 *
 * alwaysRun = true makes these run even when the suite filters by groups (smoke, regression).
 */
public abstract class BaseTest {

    protected final Logger log = LogManager.getLogger(getClass());
    protected BatchReviewPage page;

    @BeforeSuite(alwaysRun = true)
    public void startEnvironment() {
        MockServerHook.startIfEnabled();
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws IOException {
        String threadName = Thread.currentThread().getName().replaceAll("[^A-Za-z0-9_-]", "_");
        String downloadDir = Paths.get("target", "downloads", threadName).toAbsolutePath().toString();
        FileWaitUtil.cleanDirectory(downloadDir);

        WebDriver driver = DriverFactory.create(
                ConfigReader.get("browser"), ConfigReader.getBoolean("headless"), downloadDir);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(ConfigReader.getInt("implicit.wait.seconds")));
        DriverManager.setDriver(driver, downloadDir);

        page = new BatchReviewPage().open();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        DriverManager.quitDriver();
    }

    @AfterSuite(alwaysRun = true)
    public void stopEnvironment() {
        MockServerHook.stop();
    }

    /** Shared precondition used by many tests: upload a file and wait for the review to finish. */
    protected void uploadAndReview(String fileName) {
        ReportLogger.step("Precondition: upload " + fileName + " and run review");
        page.upload(TestData.path(fileName));
        page.runReviewAndWait();
    }
}
