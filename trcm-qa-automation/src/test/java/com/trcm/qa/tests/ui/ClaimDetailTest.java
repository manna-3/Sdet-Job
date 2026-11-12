package com.trcm.qa.tests.ui;

import com.trcm.qa.base.BaseTest;
import com.trcm.qa.pages.ClaimDetailModal;
import com.trcm.qa.reports.ReportLogger;
import com.trcm.qa.utils.EdiClaim;
import com.trcm.qa.utils.EdiParser;
import com.trcm.qa.utils.TestData;
import io.restassured.path.json.JsonPath;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.IOException;
import java.util.List;

/** Module: Claim Detail popup (TC-027 to TC-031). */
public class ClaimDetailTest extends BaseTest {

    private static final String CLEAN_CLAIM = "STRESS00003";
    private static final String REJECTED_CLAIM = "STRESS00005"; // seeded with a missing ICD-10 code

    @Test(description = "TC-027 Detail popup opens and shows patient, provider, payer, diagnosis and service line",
            groups = {"smoke", "regression", "detail"})
    public void detailPopupOpens() {
        uploadAndReview(TestData.CLAIMS_50);
        ClaimDetailModal modal = page.openDetails(CLEAN_CLAIM);
        Assert.assertTrue(modal.isOpen());
        Assert.assertEquals(modal.getTitle(), "Claim detail: " + CLEAN_CLAIM);
        for (String field : List.of("patientName", "providerName", "payerName", "icd", "cpt")) {
            Assert.assertFalse(modal.getField(field).isBlank(), field + " should be shown");
        }
    }

    @Test(description = "TC-028 Detail view shows correct extracted fields (matches source EDI)", groups = {"regression", "detail"})
    public void detailFieldsMatchSource() throws IOException {
        uploadAndReview(TestData.CLAIMS_50);
        EdiClaim expected = EdiParser.parse(TestData.path(TestData.CLAIMS_50)).stream()
                .filter(c -> c.claimId().equals(CLEAN_CLAIM)).findFirst().orElseThrow();
        ClaimDetailModal modal = page.openDetails(CLEAN_CLAIM);

        SoftAssert soft = new SoftAssert();
        soft.assertEquals(modal.getField("patientName"), expected.patientName(), "patient");
        soft.assertEquals(modal.getField("dob"), expected.dob(), "dob");
        soft.assertEquals(modal.getField("memberId"), expected.memberId(), "member id");
        soft.assertEquals(modal.getField("npi"), expected.npi(), "npi");
        soft.assertEquals(modal.getField("payerId"), expected.payerId(), "payer id");
        soft.assertEquals(modal.getField("icd"), expected.icd(), "icd");
        soft.assertEquals(modal.getField("cpt"), expected.cpt(), "cpt");
        soft.assertEquals(modal.getField("charge"), expected.charge(), "charge");
        soft.assertAll();
    }

    @Test(description = "TC-029 Rejected fields are highlighted; clean claims have no highlights", groups = {"regression", "detail"})
    public void rejectedFieldsHighlighted() {
        uploadAndReview(TestData.CLAIMS_50);
        ClaimDetailModal rejected = page.openDetails(REJECTED_CLAIM);
        Assert.assertEquals(rejected.getHighlightedFields(), List.of("icd"), "Only the missing ICD should be flagged");
        Assert.assertEquals(rejected.getField("icd"), "(missing)");
        rejected.close();

        ClaimDetailModal clean = page.openDetails(CLEAN_CLAIM);
        Assert.assertTrue(clean.getHighlightedFields().isEmpty(), "A clean claim must show no flags");
    }

    @Test(description = "TC-030 Detail view shows valid, readable raw JSON", groups = {"regression", "detail"})
    public void rawJsonIsValid() {
        uploadAndReview(TestData.CLAIMS_50);
        String json = page.openDetails(REJECTED_CLAIM).viewRawJson();
        ReportLogger.json("Raw JSON", json);
        JsonPath parsed = new JsonPath(json); // throws if the JSON is malformed
        Assert.assertEquals(parsed.getString("claimId"), REJECTED_CLAIM);
        Assert.assertEquals(parsed.getString("prediction.status"), "Rejected");
        Assert.assertTrue(parsed.getList("actionEdits").contains("E101"));
    }

    @Test(description = "TC-031 Detail view closes cleanly and the grid is unchanged", groups = {"regression", "detail"})
    public void detailClosesCleanly() {
        uploadAndReview(TestData.CLAIMS_50);
        ClaimDetailModal modal = page.openDetails(CLEAN_CLAIM);
        modal.close();
        Assert.assertFalse(modal.isOpen());
        Assert.assertEquals(page.getRowCount(), 50, "Grid must remain intact");
    }
}
