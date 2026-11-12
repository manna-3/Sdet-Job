package com.trcm.qa.reports;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.markuputils.CodeLanguage;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Writes a step to BOTH places at once:
 *   - Log4j2 (console + target/logs/automation.log)
 *   - the Extent HTML report (as a step under the current test)
 */
public final class ReportLogger {

    private static final Logger LOG = LogManager.getLogger("STEP");

    private ReportLogger() {
    }

    public static void step(String message) {
        LOG.info(message);
        ExtentTest test = ExtentTestManager.getTest();
        if (test != null) {
            test.info(message);
        }
    }

    public static void json(String title, String json) {
        LOG.info("{}: {}", title, json);
        ExtentTest test = ExtentTestManager.getTest();
        if (test != null) {
            test.info(title);
            test.info(MarkupHelper.createCodeBlock(json, CodeLanguage.JSON));
        }
    }
}
