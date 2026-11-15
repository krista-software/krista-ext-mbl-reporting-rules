package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The tunable constants behind {@link ShortReplyDetector}, deliberately gathered in one place.
 *
 * <p>These thresholds are a provisional default agreed during the build; they are expected to be
 * reconciled with MBL's AI Studio {@code short-reply.ts}. Keeping them here means that reconciliation
 * is a constant change, not a rewrite of the detector.
 */
public final class ShortReplyRules {

    /** A body with fewer than this many words (after stripping) is a short reply. */
    public static final int WORD_THRESHOLD = 25;

    /** Bodies that are exactly one of these acknowledgements are short replies at any length. */
    public static final Set<String> ACKNOWLEDGEMENT_PHRASES = Set.of(
            "thanks", "thank you", "thanks!", "got it", "noted", "ok", "okay", "will do",
            "received", "sounds good");

    /** Lines at or after any of these markers are quoted history, not the sender's own words. */
    public static final List<Pattern> QUOTED_HISTORY_MARKERS = List.of(
            Pattern.compile("^\\s*>.*$"),
            Pattern.compile("^\\s*on\\s+.{0,300}\\bwrote\\s*:\\s*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*-{2,}\\s*original message\\s*-{2,}\\s*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*-{2,}\\s*forwarded message\\s*-{2,}\\s*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*from\\s*:\\s*.+$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*sent\\s*:\\s*.+$", Pattern.CASE_INSENSITIVE));

    /** Lines at or after any of these markers are a signature or a boilerplate disclaimer. */
    public static final List<Pattern> SIGNATURE_MARKERS = List.of(
            Pattern.compile("^\\s*--\\s*$"),
            Pattern.compile("^\\s*__+\\s*$"),
            Pattern.compile("^\\s*sent from my .*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*get outlook for .*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*confidentiality notice\\b.*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*this (?:e-?mail|message) and any attachments\\b.*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*the information contained in this (?:e-?mail|message)\\b.*$",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\s*disclaimer\\s*:.*$", Pattern.CASE_INSENSITIVE));

    private ShortReplyRules() {
    }
}
