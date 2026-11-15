package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules;

import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities.ShortReplyAssessment;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.transformers.ShortReplyAssessmentTransformer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Short-reply and reply-to-our-own-mail detection. */
class ShortReplyDetectorTest {

    private static final String LONG_REPORT = """
            Here is my accountability report for the week.
            I closed the diligence on the second fund and met with four founders,
            two of whom are now in the pipeline for next quarter. The operating
            review slipped by a day because of travel, and I have rescheduled it
            for Tuesday morning with the whole team present and prepared.
            """;

    @Test
    void aShortBodyIsAShortReply() {
        var result = ShortReplyDetector.evaluate("Re: Weekly report", "Got it, will send mine shortly.",
                null, null, null);
        assertTrue(result.isShortReply());
        assertFalse(result.isReplyToOurMail());
        assertTrue(result.reason().contains("below the threshold"));
    }

    @Test
    void anAcknowledgementPhraseIsAShortReply() {
        var result = ShortReplyDetector.evaluate("Re: Weekly report", "Thanks!", null, null, null);
        assertTrue(result.isShortReply());
        assertTrue(result.reason().contains("acknowledgement"));
    }

    @Test
    void acknowledgementMatchingIsCaseInsensitiveAndIgnoresTrailingPunctuation() {
        assertTrue(ShortReplyDetector.evaluate("s", "WILL DO.", null, null, null).isShortReply());
        assertTrue(ShortReplyDetector.evaluate("s", "  noted  ", null, null, null).isShortReply());
    }

    @Test
    void aRealReportIsNotAShortReply() {
        var result = ShortReplyDetector.evaluate("Weekly report", LONG_REPORT, null, null, null);
        assertFalse(result.isShortReply());
        assertTrue(result.ownWordCount() >= ShortReplyRules.WORD_THRESHOLD);
    }

    @Test
    void lengthThatComesOnlyFromQuotedHistoryDoesNotCount() {
        String body = "Thanks, see below.\n\nOn Mon, Nov 3, 2025 at 9:00 AM Jane Doe <jane@x.test> wrote:\n"
                + "> " + LONG_REPORT.replace("\n", "\n> ");
        var result = ShortReplyDetector.evaluate("Re: Weekly report", body, null, null, null);
        assertTrue(result.isShortReply());
        assertTrue(result.ownWordCount() < ShortReplyRules.WORD_THRESHOLD);
    }

    @Test
    void signatureAndDisclaimerAreStripped() {
        String body = "Short note.\n--\nJane Doe\nPartner\nConfidentiality notice: this is boilerplate "
                + "text that goes on and on and would otherwise push this message over the threshold "
                + "entirely by itself without any real content at all from the sender.";
        var result = ShortReplyDetector.evaluate("Re: hello", body, null, null, null);
        assertTrue(result.isShortReply());
        assertEquals(2, result.ownWordCount());
    }

    @Test
    void sentFromMyPhoneIsStripped() {
        assertEquals("Short note.",
                ShortReplyDetector.stripQuotedAndSignature("Short note.\nSent from my iPhone"));
    }

    @Test
    void originalMessageMarkerEndsTheSendersOwnText() {
        String body = "ok\n-----Original Message-----\nFrom: someone\n" + LONG_REPORT;
        assertEquals("ok", ShortReplyDetector.stripQuotedAndSignature(body));
    }

    @Test
    void inReplyToMatchingOurMessageIdIsAReplyToOurMail() {
        var result = ShortReplyDetector.evaluate("Re: Reminder", LONG_REPORT,
                "<abc123@mbl.example>", null, List.of("abc123@mbl.example"));
        assertTrue(result.isReplyToOurMail());
        assertFalse(result.isShortReply());
        assertTrue(result.reason().contains("replies to a message we sent"));
    }

    @Test
    void referencesContainingOneOfOurIdsAmongSeveralCounts() {
        var result = ShortReplyDetector.evaluate("Re: Reminder", LONG_REPORT, "<other@x.test>",
                "<first@x.test> <abc123@mbl.example> <third@x.test>", List.of("abc123@mbl.example"));
        assertTrue(result.isReplyToOurMail());
    }

    @Test
    void messageIdsCompareWithAndWithoutAngleBracketsAndCase() {
        var result = ShortReplyDetector.evaluate("Re: Reminder", LONG_REPORT, "ABC123@MBL.example", null,
                List.of("<abc123@mbl.example>"));
        assertTrue(result.isReplyToOurMail());
    }

    @Test
    void noThreadingHeadersMeansNotAReplyToOurMail() {
        var result = ShortReplyDetector.evaluate("Weekly report", LONG_REPORT, null, null,
                List.of("abc123@mbl.example"));
        assertFalse(result.isReplyToOurMail());
    }

    @Test
    void emptyOurMessageIdsNeverMatches() {
        var result = ShortReplyDetector.evaluate("Re: x", LONG_REPORT, "<abc@x.test>", null, List.of());
        assertFalse(result.isReplyToOurMail());
    }

    @Test
    void anEmptyBodyIsAShortReplyAndNeverThrows() {
        var result = ShortReplyDetector.evaluate("Re: x", "", null, null, null);
        assertTrue(result.isShortReply());
        assertEquals(0, result.ownWordCount());
    }

    @Test
    void transformerMapsEveryField() {
        var result = ShortReplyDetector.evaluate("Re: Weekly report", "Thanks!", "<a@x>", null,
                List.of("a@x"));
        ShortReplyAssessment entity = ShortReplyAssessmentTransformer.transform(result);
        assertEquals("Re: Weekly report", entity.subject);
        assertEquals(Boolean.TRUE, entity.isShortReply);
        assertEquals(Boolean.TRUE, entity.isReplyToOurMail);
        assertEquals(1.0, entity.ownWordCount);
        assertEquals(5, entity.toFields().size());
        assertNull(ShortReplyAssessmentTransformer.transform(null));
    }
}
