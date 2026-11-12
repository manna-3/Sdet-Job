package com.trcm.qa.tests.ui;

import com.trcm.qa.base.BaseTest;
import com.trcm.qa.utils.TestData;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Module: Run Review (TC-014 to TC-018). */
public class RunReviewTest extends BaseTest {

    @Test(description = "TC-014 Run review button is disabled before upload", groups = {"regression", "review"})
    public void runReviewDisabledBeforeUpload() {
        Assert.assertFalse(page.isRunReviewEnabled(), "Run review must be disabled on a fresh page");
    }

    @Test(description = "TC-015 Run review completes with a valid 50-claim file", groups = {"smoke", "regression", "review"})
    public void runReviewWith50Claims() {
        uploadAndReview(TestData.CLAIMS_50);
        Assert.assertEquals(page.getRowCount(), 50, "One grid row per claim");
        Assert.assertEquals(page.getTotalCount(), 50);
    }

    @Test(description = "TC-016 Progress indicator shows and button is disabled while review runs", groups = {"regression", "review"})
    public void progressIndicatorDuringReview() {
        page.upload(TestData.path(TestData.CLAIMS_50));
        page.clickRunReview();
        Assert.assertTrue(page.isSpinnerDisplayed(), "Spinner should be visible during processing");
        Assert.assertFalse(page.isRunReviewEnabled(), "Button should be disabled while running");
        page.waitForReviewToFinish();
        Assert.assertFalse(page.isSpinnerDisplayed(), "Spinner should disappear when done");
    }

    @Test(description = "TC-017 Run review stays disabled after a 51-claim file is rejected", groups = {"regression", "review"})
    public void runReviewDisabledAfterLimitError() {
        page.upload(TestData.path(TestData.CLAIMS_50));
        Assert.assertTrue(page.isRunReviewEnabled(), "Sanity check: enabled after a valid upload");
        page.upload(TestData.path(TestData.CLAIMS_51));
        Assert.assertFalse(page.isRunReviewEnabled(), "A failed upload must disable Run review again");
    }

    @Test(description = "TC-018 Double-clicking Run review processes the batch only once", groups = {"regression", "review"})
    public void doubleClickRunsOnce() {
        page.upload(TestData.path(TestData.CLAIMS_50));
        page.doubleClickRunReview();
        page.waitForReviewToFinish();
        Assert.assertEquals(page.getReviewRunCount(), 1, "Review must run only once");
        Assert.assertEquals(page.getRowCount(), 50, "No duplicate rows");
    }
}
