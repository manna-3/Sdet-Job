package com.trcm.qa.utils;

/** One claim as read from the SOURCE EDI file. Used as the "expected" side of data checks. */
public record EdiClaim(
        String claimId,
        String firstName,
        String lastName,
        String dob,
        String memberId,
        String providerName,
        String npi,
        String payerName,
        String payerId,
        String icd,
        String cpt,
        String units,
        String charge) {

    public String patientName() {
        return firstName + " " + lastName;
    }
}
