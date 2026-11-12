package com.trcm.qa.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal 837 parser used by the tests as an independent "source of truth".
 *
 * Why this exists: the critical bug in this project was that the converter's output
 * was STRUCTURALLY correct but carried the WRONG DATA. Checking structure is not enough,
 * so tests parse the source EDI themselves and compare every field with what the UI shows.
 *
 * Segments used (separated by ~, elements by *):
 *   CLM*claimId*charge              claim start
 *   NM1*IL*1*LAST*FIRST****MI*id    patient / subscriber
 *   DMG*D8*CCYYMMDD                 date of birth
 *   NM1*85*2*NAME*****XX*npi        rendering provider
 *   NM1*PR*2*NAME*****PI*id         payer
 *   HI*ABK:code                     primary diagnosis (ICD-10)
 *   SV1*HC:cpt*charge*UN*units      service line
 */
public final class EdiParser {

    private EdiParser() {
    }

    public static List<EdiClaim> parse(Path file) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        List<EdiClaim> claims = new ArrayList<>();
        Builder current = null;

        for (String raw : content.split("~")) {
            String segment = raw.trim();
            if (segment.isEmpty()) {
                continue;
            }
            String[] el = segment.split("\\*", -1); // -1 keeps empty trailing elements
            switch (el[0]) {
                case "CLM" -> {
                    if (current != null) {
                        claims.add(current.build());
                    }
                    current = new Builder();
                    current.claimId = el[1];
                    current.charge = el[2];
                }
                case "NM1" -> {
                    if (current == null) {
                        break;
                    }
                    switch (el[1]) {
                        case "IL" -> {
                            current.lastName = el[3];
                            current.firstName = el[4];
                            current.memberId = el[9];
                        }
                        case "85" -> {
                            current.providerName = el[3];
                            current.npi = el[9];
                        }
                        case "PR" -> {
                            current.payerName = el[3];
                            current.payerId = el[9];
                        }
                        default -> { }
                    }
                }
                case "DMG" -> {
                    if (current != null) {
                        String d = el[2];
                        current.dob = d.substring(0, 4) + "-" + d.substring(4, 6) + "-" + d.substring(6, 8);
                    }
                }
                case "HI" -> {
                    if (current != null) {
                        current.icd = after(el[1], ':');
                    }
                }
                case "SV1" -> {
                    if (current != null) {
                        current.cpt = after(el[1], ':');
                        current.units = el[4];
                    }
                }
                default -> { }
            }
        }
        if (current != null) {
            claims.add(current.build());
        }
        return claims;
    }

    private static String after(String value, char sep) {
        int i = value.indexOf(sep);
        return i < 0 ? "" : value.substring(i + 1);
    }

    private static final class Builder {
        String claimId, firstName, lastName, dob, memberId, providerName, npi,
                payerName, payerId, icd = "", cpt = "", units, charge;

        EdiClaim build() {
            return new EdiClaim(claimId, firstName, lastName, dob, memberId, providerName, npi,
                    payerName, payerId, icd, cpt, units, charge);
        }
    }
}
