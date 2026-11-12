package com.trcm.qa.tests.ui;

import com.trcm.qa.base.BaseTest;
import com.trcm.qa.driver.DriverManager;
import com.trcm.qa.pages.ChatPanel;
import com.trcm.qa.utils.TestData;
import org.openqa.selenium.NoAlertPresentException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Chatbot: general cases (Part 1, TC-GEN-xx) and Bulk chat (Part 2, TC-BC-xx)
 * from Master_Chatbot_TestCases_Sheet.xlsx.
 *
 * Key idea for AI testing: the bot must be GROUNDED in the table data.
 * Expected answers are calculated from the grid, never hard-coded guesses,
 * and the bot must say "not available" instead of inventing (hallucinating) answers.
 */
public class BulkChatTest extends BaseTest {

    // ------------------------------------------------------------- general / smoke

    @Test(description = "TC-GEN-01 Chatbot loads with a greeting message", groups = {"smoke", "regression", "chatbot"})
    public void chatLoadsWithGreeting() {
        Assert.assertTrue(page.chat().getFirstBotMessage().contains("Batch Review assistant"));
    }

    @Test(description = "TC-GEN-02 Sample questions match the claim batch capabilities", groups = {"regression", "chatbot"})
    public void sampleQuestionsCorrect() {
        Assert.assertEquals(page.chat().getSampleQuestions(), List.of(
                "Give an overview of this batch execution", "Which claims were accepted?",
                "Which claims got rejected and why?", "What is the overall acceptance rate?"));
    }

    @Test(description = "TC-GEN-04 Bot responds to a greeting", groups = {"regression", "chatbot"})
    public void respondsToGreeting() {
        Assert.assertTrue(page.chat().ask("Hi").startsWith("Hello"));
    }

    @Test(description = "TC-GEN-05 / TC-GEN-06 Blank or whitespace-only input is ignored", groups = {"regression", "chatbot"})
    public void blankInputIgnored() {
        ChatPanel chat = page.chat();
        int before = chat.getMessageCount();
        chat.sendWithoutWaiting("");
        chat.sendWithoutWaiting("     ");
        Assert.assertTrue(chat.noNewMessageWithin(before, Duration.ofSeconds(2)), "Blank input should not create messages");
    }

    @Test(description = "TC-GEN-09 Out-of-scope question is politely redirected", groups = {"regression", "chatbot"})
    public void outOfScopeRedirected() {
        Assert.assertTrue(page.chat().ask("What's the weather today?").contains("only help with claim batch review"));
    }

    @Test(description = "TC-GEN-26 Script / SQL injection input is sanitized", groups = {"regression", "chatbot", "security"})
    public void injectionInputSanitized() {
        ChatPanel chat = page.chat();
        String script = "<script>alert(1)</script>";
        chat.ask(script);
        try {
            DriverManager.getDriver().switchTo().alert();
            Assert.fail("A JavaScript alert opened: the input was executed, not sanitized");
        } catch (NoAlertPresentException expected) {
            // good: nothing executed
        }
        Assert.assertEquals(chat.getLastUserMessage(), script, "Input should be shown as plain text");
        chat.ask("'; DROP TABLE claims;--");
        Assert.assertTrue(page.isLoaded(), "App still works after SQL-like input");
    }

    // ---------------------------------------------------------------- bulk chat

    @Test(description = "TC-BC24 Asking before review: bot says no results instead of fabricating a number",
            groups = {"regression", "chatbot", "hallucination"})
    public void askBeforeReview() {
        String reply = page.chat().ask("What is the overall acceptance rate?");
        Assert.assertTrue(reply.contains("No review results are available yet"), reply);
        Assert.assertFalse(reply.contains("%"), "Must not fabricate a percentage: " + reply);
    }

    @Test(description = "TC-BC17 Overview sample question matches the table stats exactly", groups = {"smoke", "regression", "chatbot"})
    public void overviewMatchesStats() {
        uploadAndReview(TestData.CLAIMS_50);
        String reply = page.chat().clickSampleQuestion("Give an overview of this batch execution");
        String expected = String.format("Batch overview: 1 file (%s), %d claims, %d rejected, %d accepted.",
                TestData.CLAIMS_50, page.getTotalCount(), page.getRejectedCount(), page.getAcceptedCount());
        Assert.assertEquals(reply, expected);
    }

