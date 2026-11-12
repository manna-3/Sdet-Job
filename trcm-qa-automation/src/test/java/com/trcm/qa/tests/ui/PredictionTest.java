package com.trcm.qa.tests.ui;

import com.trcm.qa.base.BaseTest;
import com.trcm.qa.utils.TestData;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.util.List;
import java.util.Map;

/** Module: Denial prediction (TC-032 to TC-036). */
public class PredictionTest extends BaseTest {

    private static int score(Map<String, String> row) {
        return Integer.parseInt(row.get("score").replace("%", "").trim());
    }

    @Test(description = "TC-032 Every reviewed claim shows a prediction score", groups = {"regression", "prediction"})
    public void everyClaimHasScore() {
        uploadAndReview(TestData.CLAIMS_50);
        SoftAssert soft = new SoftAssert();
        for (Map<String, String> row : page.getGridData()) {
            soft.assertTrue(row.get("score").matches("\\d{1,3}%"), "Bad score for " + row.get("claimId") + ": " + row.get("score"));
        }
        soft.assertAll();
    }

    @Test(description = "TC-033 Prediction scores are between 0% and 100%", groups = {"regression", "prediction"})
    public void scoresWithinRange() {
        uploadAndReview(TestData.CLAIMS_50);
        for (Map<String, String> row : page.getGridData()) {
            int s = score(row);
            Assert.assertTrue(s >= 0 && s <= 100, row.get("claimId") + " score out of range: " + s);
        }
    }

    @Test(description = "TC-034 Denied claims show a denial reason next to status and score", groups = {"regression", "prediction"})
    public void deniedClaimsShowReason() {
        uploadAndReview(TestData.CLAIMS_50);
        SoftAssert soft = new SoftAssert();
        page.getGridData().stream().filter(r -> r.get("status").equals("Rejected")).forEach(r ->
                soft.assertTrue(r.get("reason").matches("E\\d{3}: .+"),
                        "Missing or malformed reason for " + r.get("claimId") + ": '" + r.get("reason") + "'"));
        soft.assertAll();
    }

    @Test(description = "TC-035 Prediction is logically consistent: risky claims score higher than clean ones",
            groups = {"regression", "prediction"})
    public void predictionConsistentWithData() {
        uploadAndReview(TestData.CLAIMS_50);
        List<Map<String, String>> grid = page.getGridData();
        int minRejected = grid.stream().filter(r -> r.get("status").equals("Rejected")).mapToInt(PredictionTest::score).min().orElseThrow();
        int maxAccepted = grid.stream().filter(r -> r.get("status").equals("Accepted")).mapToInt(PredictionTest::score).max().orElseThrow();
        Assert.assertTrue(minRejected > maxAccepted,
                "Lowest rejected score (" + minRejected + ") should exceed highest accepted score (" + maxAccepted + ")");
    }

    @Test(description = "TC-036 Only genuinely invalid fields are flagged", groups = {"regression", "prediction"})
    public void onlyInvalidFieldsFlagged() {
        uploadAndReview(TestData.CLAIMS_50);
        // Seeded errors in test data: every 5th claim. E101 = missing ICD, E205 = bad CPT, E310 = bad NPI
        Assert.assertEquals(page.getCell("STRESS00005", "reason"), "E101: Missing primary diagnosis code (HI segment)");
        Assert.assertEquals(page.getCell("STRESS00010", "reason"), "E205: Invalid procedure code in SV1");
        Assert.assertEquals(page.getCell("STRESS00015", "reason"), "E310: Invalid rendering provider NPI");
        Assert.assertEquals(page.getCell("STRESS00001", "reason"), "", "A clean claim must not be flagged");
        Assert.assertEquals(page.getRejectedCount(), 10);
    }
}
