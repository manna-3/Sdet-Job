package com.trcm.qa.tests.ui;

import com.trcm.qa.base.BaseTest;
import com.trcm.qa.utils.TestData;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Module: Re-validate (TC-043, TC-044). Fix the EDI, re-upload, and the errors should be gone. */
public class RevalidateTest extends BaseTest {

    @Test(description = "TC-043 / TC-044 Corrected EDI re-validates and previously rejected claims now pass",
            groups = {"regression", "revalidate"})
    public void correctedFilePassesValidation() {
        uploadAndReview(TestData.CLAIMS_50);
        Assert.assertEquals(page.getRejectedCount(), 10, "First run: 10 seeded errors");
        Assert.assertEquals(page.getCell("STRESS00005", "status"), "Rejected");

        uploadAndReview(TestData.CORRECTED_50);
        Assert.assertEquals(page.getRejectedCount(), 0, "Second run: all errors fixed");
        Assert.assertEquals(page.getCell("STRESS00005", "status"), "Accepted");
        Assert.assertEquals(page.getCell("STRESS00005", "reason"), "", "No rejection flag after the fix");
    }
}
