package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Combines the sender, period, short-reply and model signals into one filing decision.
 *
 * <p>The hard rules MBL set, enforced in this exact order:
 * <ol>
 *   <li>A short reply, a reply to our own mail, or mail from the reporting mailbox is
 *       <strong>Set Aside</strong> — never filed, whoever it came from.</li>
 *   <li>A reading of "not a report" is <strong>Set Aside</strong>.</li>
 *   <li>An unknown or ambiguous sender is <strong>Hold for Review</strong> — never auto-adopted.</li>
 *   <li>A stated period that disagrees with the model's week is <strong>Hold for Review</strong>, with
 *       both values named in the reason.</li>
 *   <li>Anything less than high confidence on both signals, or no agreed week at all, is
 *       <strong>Hold for Review</strong>.</li>
 *   <li>A company update is <strong>File under Company</strong> only when the reporter actually has a
 *       company id; without one it is <strong>Hold for Review</strong>.</li>
 *   <li>Only when sender, kind and week all agree with high confidence is it
 *       <strong>File as Report</strong>.</li>
 * </ol>
 */
public final class FilingDecisionEngine {

    public static final String FILE_AS_REPORT = "File as Report";
    public static final String FILE_UNDER_COMPANY = "File under Company";
    public static final String HOLD_FOR_REVIEW = "Hold for Review";
    public static final String SET_ASIDE = "Set Aside";

    public static final String KIND_OWN_REPORT = "own report";
    public static final String KIND_COMPANY_UPDATE = "company update";
    public static final String KIND_NOT_A_REPORT = "not a report";

    private FilingDecisionEngine() {
    }

    public static FilingDecisionResult decide(SenderSignal sender, PeriodSignal period,
                                              ShortReplySignal shortReply, ModelReadingSignal model,
                                              DayOfWeek weekStart) {
        // 1. Noise never gets filed.
        if (shortReply.isReplyToOurMail()) {
            return setAside(sender, null, "The message is a reply to mail we sent, so it is not a new report."
                    + detail(shortReply));
        }
        if (shortReply.isShortReply()) {
            return setAside(sender, null, "The message is a short reply, not a report." + detail(shortReply));
        }
        if (sender.isReportingMailbox()) {
            return setAside(sender, null,
                    "The message was sent from the shared reporting mailbox itself, so it is skipped.");
        }

        // 2. The model says this is not a report at all.
        if (Validation.equalsIgnoreCase(model.kind(), KIND_NOT_A_REPORT)) {
            return setAside(sender, null, "The reading of the message is 'not a report'.");
        }

        LocalDate periodWeek = period.periodFound()
                ? WeekCalculator.startOfWeek(period.weekStart(), weekStart) : null;
        LocalDate modelWeek = WeekCalculator.startOfWeek(model.week(), weekStart);

        // 3. An unknown or ambiguous sender is never adopted automatically.
        if (SenderMatcher.AMBIGUOUS.equalsIgnoreCase(sender.matchType())) {
            return hold(sender, firstNonNull(periodWeek, modelWeek),
                    "The sender matches " + sender.candidateCount()
                            + " active reporters, so it is ambiguous and cannot be adopted automatically.");
        }
        if (!SenderMatcher.EXACT.equalsIgnoreCase(sender.matchType())
                && !SenderMatcher.ALIAS.equalsIgnoreCase(sender.matchType())) {
            return hold(sender, firstNonNull(periodWeek, modelWeek),
                    "The sender is not a known reporter, so it cannot be adopted automatically.");
        }

        // 4. The stated period and the model's reading must not contradict each other.
        if (periodWeek != null && modelWeek != null && !periodWeek.equals(modelWeek)) {
            return hold(sender, periodWeek,
                    "The week stated in the message (" + Dates.formatDate(periodWeek)
                            + ") and the week in the reading (" + Dates.formatDate(modelWeek)
                            + ") disagree.");
        }

        // 5. Confidence and an agreed week are both required before anything is filed.
        boolean periodHigh = Validation.equalsIgnoreCase(period.confidence(), StatedPeriodDetector.HIGH);
        boolean modelHigh = Validation.equalsIgnoreCase(model.confidence(), StatedPeriodDetector.HIGH);
        LocalDate agreedWeek = firstNonNull(periodWeek, modelWeek);
        if (agreedWeek == null) {
            return hold(sender, null, "No reporting week could be established from the message or the reading.");
        }
        if (periodWeek != null && !periodHigh) {
            return hold(sender, agreedWeek, "The week stated in the message was read with low confidence ("
                    + describeEvidence(period) + ").");
        }
        if (periodWeek == null) {
            return hold(sender, agreedWeek,
                    "The message does not state its reporting week, so the reading alone is not enough to file it.");
        }
        if (!modelHigh) {
            return hold(sender, agreedWeek, "The reading of the message is not high confidence"
                    + (Validation.isBlank(model.confidence()) ? "" : " (" + model.confidence().trim() + ")") + ".");
        }

        // 6/7. Everything agrees — file it.
        if (Validation.equalsIgnoreCase(model.kind(), KIND_COMPANY_UPDATE)) {
            if (Validation.isBlank(sender.companyId())) {
                return hold(sender, agreedWeek, "The message reads as a company update, but "
                        + describeReporter(sender) + " has no company id to file it under.");
            }
            return new FilingDecisionResult(FILE_UNDER_COMPANY, sender.reporterId(), sender.companyId(),
                    agreedWeek, "Company update from " + describeReporter(sender) + " for the week of "
                    + Dates.formatDate(agreedWeek) + "; sender, kind and week all agree with high confidence.");
        }
        if (Validation.equalsIgnoreCase(model.kind(), KIND_OWN_REPORT)) {
            return new FilingDecisionResult(FILE_AS_REPORT, sender.reporterId(), sender.companyId(),
                    agreedWeek, "Own report from " + describeReporter(sender) + " for the week of "
                    + Dates.formatDate(agreedWeek) + "; sender, kind and week all agree with high confidence.");
        }
        return hold(sender, agreedWeek, "The reading of the message is '"
                + Validation.trimToEmpty(model.kind()) + "', which is not a recognized kind.");
    }

