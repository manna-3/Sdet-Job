package com.trcm.qa.tests.ui;

import com.trcm.qa.base.BaseTest;
import com.trcm.qa.reports.ReportLogger;
import com.trcm.qa.utils.EdiClaim;
import com.trcm.qa.utils.EdiParser;
import com.trcm.qa.utils.TestData;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Module: Claim Grid (TC-021 to TC-026). DATA VALIDATION against the source EDI.
 *
 * This is the "structure is right but data is wrong" check: every value in the grid
 * is compared with the same field parsed independently from the uploaded file.
 * SoftAssert collects ALL mismatches before failing, so one run lists every bad field.
 */
public class ClaimGridTest extends BaseTest {

    @Test(description = "TC-021 All 50 claims appear as rows with no duplicates or missing rows",
            groups = {"smoke", "regression", "grid"})
    public void all50ClaimsShown() throws IOException {
        uploadAndReview(TestData.CLAIMS_50);
        List<Map<String, String>> grid = page.getGridData();
        List<String> expectedIds = EdiParser.parse(TestData.path(TestData.CLAIMS_50)).stream()
                .map(EdiClaim::claimId).collect(Collectors.toList());
        List<String> actualIds = grid.stream().map(r -> r.get("claimId")).collect(Collectors.toList());

        Assert.assertEquals(grid.size(), 50, "Row count");
        Assert.assertEquals(actualIds.stream().distinct().count(), 50L, "Claim IDs must be unique");
        Assert.assertEquals(actualIds, expectedIds, "Same claims, same order as the source file");
    }

    @Test(description = "TC-022 Each row shows correct patient data (name, DOB, member ID)", groups = {"smoke", "regression", "grid"})
    public void patientDataMatchesSource() throws IOException {
        uploadAndReview(TestData.CLAIMS_50);
        SoftAssert soft = new SoftAssert();
        compare(soft, "patientName", EdiClaim::patientName);
        compare(soft, "dob", EdiClaim::dob);
        compare(soft, "memberId", EdiClaim::memberId);
        soft.assertAll();
    }

    @Test(description = "TC-023 Each row shows correct provider data (NPI, name)", groups = {"regression", "grid"})
    public void providerDataMatchesSource() throws IOException {
        uploadAndReview(TestData.CLAIMS_50);
        SoftAssert soft = new SoftAssert();
        compare(soft, "npi", EdiClaim::npi);
        compare(soft, "providerName", EdiClaim::providerName);
        soft.assertAll();
    }

    @Test(description = "TC-024 Each row shows correct payer data (ID, name)", groups = {"regression", "grid"})
    public void payerDataMatchesSource() throws IOException {
        uploadAndReview(TestData.CLAIMS_50);
        SoftAssert soft = new SoftAssert();
        compare(soft, "payerId", EdiClaim::payerId);
        compare(soft, "payerName", EdiClaim::payerName);
        soft.assertAll();
    }

    @Test(description = "TC-025 Each row shows correct ICD-10 diagnosis codes", groups = {"regression", "grid"})
    public void diagnosisCodesMatchSource() throws IOException {
        uploadAndReview(TestData.CLAIMS_50);
        SoftAssert soft = new SoftAssert();
        compare(soft, "icd", EdiClaim::icd);
        soft.assertAll();
    }

    @Test(description = "TC-026 Each row shows correct service line details (CPT, units, charge)", groups = {"regression", "grid"})
    public void serviceLinesMatchSource() throws IOException {
        uploadAndReview(TestData.CLAIMS_50);
        SoftAssert soft = new SoftAssert();
        compare(soft, "cpt", EdiClaim::cpt);
        compare(soft, "units", EdiClaim::units);
        compare(soft, "charge", EdiClaim::charge);
        soft.assertAll();
    }

    /** Compares one grid column with the matching field from the source EDI, for every claim. */
    private void compare(SoftAssert soft, String column, Function<EdiClaim, String> expectedField) throws IOException {
        Map<String, EdiClaim> source = EdiParser.parse(TestData.path(TestData.CLAIMS_50)).stream()
                .collect(Collectors.toMap(EdiClaim::claimId, c -> c));
        List<Map<String, String>> grid = page.getGridData();
        int checked = 0;
        for (Map<String, String> row : grid) {
            EdiClaim expected = source.get(row.get("claimId"));
            soft.assertNotNull(expected, "Grid shows a claim that is not in the source file: " + row.get("claimId"));
            if (expected != null) {
                soft.assertEquals(row.get(column), expectedField.apply(expected),
                        "Mismatch in column '" + column + "' for " + row.get("claimId"));
                checked++;
            }
        }
        ReportLogger.step("Compared column '" + column + "' for " + checked + " claims against source EDI");
    }
}
