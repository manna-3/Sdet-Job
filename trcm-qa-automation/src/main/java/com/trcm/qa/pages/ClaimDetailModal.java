package com.trcm.qa.pages;

import com.trcm.qa.reports.ReportLogger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.stream.Collectors;

/** Page Object for the claim detail popup. */
public class ClaimDetailModal extends BasePage {

    private final By modal = By.id("detailModal");
    private final By title = By.id("detailTitle");
    private final By highlightedFields = By.cssSelector("#detailFields dd.rejected-field");
    private final By toggleJson = By.id("toggleJsonBtn");
    private final By rawJson = By.id("rawJson");
    private final By closeButton = By.id("closeDetailBtn");

    ClaimDetailModal waitUntilOpen() {
        visible(modal);
        return this;
    }

    public boolean isOpen() {
        return isDisplayed(modal);
    }

    public String getTitle() {
        return text(title);
    }

    public String getField(String field) {
        return text(By.cssSelector("#detailFields dd[data-field='" + field + "']"));
    }

    /** Returns the data-field names of every value highlighted as rejected. */
    public List<String> getHighlightedFields() {
        return driver.findElements(highlightedFields).stream()
                .map((WebElement e) -> e.getDomAttribute("data-field"))
                .collect(Collectors.toList());
    }

    public String viewRawJson() {
        ReportLogger.step("Click View raw JSON");
        click(toggleJson);
        return text(rawJson);
    }

    public void close() {
        ReportLogger.step("Close detail popup");
        click(closeButton);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(modal));
    }
}
