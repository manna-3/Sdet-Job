package com.trcm.qa.driver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory pattern: builds the right WebDriver for the requested browser.
 * No WebDriverManager needed: Selenium Manager (Selenium 4.6+) resolves drivers automatically.
 */
public final class DriverFactory {

    private static final Logger LOG = LogManager.getLogger(DriverFactory.class);

    private DriverFactory() {
    }

    public static WebDriver create(String browser, boolean headless, String downloadDir) {
        LOG.info("Creating {} driver (headless={})", browser, headless);
        switch (browser.toLowerCase()) {
            case "firefox":
                FirefoxOptions ff = new FirefoxOptions();
                if (headless) {
                    ff.addArguments("-headless");
                }
                ff.addPreference("browser.download.folderList", 2);
                ff.addPreference("browser.download.dir", downloadDir);
                ff.addPreference("browser.helperApps.neverAsk.saveToDisk", "text/csv");
                return new FirefoxDriver(ff);
            case "edge":
                EdgeOptions edge = new EdgeOptions();
                if (headless) {
                    edge.addArguments("--headless=new");
                }
                edge.addArguments("--window-size=1600,1000");
                edge.setExperimentalOption("prefs", downloadPrefs(downloadDir));
                return new EdgeDriver(edge);
            case "chrome":
            default:
                ChromeOptions chrome = new ChromeOptions();
                if (headless) {
                    chrome.addArguments("--headless=new");
                }
                // These flags keep Chrome stable inside CI agents and Docker containers
                chrome.addArguments("--window-size=1600,1000", "--no-sandbox",
                        "--disable-dev-shm-usage", "--disable-gpu", "--remote-allow-origins=*");
                chrome.setExperimentalOption("prefs", downloadPrefs(downloadDir));
                return new ChromeDriver(chrome);
        }
    }

    private static Map<String, Object> downloadPrefs(String downloadDir) {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir);
        prefs.put("download.prompt_for_download", false);
        prefs.put("safebrowsing.enabled", true);
        return prefs;
    }
}
