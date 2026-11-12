package com.trcm.qa.tests.ui;

import com.trcm.qa.base.BaseTest;
import com.trcm.qa.reports.ReportLogger;
import com.trcm.qa.utils.TestData;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Module: File Upload (BatchReview_QA_TestCases.xlsx, TC-001 to TC-013).
 * Techniques: Boundary Value Analysis on the 50-claim limit (49 / 50 / 51 / 1 / 0)
 * and Equivalence Partitioning on file types (.edi valid vs csv/pdf/docx/txt invalid).
 */
public class FileUploadTest extends BaseTest {

    private static final String LIMIT_ERROR = "Exceeds the 50 claim limit. Please remove claims and re-upload.";
    private static final String FORMAT_ERROR = "Invalid file format. Only .edi files are accepted.";

    @Test(description = "TC-001 Upload valid .edi file with exactly 50 claims", groups = {"smoke", "regression", "upload"})
    public void uploadExactly50Claims() {
        String message = page.upload(TestData.path(TestData.CLAIMS_50));
        Assert.assertEquals(message, "File uploaded successfully: 50 claims found.");
        Assert.assertFalse(page.isUploadError(), "No error should be shown");
        Assert.assertTrue(page.isRunReviewEnabled(), "Run review should become enabled");
    }

    @Test(description = "TC-002 Upload .edi file with 51 claims (boundary +1) is rejected", groups = {"smoke", "regression", "upload"})
    public void upload51ClaimsRejected() {
        String message = page.upload(TestData.path(TestData.CLAIMS_51));
        Assert.assertEquals(message, LIMIT_ERROR);
        Assert.assertTrue(page.isUploadError());
        Assert.assertFalse(page.isRunReviewEnabled(), "Run review must stay disabled");
    }

    @Test(description = "TC-003 Upload .edi file with 100 claims is rejected", groups = {"regression", "upload"})
    public void upload100ClaimsRejected() {
        Assert.assertEquals(page.upload(TestData.path(TestData.CLAIMS_100)), LIMIT_ERROR);
        Assert.assertFalse(page.isRunReviewEnabled(), "Run review must stay disabled");
    }

    @Test(description = "TC-004 Upload .edi file with 49 claims (boundary -1) and review shows 49 rows", groups = {"regression", "upload"})
    public void upload49ClaimsAccepted() {
        Assert.assertEquals(page.upload(TestData.path(TestData.CLAIMS_49)), "File uploaded successfully: 49 claims found.");
        page.runReviewAndWait();
        Assert.assertEquals(page.getRowCount(), 49);
    }

    @Test(description = "TC-005 Upload .edi file with 1 claim (minimum) shows a single row", groups = {"regression", "upload"})
    public void uploadSingleClaim() {
        Assert.assertEquals(page.upload(TestData.path(TestData.CLAIM_1)), "File uploaded successfully: 1 claims found.");
        page.runReviewAndWait();
        Assert.assertEquals(page.getRowCount(), 1);
    }

    @Test(description = "TC-006 Upload empty .edi file (0 claims) is rejected", groups = {"regression", "upload"})
    public void uploadEmptyFileRejected() {
        Assert.assertEquals(page.upload(TestData.path(TestData.EMPTY)), "File is empty or contains no valid claims.");
        Assert.assertFalse(page.isRunReviewEnabled());
    }

    @DataProvider(name = "wrongFormats")
    public Object[][] wrongFormats() {
        return new Object[][]{
                {"TC-007", TestData.CSV},
                {"TC-008", TestData.PDF},
                {"TC-009", TestData.DOCX},
                {"TC-010", TestData.TXT},
        };
    }

    @Test(description = "TC-007 to TC-010 Wrong file formats (csv, pdf, docx, txt) are rejected",
            dataProvider = "wrongFormats", groups = {"regression", "upload"})
    public void wrongFormatRejected(String tcId, String fileName) {
        ReportLogger.step(tcId + ": uploading " + fileName);
        Assert.assertEquals(page.upload(TestData.path(fileName)), FORMAT_ERROR, tcId + " failed for " + fileName);
        Assert.assertFalse(page.isRunReviewEnabled(), tcId + ": Run review must stay disabled");
    }

    @Test(description = "TC-011 Upload a corrupted .edi file shows a validation error", groups = {"regression", "upload"})
    public void uploadCorruptedEdi() {
        Assert.assertEquals(page.upload(TestData.path(TestData.CORRUPT)), "File is not a valid EDI format.");
        Assert.assertTrue(page.isLoaded(), "App must not crash");
    }

    @Test(description = "TC-012 Upload without selecting any file", groups = {"regression", "upload"})
    public void uploadWithoutFile() {
        Assert.assertEquals(page.upload(null), "No file selected. Please choose an EDI file.");
        Assert.assertFalse(page.isRunReviewEnabled());
    }

    @Test(description = "TC-013 PDF renamed to .edi is rejected (content is validated, not just extension)",
            groups = {"regression", "upload"})
    public void renamedExtensionRejected() {
        Assert.assertEquals(page.upload(TestData.path(TestData.RENAMED_PDF)), "File is not a valid EDI format.");
        Assert.assertFalse(page.isRunReviewEnabled());
    }
}
