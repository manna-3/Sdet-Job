package com.trcm.qa.tests.ui;

import com.trcm.qa.base.BaseTest;
import com.trcm.qa.driver.DriverManager;
import com.trcm.qa.reports.ReportLogger;
import com.trcm.qa.utils.CsvReader;
import com.trcm.qa.utils.FileWaitUtil;
import com.trcm.qa.utils.TestData;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Module: Export (TC-037 to TC-042). Verifies the downloaded CSV file itself, not just the button. */
public class ExportTest extends BaseTest {

    private Path exportCsv() throws Exception {
        page.clickExport();
        Path file = FileWaitUtil.waitForDownload(DriverManager.getDownloadDir(), "batch_review_export_", ".csv",
                Duration.ofSeconds(15));
        ReportLogger.step("Downloaded: " + file.getFileName());
        return file;
    }

    @Test(description = "TC-037 Export button is available after review", groups = {"regression", "export"})
    public void exportEnabledAfterReview() {
        uploadAndReview(TestData.CLAIMS_50);
        Assert.assertTrue(page.isExportEnabled());
    }

    @Test(description = "TC-038 Export downloads a CSV with a meaningful file name", groups = {"regression", "export"})
    public void exportDownloadsCsv() throws Exception {
        uploadAndReview(TestData.CLAIMS_50);
        Path file = exportCsv();
        Assert.assertTrue(file.getFileName().toString().matches("batch_review_export_\\d{8}\\.csv"),
                "Unexpected file name: " + file.getFileName());
    }

    @Test(description = "TC-039 Exported CSV contains all 50 claims (51 lines with header)", groups = {"smoke", "regression", "export"})
    public void exportContainsAllClaims() throws Exception {
        uploadAndReview(TestData.CLAIMS_50);
        Path file = exportCsv();
        Assert.assertEquals(CsvReader.totalLineCount(file), 51, "1 header + 50 data rows");
        Assert.assertEquals(CsvReader.rows(file).size(), 50);
    }

    @Test(description = "TC-040 Exported CSV contains all expected columns", groups = {"regression", "export"})
    public void exportHasExpectedColumns() throws Exception {
        uploadAndReview(TestData.CLAIMS_50);
        List<String> expected = List.of("Claim ID", "Patient Name", "DOB", "Member ID", "Provider NPI", "Provider Name",
                "Payer ID", "Payer Name", "ICD-10", "CPT", "Units", "Charge", "Denial Score", "Status", "Rejection Reason");
        Assert.assertEquals(CsvReader.header(exportCsv()), expected);
    }

    @Test(description = "TC-041 Exported CSV data matches the UI grid", groups = {"regression", "export"})
    public void exportMatchesGrid() throws Exception {
        uploadAndReview(TestData.CLAIMS_50);
        List<Map<String, String>> grid = page.getGridData();
        List<Map<String, String>> csv = CsvReader.rows(exportCsv());
        Assert.assertEquals(csv.size(), grid.size(), "Row count");

        SoftAssert soft = new SoftAssert();
        for (int i = 0; i < grid.size(); i++) {
            Map<String, String> ui = grid.get(i);
            Map<String, String> file = csv.get(i);
            String id = ui.get("claimId");
            soft.assertEquals(file.get("Claim ID"), id, "Row " + i);
            soft.assertEquals(file.get("Patient Name"), ui.get("patientName"), id + " patient");
            soft.assertEquals(file.get("Charge"), ui.get("charge"), id + " charge");
            soft.assertEquals(file.get("Denial Score"), ui.get("score"), id + " score");
            soft.assertEquals(file.get("Status"), ui.get("status"), id + " status");
            soft.assertEquals(file.get("Rejection Reason"), ui.get("reason"), id + " reason");
        }
        soft.assertAll();
    }

    @Test(description = "TC-042 Export is disabled before a review is run", groups = {"regression", "export"})
    public void exportDisabledBeforeReview() {
        page.upload(TestData.path(TestData.CLAIMS_50));
        Assert.assertFalse(page.isExportEnabled(), "Export must stay disabled until the review completes");
    }
}
