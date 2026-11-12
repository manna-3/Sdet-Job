package com.trcm.qa.listeners;

import com.trcm.qa.config.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Re-runs a failed test up to retry.count times (0 locally, 1 in dev/cert).
 * Use it only to absorb environment flakiness. A test that needs a retry to pass
 * should still be investigated, not ignored.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger LOG = LogManager.getLogger(RetryAnalyzer.class);
    private final int maxRetries = ConfigReader.getInt("retry.count");
    private int attempt = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (attempt < maxRetries) {
            attempt++;
            LOG.warn("Retrying {} (attempt {} of {})", result.getMethod().getMethodName(), attempt, maxRetries);
            return true;
        }
        return false;
    }
}
