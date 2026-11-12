package com.trcm.qa.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Resolves test data files. Selenium needs ABSOLUTE paths for file uploads. */
public final class TestData {

    public static final String CLAIMS_50 = "test_50_claims.edi";
    public static final String CLAIMS_49 = "test_49_claims.edi";
    public static final String CLAIMS_51 = "test_51_claims.edi";
    public static final String CLAIMS_100 = "test_100_claims.edi";
    public static final String CLAIM_1 = "test_1_claim.edi";
    public static final String CORRECTED_50 = "test_corrected_50_claims.edi";
    public static final String EMPTY = "test_empty.edi";
    public static final String CORRUPT = "test_corrupt.edi";
    public static final String RENAMED_PDF = "test_renamed.edi";
    public static final String CSV = "test_valid.csv";
    public static final String PDF = "test_valid.pdf";
    public static final String DOCX = "test_valid.docx";
    public static final String TXT = "test_valid.txt";

    private static final Path ROOT = Paths.get("src", "test", "resources", "testdata");

    private TestData() {
    }

    public static Path path(String fileName) {
        Path p = ROOT.resolve(fileName).toAbsolutePath();
        if (!Files.exists(p)) {
            throw new IllegalArgumentException("Test data file not found: " + p);
        }
        return p;
    }
}
