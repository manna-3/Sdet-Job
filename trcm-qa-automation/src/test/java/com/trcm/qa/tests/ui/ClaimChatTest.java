package com.trcm.qa.tests.ui;

import com.trcm.qa.base.BaseTest;
import com.trcm.qa.pages.ChatPanel;
import com.trcm.qa.utils.TestData;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Chatbot: Bulk <-> Claim-level navigation and context isolation (Sections D, TC-BC31 onward).
 * Context bleed (one claim's answer showing under another claim) is treated as a critical defect.
 */
public class ClaimChatTest extends BaseTest {

    private static final String REJECTED_CLAIM = "STRESS00005";

    /** Runs after BaseTest.setUp(): TestNG runs superclass @BeforeMethod first. */
    @BeforeMethod(alwaysRun = true)
    public void reviewBatch() {
        uploadAndReview(TestData.CLAIMS_50);
    }

    @Test(description = "TC-BC31 Clicking Ask opens claim-level chat with the claim header and claim questions",
            groups = {"smoke", "regression", "chatbot"})
    public void askOpensClaimChat() {
        ChatPanel chat = page.clickAsk(REJECTED_CLAIM);
        Assert.assertTrue(chat.getHeader().startsWith("CLM " + REJECTED_CLAIM), chat.getHeader());
        Assert.assertTrue(chat.getHeader().contains(TestData.CLAIMS_50), "Header should show the file name");
        Assert.assertTrue(chat.isBatchButtonVisible());
        Assert.assertEquals(chat.getSampleQuestions(), List.of("Why was this claim rejected?",
                "Summarize this claim in plain English", "List the recommended fixes from Action Edits"));
    }

    @Test(description = "TC-BC32 Batch button returns to Bulk chat with batch questions restored", groups = {"regression", "chatbot"})
    public void backToBatch() {
        ChatPanel chat = page.clickAsk(REJECTED_CLAIM).backToBatch();
        Assert.assertEquals(chat.getHeader(), "Bulk chat");
        Assert.assertEquals(chat.getSampleQuestions().get(0), "Give an overview of this batch execution");
    }

    @Test(description = "Claim chat explains the real rejection reason for that claim", groups = {"regression", "chatbot"})
    public void claimRejectionReason() {
        String expectedReason = page.getCell(REJECTED_CLAIM, "reason");
        String reply = page.clickAsk(REJECTED_CLAIM).clickSampleQuestion("Why was this claim rejected?");
        Assert.assertTrue(reply.contains(expectedReason), "Reply should quote the grid reason: " + reply);
    }

    @Test(description = "TC-BC55 Recommended fixes are tied to the claim's actual edit codes", groups = {"regression", "chatbot"})
    public void recommendedFixesSpecific() {
        String reply = page.clickAsk(REJECTED_CLAIM).clickSampleQuestion("List the recommended fixes from Action Edits");
        Assert.assertTrue(reply.contains("Add the primary ICD-10 diagnosis code"), reply);
    }

    @Test(description = "TC-BC56 Plain-English summary matches the claim's real data", groups = {"regression", "chatbot"})
    public void plainEnglishSummaryAccurate() {
        String patient = page.getCell(REJECTED_CLAIM, "patientName");
        String charge = page.getCell(REJECTED_CLAIM, "charge");
        String reply = page.clickAsk(REJECTED_CLAIM).clickSampleQuestion("Summarize this claim in plain English");
        Assert.assertTrue(reply.contains(patient) && reply.contains("$" + charge) && reply.contains("rejected"), reply);
    }

    @Test(description = "TC-BC57 Asking about another claim inside claim chat stays scoped", groups = {"regression", "chatbot"})
    public void otherClaimStaysScoped() {
        String reply = page.clickAsk(REJECTED_CLAIM).ask("What about STRESS00002?");
        Assert.assertTrue(reply.contains("scoped to CLM " + REJECTED_CLAIM), reply);
    }

    @Test(description = "TC-BC36 Claim chat does not carry over batch chat history", groups = {"regression", "chatbot"})
    public void claimChatIsolatedFromBatchChat() {
        page.chat().ask("Give an overview of this batch execution");
        ChatPanel claimChat = page.clickAsk(REJECTED_CLAIM);
        Assert.assertEquals(claimChat.getMessageCount(), 1, "Only the claim intro message should be shown");
        Assert.assertFalse(claimChat.getLastBotMessage().contains("Batch overview"));
    }

    @Test(description = "TC-BC61 Bulk -> claim -> bulk round trip keeps the batch chat history", groups = {"regression", "chatbot"})
    public void roundTripKeepsHistory() {
        ChatPanel chat = page.chat();
        chat.ask("Give an overview of this batch execution");
        int before = chat.getMessageCount();
        page.clickAsk(REJECTED_CLAIM).backToBatch();
        Assert.assertEquals(chat.getMessageCount(), before, "Batch history should be intact");
    }
}
