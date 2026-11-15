package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules;

import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities.FilingDecision;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.FilingDecisionEngine.FilingDecisionResult;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.FilingDecisionEngine.ModelReadingSignal;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.FilingDecisionEngine.PeriodSignal;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.FilingDecisionEngine.SenderSignal;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.FilingDecisionEngine.ShortReplySignal;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.transformers.FilingDecisionTransformer;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The hard filing rules. */
class FilingDecisionEngineTest {

    private static final LocalDate WEEK = LocalDate.of(2025, 11, 3);
    private static final DayOfWeek WEEK_START = DayOfWeek.MONDAY;

    private static SenderSignal exactSender() {
        return new SenderSignal("Exact", "r-1", "c-1", 1, false);
    }

    private static PeriodSignal highPeriod() {
        return new PeriodSignal(true, WEEK, WEEK.plusDays(6), "Subject: week of Nov 3", "High");
    }

    private static ShortReplySignal normalMail() {
        return new ShortReplySignal(false, false, "The message body has 120 words of its own.");
    }

    private static ModelReadingSignal ownReport() {
        return new ModelReadingSignal("own report", WEEK, "High");
    }

    private static FilingDecisionResult decide(SenderSignal sender, PeriodSignal period,
                                               ShortReplySignal shortReply, ModelReadingSignal model) {
        return FilingDecisionEngine.decide(sender, period, shortReply, model, WEEK_START);
    }

    @Test
    void everythingAgreeingFilesAsReport() {
        var result = decide(exactSender(), highPeriod(), normalMail(), ownReport());
        assertEquals(FilingDecisionEngine.FILE_AS_REPORT, result.decision());
        assertEquals("r-1", result.reporterId());
        assertEquals(WEEK, result.weekStart());
        assertFalse(result.reason().isBlank());
    }

    @Test
    void anAliasSenderFilesTheSameWayAsAnExactSender() {
        var result = decide(new SenderSignal("Alias", "r-1", "c-1", 1, false), highPeriod(), normalMail(),
                ownReport());
        assertEquals(FilingDecisionEngine.FILE_AS_REPORT, result.decision());
    }

    @Test
    void anUnknownSenderIsHeldForReviewAndNeverAdopted() {
        var result = decide(new SenderSignal("None", "", "", 0, false), highPeriod(), normalMail(),
                ownReport());
        assertEquals(FilingDecisionEngine.HOLD_FOR_REVIEW, result.decision());
        assertEquals("", result.reporterId());
        assertTrue(result.reason().contains("not a known reporter"));
    }

    @Test
    void anAmbiguousSenderIsHeldForReviewWithTheCandidateCount() {
        var result = decide(new SenderSignal("Ambiguous", "", "", 3, false), highPeriod(), normalMail(),
                ownReport());
        assertEquals(FilingDecisionEngine.HOLD_FOR_REVIEW, result.decision());
        assertTrue(result.reason().contains("3 active reporters"));
    }

    @Test
    void aShortReplyIsSetAsideEvenWhenEverythingElseAgrees() {
        var result = decide(exactSender(), highPeriod(),
                new ShortReplySignal(true, false, "The message body has 3 words."), ownReport());
        assertEquals(FilingDecisionEngine.SET_ASIDE, result.decision());
        assertTrue(result.reason().contains("short reply"));
    }

    @Test
    void aReplyToOurOwnMailIsSetAside() {
        var result = decide(exactSender(), highPeriod(),
                new ShortReplySignal(false, true, "It replies to a message we sent."), ownReport());
        assertEquals(FilingDecisionEngine.SET_ASIDE, result.decision());
        assertTrue(result.reason().contains("reply to mail we sent"));
    }

    @Test
    void aShortReplyFromAnUnknownSenderIsSetAsideNotHeld() {
        var result = decide(new SenderSignal("None", "", "", 0, false), highPeriod(),
                new ShortReplySignal(true, false, "Short."), ownReport());
        assertEquals(FilingDecisionEngine.SET_ASIDE, result.decision());
    }

    @Test
    void mailFromTheReportingMailboxIsSetAside() {
        var result = decide(new SenderSignal("None", "", "", 0, true), highPeriod(), normalMail(),
                ownReport());
        assertEquals(FilingDecisionEngine.SET_ASIDE, result.decision());
        assertTrue(result.reason().contains("reporting mailbox"));
    }

    @Test
    void aReadingOfNotAReportIsSetAside() {
        var result = decide(exactSender(), highPeriod(), normalMail(),
                new ModelReadingSignal("not a report", WEEK, "High"));
        assertEquals(FilingDecisionEngine.SET_ASIDE, result.decision());
    }

