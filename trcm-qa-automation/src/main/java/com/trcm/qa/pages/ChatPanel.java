package com.trcm.qa.pages;

import com.trcm.qa.reports.ReportLogger;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Page Object for the AI chatbot panel (Bulk chat and Claim-level chat).
 * The bot answers asynchronously, so ask() waits for a NEW bot message instead of sleeping.
 */
public class ChatPanel extends BasePage {

    private final By header = By.id("chatHeader");
    private final By batchButton = By.id("batchBtn");
    private final By sampleQuestions = By.cssSelector("#sampleQuestions .sample-q");
    private final By allMessages = By.cssSelector("#chatMessages .msg");
    private final By botMessages = By.cssSelector("#chatMessages .msg.bot");
    private final By userMessages = By.cssSelector("#chatMessages .msg.user");
    private final By input = By.id("chatInput");
    private final By sendButton = By.id("chatSendBtn");

    /** Types a question, sends it and returns the bot's reply. */
    public String ask(String question) {
        int before = driver.findElements(botMessages).size();
        ReportLogger.step("Ask chatbot: " + question);
        type(input, question);
        click(sendButton);
        return waitForReplyAfter(before);
    }

    /** Clicks one of the suggested questions and returns the reply. */
    public String clickSampleQuestion(String questionText) {
        int before = driver.findElements(botMessages).size();
        ReportLogger.step("Click sample question: " + questionText);
        driver.findElements(sampleQuestions).stream()
                .filter(b -> b.getText().trim().equals(questionText))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Sample question not found: " + questionText))
                .click();
        return waitForReplyAfter(before);
    }

    /** Sends text without waiting for a reply (used for blank-input tests). */
    public void sendWithoutWaiting(String text) {
        ReportLogger.step("Send raw chat input: '" + text + "'");
        WebElement box = visible(input);
        box.clear();
        box.sendKeys(text);
        click(sendButton);
    }

    private String waitForReplyAfter(int previousBotCount) {
        wait.until(d -> d.findElements(botMessages).size() > previousBotCount);
        String reply = getLastBotMessage();
        ReportLogger.step("Bot replied: " + reply);
        return reply;
    }

    public String getLastBotMessage() {
        List<WebElement> msgs = driver.findElements(botMessages);
        return msgs.isEmpty() ? "" : msgs.get(msgs.size() - 1).getText().trim();
    }

    public String getFirstBotMessage() {
        return visible(botMessages).getText().trim();
    }

    public String getLastUserMessage() {
        List<WebElement> msgs = driver.findElements(userMessages);
        return msgs.isEmpty() ? "" : msgs.get(msgs.size() - 1).getText().trim();
    }

    /**
     * For "nothing should happen" checks (e.g. blank input). A bounded wait that is
     * EXPECTED to time out. Still no Thread.sleep(): it returns early if a message appears.
     */
    public boolean noNewMessageWithin(int previousCount, Duration duration) {
        try {
            new WebDriverWait(driver, duration).until(d -> d.findElements(allMessages).size() > previousCount);
            return false;
        } catch (TimeoutException expected) {
            return true;
        }
    }

    public int getMessageCount() {
        return driver.findElements(allMessages).size();
    }

    public String getHeader() {
        return text(header);
    }

    public List<String> getSampleQuestions() {
        return driver.findElements(sampleQuestions).stream().map(e -> e.getText().trim()).collect(Collectors.toList());
    }

    public boolean isBatchButtonVisible() {
        return isDisplayed(batchButton);
    }

    public ChatPanel backToBatch() {
        ReportLogger.step("Click Batch to return to Bulk chat");
        click(batchButton);
        wait.until(d -> d.findElement(header).getText().trim().equals("Bulk chat"));
        return this;
    }
}
