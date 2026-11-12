package com.trcm.qa.driver;

import org.openqa.selenium.WebDriver;

/**
 * Holds one WebDriver per thread (ThreadLocal) so tests can run in parallel
 * without two tests sharing, and fighting over, the same browser.
 */
public final class DriverManager {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();
    private static final ThreadLocal<String> DOWNLOAD_DIR = new ThreadLocal<>();

    private DriverManager() {
    }

    public static WebDriver getDriver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("WebDriver not initialised for thread " + Thread.currentThread().getName());
        }
        return driver;
    }

    public static boolean hasDriver() {
        return DRIVER.get() != null;
    }

    public static void setDriver(WebDriver driver, String downloadDir) {
        DRIVER.set(driver);
        DOWNLOAD_DIR.set(downloadDir);
    }

    public static String getDownloadDir() {
        return DOWNLOAD_DIR.get();
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            driver.quit();
        }
        DRIVER.remove(); // avoid memory leaks in thread pools
        DOWNLOAD_DIR.remove();
    }
}
