package com.trcm.qa.reports;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.trcm.qa.config.ConfigReader;

/**
 * Singleton that owns the one ExtentReports instance for the whole run.
 * Output: target/reports/ExtentReport.html (fixed name so CI can publish it).
 */
public final class ExtentManager {

    public static final String REPORT_PATH = "target/reports/ExtentReport.html";
    private static ExtentReports extent;

    private ExtentManager() {
    }

    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            ExtentSparkReporter spark = new ExtentSparkReporter(REPORT_PATH);
            spark.config().setDocumentTitle("TRCM QA Automation Report");
            spark.config().setReportName("TRCM Batch Review, Chatbot and APIM Tests");
            spark.config().setTheme(Theme.STANDARD);
            spark.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");

            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Environment", ConfigReader.env());
            extent.setSystemInfo("Browser", ConfigReader.get("browser"));
            extent.setSystemInfo("Headless", ConfigReader.get("headless"));
            extent.setSystemInfo("OS", System.getProperty("os.name"));
            extent.setSystemInfo("Java", System.getProperty("java.version"));
            String build = System.getenv("BUILD_NUMBER") != null ? System.getenv("BUILD_NUMBER") // Jenkins
                    : System.getenv("BUILD_BUILDNUMBER");                                        // Azure DevOps
            extent.setSystemInfo("CI build", build != null ? build : "local run");
        }
        return extent;
    }

    public static synchronized void flush() {
        if (extent != null) {
            extent.flush();
        }
    }
}
