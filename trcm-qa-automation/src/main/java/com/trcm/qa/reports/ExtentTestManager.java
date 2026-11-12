package com.trcm.qa.reports;

import com.aventstack.extentreports.ExtentTest;

/** One ExtentTest node per thread, so parallel tests write to their own entry. */
public final class ExtentTestManager {

    private static final ThreadLocal<ExtentTest> TEST = new ThreadLocal<>();

    private ExtentTestManager() {
    }

    public static ExtentTest getTest() {
        return TEST.get();
    }

    public static void setTest(ExtentTest test) {
        TEST.set(test);
    }

    public static void remove() {
        TEST.remove();
    }
}