    private static FilingDecisionResult hold(SenderSignal sender, LocalDate week, String reason) {
        return new FilingDecisionResult(HOLD_FOR_REVIEW, sender.reporterId(), sender.companyId(), week, reason);
    }

    private static FilingDecisionResult setAside(SenderSignal sender, LocalDate week, String reason) {
        return new FilingDecisionResult(SET_ASIDE, sender.reporterId(), sender.companyId(), week, reason);
    }

    private static String detail(ShortReplySignal shortReply) {
        return Validation.isBlank(shortReply.reason()) ? "" : " " + shortReply.reason().trim();
    }

    private static String describeEvidence(PeriodSignal period) {
        return Validation.isBlank(period.evidenceText())
                ? "confidence " + Validation.trimToEmpty(period.confidence())
                : period.evidenceText().trim();
    }

    private static String describeReporter(SenderSignal sender) {
        return Validation.isBlank(sender.reporterId()) ? "the sender" : "reporter " + sender.reporterId();
    }

    private static LocalDate firstNonNull(LocalDate a, LocalDate b) {
        return a != null ? a : b;
    }

    /** The Sender Match signal, as Decide Filing needs it. */
    public record SenderSignal(String matchType, String reporterId, String companyId, int candidateCount,
                               boolean isReportingMailbox) {
    }

    /** The Detect Stated Period signal. */
    public record PeriodSignal(boolean periodFound, LocalDate weekStart, LocalDate weekEnd,
                               String evidenceText, String confidence) {
    }

    /** The Is Short Reply signal. */
    public record ShortReplySignal(boolean isShortReply, boolean isReplyToOurMail, String reason) {
    }

    /** The model's own reading of the message, supplied by the caller. */
    public record ModelReadingSignal(String kind, LocalDate week, String confidence) {
    }

    /** The decision. */
    public record FilingDecisionResult(String decision, String reporterId, String companyId,
                                       LocalDate weekStart, String reason) {
    }
}
