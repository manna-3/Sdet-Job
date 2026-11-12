package com.trcm.qa.utils;

import com.trcm.qa.driver.DriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Takes a screenshot, saves a PNG under target/screenshots (published as a CI artifact)
 * and returns it as Base64 so it can be embedded straight into the Extent report.
 * Base64 embedding means the report still shows images after being moved or archived.
 */
public final class ScreenshotUtil {

    private static final Path DIR = Paths.get("target", "screenshots");

    private ScreenshotUtil() {
    }

    public static String captureBase64AndSave(String name) throws IOException {
        byte[] png = ((TakesScreenshot) DriverManager.getDriver()).getScreenshotAs(OutputType.BYTES);
        Files.createDirectories(DIR);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Files.write(DIR.resolve(name + "_" + stamp + ".png"), png);
        return Base64.getEncoder().encodeToString(png);
    }
}
