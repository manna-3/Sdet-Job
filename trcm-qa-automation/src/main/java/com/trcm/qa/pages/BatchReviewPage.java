package com.trcm.qa.pages;

import com.trcm.qa.config.ConfigReader;
import com.trcm.qa.reports.ReportLogger;
import org.openqa.selenium.By;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Page Object for the Batch Review screen: upload, review, grid, export, clear. */
public class BatchReviewPage extends BasePage {

    // ---- Locators: kept in one place, so a UI change means one edit here, not in every test
    private final By fileInput = By.id("fileInput");
    private final By uploadButton = By.id("uploadBtn");
    private final By uploadMessage = By.id("uploadMessage");
    private final By runReviewButton = By.id("runReviewBtn");
    private final By spinner = By.id("spinner");
    private final By claimRows = By.cssSelector("#claimsBody tr.claim-row");
    private final By emptyRow = By.id("emptyRow");
    private final By statsTotal = By.id("statsTotal");
    private final By statsAccepted = By.id("statsAccepted");
    private final By statsRejected = By.id("statsRejected");
    private final By exportButton = By.id("exportBtn");
    private final By clearButton = By.id("clearBtn");
    private final By envBadge = By.id("envBadge");

    public BatchReviewPage open() {
        String url = ConfigReader.get("app.url");
        ReportLogger.step("Open Batch Review page: " + url);
        driver.get(url);
        visible(uploadButton);
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(uploadButton) && isDisplayed(By.id("chatPanel"));
    }

    public String getEnvironmentBadge() {
        return text(envBadge);
    }

    /** Uploads a file and waits for the validation message. Pass null to click Upload with no file. */
    public String upload(Path file) {
        if (file != null) {
            ReportLogger.step("Select file: " + file.getFileName());
            // For <input type=file>, sendKeys with an absolute path selects the file. No OS dialog needed.
            driver.findElement(fileInput).sendKeys(file.toAbsolutePath().toString());
        } else {
            ReportLogger.step("Click Upload without selecting a file");
        }
        click(uploadButton);
        wait.until(d -> !d.findElement(uploadMessage).getText().isBlank());
        String message = text(uploadMessage);
        ReportLogger.step("Upload message: " + message);
        return message;
    }

    public String getUploadMessage() {
        return driver.findElement(uploadMessage).getText().trim();
    }

    public boolean isUploadError() {
        return driver.findElement(uploadMessage).getDomAttribute("class").contains("error");
    }

    public boolean isRunReviewEnabled() {
        return isEnabled(runReviewButton);
    }

    public void clickRunReview() {
        ReportLogger.step("Click Run review");
        click(runReviewButton);
    }

    public void doubleClickRunReview() {
        ReportLogger.step("Double-click Run review");
        new Actions(driver).doubleClick(driver.findElement(runReviewButton)).perform();
    }

    public boolean isSpinnerDisplayed() {
        return isDisplayed(spinner);
    }

    /** Clicks Run review and waits until processing finishes (spinner gone, rows rendered). */
    public BatchReviewPage runReviewAndWait() {
        clickRunReview();
        waitForReviewToFinish();
        return this;
    }

    public void waitForReviewToFinish() {
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(ConfigReader.getInt("review.wait.seconds")));
        longWait.until(ExpectedConditions.invisibilityOfElementLocated(spinner));
        longWait.until(ExpectedConditions.presenceOfElementLocated(claimRows));
        ReportLogger.step("Review finished with " + getRowCount() + " rows");
    }

    /** How many times the app actually started a review (exposed by the mock for TC-018). */
    public int getReviewRunCount() {
        return Integer.parseInt(driver.findElement(By.tagName("body")).getDomAttribute("data-review-count"));
    }

    public int getRowCount() {
        return driver.findElements(claimRows).size();
    }

    public boolean isGridEmpty() {
        return isDisplayed(emptyRow);
    }

    /**
     * Reads the whole grid in ONE JavaScript call instead of 50 rows x 15 cells = 750
     * findElement round-trips. Much faster for data-validation tests.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getGridData() {
        return (List<Map<String, String>>) js(
                "return Array.from(document.querySelectorAll('#claimsBody tr.claim-row')).map(tr => {"
                        + "  const row = {};"
                        + "  tr.querySelectorAll('td[data-col]').forEach(td => row[td.dataset.col] = td.textContent.trim());"
                        + "  return row; });");
    }

    public String getCell(String claimId, String column) {
        return driver.findElement(By.cssSelector(
                "tr.claim-row[data-claim-id='" + claimId + "'] td[data-col='" + column + "']")).getText().trim();
    }

    public int getTotalCount() {
        return Integer.parseInt(text(statsTotal));
    }

    public int getAcceptedCount() {
        return Integer.parseInt(text(statsAccepted));
    }

    public int getRejectedCount() {
        return Integer.parseInt(text(statsRejected));
    }

    public ClaimDetailModal openDetails(String claimId) {
        ReportLogger.step("Open Details for " + claimId);
        click(By.cssSelector("tr.claim-row[data-claim-id='" + claimId + "'] .details-btn"));
        return new ClaimDetailModal().waitUntilOpen();
    }

    public ChatPanel clickAsk(String claimId) {
        ReportLogger.step("Click Ask for " + claimId);
        click(By.cssSelector("tr.claim-row[data-claim-id='" + claimId + "'] .ask-btn"));
        return new ChatPanel();
    }

    public boolean isExportEnabled() {
        return isEnabled(exportButton);
    }

    public void clickExport() {
        ReportLogger.step("Click Export CSV");
        click(exportButton);
    }

    public void clickClear() {
        ReportLogger.step("Click Clear");
        click(clearButton);
    }

    public ChatPanel chat() {
        return new ChatPanel();
    }
}
