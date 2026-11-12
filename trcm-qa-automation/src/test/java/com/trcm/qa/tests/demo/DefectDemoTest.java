package com.trcm.qa.tests.demo;

import com.trcm.qa.base.BaseTest;
import com.trcm.qa.utils.TestData;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * LEARNING DEMO ONLY. This test FAILS ON PURPOSE.
 * Run:  mvn test -Dsuite=src/test/resources/suites/defect-demo.xml
 * Then open target/reports/ExtentReport.html to see what a failure looks like:
 * the stack trace, the embedded screenshot, and the log steps leading up to it.
 * It is NOT part of smoke or regression, so it never breaks the CI pipeline.
 */
public class DefectDemoTest extends BaseTest {

    @Test(description = "DEMO Intentional failure: shows the screenshot and stack trace in the report", groups = {"demo"})
    public void intentionalFailureForReportDemo() {
        uploadAndReview(TestData.CLAIMS_50);
        // Wrong on purpose: STRESS00005 is really rejected with E101
        Assert.assertEquals(page.getCell("STRESS00005", "status"), "Accepted",
                "DEMO: expected 'Accepted' to show a failure in the report");
    }
}