    @Test(description = "TC-BC18 Accepted claims list matches the table", groups = {"regression", "chatbot"})
    public void acceptedClaimsMatchTable() {
        uploadAndReview(TestData.CLAIMS_50);
        String reply = page.chat().clickSampleQuestion("Which claims were accepted?");
        Assert.assertTrue(reply.startsWith(page.getAcceptedCount() + " claims were accepted"), reply);
        for (Map<String, String> row : page.getGridData()) {
            boolean listed = reply.contains(row.get("claimId"));
            Assert.assertEquals(listed, row.get("status").equals("Accepted"), "Wrong listing for " + row.get("claimId"));
        }
    }

    @Test(description = "TC-BC19 Rejected claims are listed with their actual edit-reason codes", groups = {"regression", "chatbot"})
    public void rejectedClaimsWithReasons() {
        uploadAndReview(TestData.CLAIMS_50);
        String reply = page.chat().clickSampleQuestion("Which claims got rejected and why?");
        Assert.assertTrue(reply.startsWith(page.getRejectedCount() + " claims were rejected"), reply);
        page.getGridData().stream().filter(r -> r.get("status").equals("Rejected")).forEach(r ->
                Assert.assertTrue(reply.contains(r.get("claimId") + " - " + r.get("reason")),
                        "Missing or wrong reason for " + r.get("claimId")));
    }

    @Test(description = "TC-BC23 Acceptance rate equals accepted / total from the table", groups = {"smoke", "regression", "chatbot"})
    public void acceptanceRateIsCorrect() {
        uploadAndReview(TestData.CLAIMS_50);
        double expectedPct = page.getAcceptedCount() * 100.0 / page.getTotalCount();
        String expected = String.format(Locale.US, "%.1f%%", expectedPct);
        String reply = page.chat().clickSampleQuestion("What is the overall acceptance rate?");
        Assert.assertTrue(reply.contains(expected), "Expected " + expected + " in: " + reply);
    }

    @Test(description = "TC-BC52 Same question gives the exact same answer every time (deterministic)",
            groups = {"regression", "chatbot"})
    public void answersAreDeterministic() {
        uploadAndReview(TestData.CLAIMS_50);
        ChatPanel chat = page.chat();
        String first = chat.ask("What is the overall acceptance rate?");
        Assert.assertEquals(chat.ask("What is the overall acceptance rate?"), first);
        Assert.assertEquals(chat.ask("What is the overall acceptance rate?"), first);
    }

    @Test(description = "TC-BC46 Unknown claim ID: bot says not found instead of inventing an answer",
            groups = {"regression", "chatbot", "hallucination"})
    public void unknownClaimNotInvented() {
        uploadAndReview(TestData.CLAIMS_50);
        Assert.assertEquals(page.chat().ask("Why was STRESS99999 rejected?"),
                "Claim STRESS99999 was not found in the current batch.");
    }

    @Test(description = "TC-BC47 Untracked data (trend over time): bot says it is not available",
            groups = {"regression", "chatbot", "hallucination"})
    public void untrackedDataNotFabricated() {
        uploadAndReview(TestData.CLAIMS_50);
        Assert.assertTrue(page.chat().ask("What's the trend in rejections over time?").contains("aren't tracked"));
    }

    @Test(description = "TC-BC48 Comparison with an inaccessible batch: bot states its limits",
            groups = {"regression", "chatbot", "hallucination"})
    public void noGuessingAboutOtherBatches() {
        uploadAndReview(TestData.CLAIMS_50);
        Assert.assertTrue(page.chat().ask("Is this batch worse than last week's?").contains("only have visibility"));
    }

    @Test(description = "TC-BC50 Typos and informal phrasing are still understood", groups = {"regression", "chatbot"})
    public void typosUnderstood() {
        uploadAndReview(TestData.CLAIMS_50);
        Assert.assertTrue(page.chat().ask("wich claims rejcted n y").contains("claims were rejected"),
                "Bot should infer the 'rejected' intent");
    }

    @Test(description = "TC-BC39 After Clear, bot does not answer from stale batch data",
            groups = {"regression", "chatbot", "clear"})
    public void noStaleDataAfterClear() {
        uploadAndReview(TestData.CLAIMS_50);
        page.clickClear();
        Assert.assertTrue(page.isGridEmpty(), "Grid should be empty after Clear");
        Assert.assertTrue(page.chat().ask("Give an overview of this batch execution")
                .contains("No batch data is currently loaded"));
    }
}