    @Test
    void disagreeingWeeksAreHeldForReviewWithBothValuesInTheReason() {
        var result = decide(exactSender(), highPeriod(), normalMail(),
                new ModelReadingSignal("own report", LocalDate.of(2025, 10, 27), "High"));
        assertEquals(FilingDecisionEngine.HOLD_FOR_REVIEW, result.decision());
        assertTrue(result.reason().contains("2025-11-03"), "stated week must be named");
        assertTrue(result.reason().contains("2025-10-27"), "model week must be named");
    }

    @Test
    void weeksAreComparedAfterNormalisingToTheWeekStart() {
        // The model names the Thursday of the same week; that is not a disagreement.
        var result = decide(exactSender(), highPeriod(), normalMail(),
                new ModelReadingSignal("own report", LocalDate.of(2025, 11, 6), "High"));
        assertEquals(FilingDecisionEngine.FILE_AS_REPORT, result.decision());
    }

    @Test
    void aLowConfidencePeriodIsHeldForReview() {
        var result = decide(exactSender(),
                new PeriodSignal(true, WEEK, WEEK.plusDays(6), "Body: last week", "Low"), normalMail(),
                ownReport());
        assertEquals(FilingDecisionEngine.HOLD_FOR_REVIEW, result.decision());
        assertTrue(result.reason().contains("low confidence"));
    }

    @Test
    void aLowConfidenceReadingIsHeldForReview() {
        var result = decide(exactSender(), highPeriod(), normalMail(),
                new ModelReadingSignal("own report", WEEK, "Low"));
        assertEquals(FilingDecisionEngine.HOLD_FOR_REVIEW, result.decision());
        assertTrue(result.reason().contains("not high confidence"));
    }

    @Test
    void noStatedPeriodIsHeldForReviewEvenWithAConfidentReading() {
        var result = decide(exactSender(), new PeriodSignal(false, null, null, "", ""), normalMail(),
                ownReport());
        assertEquals(FilingDecisionEngine.HOLD_FOR_REVIEW, result.decision());
        assertTrue(result.reason().contains("does not state its reporting week"));
    }

    @Test
    void noWeekAnywhereIsHeldForReview() {
        var result = decide(exactSender(), new PeriodSignal(false, null, null, "", ""), normalMail(),
                new ModelReadingSignal("own report", null, "High"));
        assertEquals(FilingDecisionEngine.HOLD_FOR_REVIEW, result.decision());
        assertNull(result.weekStart());
        assertTrue(result.reason().contains("No reporting week"));
    }

    @Test
    void aCompanyUpdateWithACompanyIdFilesUnderCompany() {
        var result = decide(exactSender(), highPeriod(), normalMail(),
                new ModelReadingSignal("company update", WEEK, "High"));
        assertEquals(FilingDecisionEngine.FILE_UNDER_COMPANY, result.decision());
        assertEquals("c-1", result.companyId());
    }

    @Test
    void aCompanyUpdateWithoutACompanyIdIsHeldForReview() {
        var result = decide(new SenderSignal("Exact", "r-1", "", 1, false), highPeriod(), normalMail(),
                new ModelReadingSignal("company update", WEEK, "High"));
        assertEquals(FilingDecisionEngine.HOLD_FOR_REVIEW, result.decision());
        assertTrue(result.reason().contains("no company id"));
    }

    @Test
    void anUnrecognisedKindIsHeldForReview() {
        var result = decide(exactSender(), highPeriod(), normalMail(),
                new ModelReadingSignal("newsletter", WEEK, "High"));
        assertEquals(FilingDecisionEngine.HOLD_FOR_REVIEW, result.decision());
        assertTrue(result.reason().contains("newsletter"));
    }

    @Test
    void kindAndConfidenceComparisonsAreCaseInsensitive() {
        var result = decide(exactSender(), highPeriod(), normalMail(),
                new ModelReadingSignal("Own Report", WEEK, "HIGH"));
        assertEquals(FilingDecisionEngine.FILE_AS_REPORT, result.decision());
    }

    @Test
    void everyDecisionCarriesAReason() {
        assertFalse(decide(exactSender(), highPeriod(), normalMail(), ownReport()).reason().isBlank());
        assertFalse(decide(new SenderSignal("None", "", "", 0, false), highPeriod(), normalMail(),
                ownReport()).reason().isBlank());
        assertFalse(decide(exactSender(), highPeriod(), new ShortReplySignal(true, false, ""),
                ownReport()).reason().isBlank());
    }

    @Test
    void transformerMapsTheDecisionAndFormatsTheWeek() {
        FilingDecision entity = FilingDecisionTransformer.transform(
                decide(exactSender(), highPeriod(), normalMail(), ownReport()));
        assertEquals("File as Report", entity.decision);
        assertEquals("2025-11-03", entity.weekStart);
        assertEquals(5, entity.toFields().size());
        assertNull(FilingDecisionTransformer.transform(null));
    }
}
