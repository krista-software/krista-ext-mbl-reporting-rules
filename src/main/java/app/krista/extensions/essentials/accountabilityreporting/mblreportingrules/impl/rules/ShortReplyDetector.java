package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Decides whether a message is a throwaway acknowledgement, and whether it is a reply to mail we sent.
 *
 * <p>Both signals exist to keep noise out of the filing pipeline: a short reply is never a weekly
 * report, and a reply to our own outbound mail is our own thread coming back to us.
 *
 * <p>The body is treated strictly as data — it is stripped and counted, never interpreted.
 */
public final class ShortReplyDetector {

    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}'\\-]*");
    private static final Pattern MESSAGE_ID_SEPARATOR = Pattern.compile("[\\s,]+");
    private static final Pattern TRAILING_PUNCTUATION = Pattern.compile("[\\p{Punct}\\s]+$");

    private ShortReplyDetector() {
    }

    public static ShortReplyResult evaluate(String subject, String bodyText, String inReplyTo,
                                            String references, List<String> ourMessageIds) {
        String body = bodyText == null ? "" : bodyText;
        String ownWords = stripQuotedAndSignature(body);
        int wordCount = countWords(ownWords);
        String acknowledgement = acknowledgementOf(ownWords);

        boolean shortReply = acknowledgement != null || wordCount < ShortReplyRules.WORD_THRESHOLD;
        boolean replyToOurMail = referencesOurMail(inReplyTo, references, ourMessageIds);

        StringBuilder reason = new StringBuilder();
        if (acknowledgement != null) {
            reason.append("The message body is the acknowledgement '").append(acknowledgement)
                    .append("' once quoted text and signature are removed.");
        } else if (shortReply) {
            reason.append("The message body has ").append(wordCount)
                    .append(wordCount == 1 ? " word" : " words")
                    .append(" once quoted text and signature are removed, below the threshold of ")
                    .append(ShortReplyRules.WORD_THRESHOLD).append(".");
        } else {
            reason.append("The message body has ").append(wordCount)
                    .append(" words of its own, at or above the threshold of ")
                    .append(ShortReplyRules.WORD_THRESHOLD).append(".");
        }
        if (replyToOurMail) {
            reason.append(" It replies to a message we sent.");
        }
        return new ShortReplyResult(Validation.trimToEmpty(subject), shortReply, replyToOurMail,
                reason.toString(), wordCount);
    }

    /**
     * Remove quoted history, signatures and boilerplate disclaimers, leaving only what the sender
     * typed. Everything from the first marker line onwards is dropped, as is any line that is itself
     * quoted with a leading {@code >}.
     */
    public static String stripQuotedAndSignature(String body) {
        List<String> kept = new ArrayList<>();
        for (String line : body.split("\\R", -1)) {
            if (matchesAny(line, ShortReplyRules.SIGNATURE_MARKERS)) {
                break;
            }
            if (matchesAny(line, ShortReplyRules.QUOTED_HISTORY_MARKERS)) {
                // A quoted line that is only prefixed with '>' is dropped; a header-style marker
                // ("On ... wrote:", "-----Original Message-----", "From:") ends the sender's own text.
                if (line.trim().startsWith(">")) {
                    continue;
                }
                break;
            }
            kept.add(line);
        }
        return String.join("\n", kept).trim();
    }

    public static int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int count = 0;
        var matcher = WORD.matcher(text);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /** The acknowledgement phrase this body is, or null when it is not one. */
    static String acknowledgementOf(String ownWords) {
        String normalized = TRAILING_PUNCTUATION.matcher(ownWords.trim()).replaceAll("")
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return null;
        }
        for (String phrase : ShortReplyRules.ACKNOWLEDGEMENT_PHRASES) {
            String candidate = TRAILING_PUNCTUATION.matcher(phrase).replaceAll("");
            if (normalized.equals(candidate)) {
                return normalized;
            }
        }
        return null;
    }

    /** True when In Reply To or References names any of our own message ids. */
    static boolean referencesOurMail(String inReplyTo, String references, List<String> ourMessageIds) {
        Set<String> ours = normalizeIds(ourMessageIds);
        if (ours.isEmpty()) {
            return false;
        }
        Set<String> theirs = new LinkedHashSet<>();
        theirs.addAll(normalizeIds(splitIds(inReplyTo)));
        theirs.addAll(normalizeIds(splitIds(references)));
        for (String id : theirs) {
            if (ours.contains(id)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> splitIds(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : MESSAGE_ID_SEPARATOR.split(raw.trim())) {
            if (!part.isBlank()) {
                out.add(part);
            }
        }
        return out;
    }

    /** Strip angle brackets and case so {@code <abc@x>} and {@code ABC@X} compare equal. */
    private static Set<String> normalizeIds(List<String> ids) {
        Set<String> out = new LinkedHashSet<>();
        if (ids == null) {
            return out;
        }
        for (String id : ids) {
            if (id == null) {
                continue;
            }
            String value = id.trim();
            if (value.startsWith("<")) {
                value = value.substring(1);
            }
            if (value.endsWith(">")) {
                value = value.substring(0, value.length() - 1);
            }
            value = value.trim().toLowerCase(Locale.ROOT);
            if (!value.isEmpty()) {
                out.add(value);
            }
        }
        return out;
    }

    private static boolean matchesAny(String line, List<Pattern> patterns) {
        for (Pattern p : patterns) {
            if (p.matcher(line).matches()) {
                return true;
            }
        }
        return false;
    }

    /** The outcome of the short-reply assessment. */
    public record ShortReplyResult(String subject, boolean isShortReply, boolean isReplyToOurMail,
                                   String reason, int ownWordCount) {
    }
}
