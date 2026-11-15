package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the reporting period a message states for itself.
 *
 * <p>Confidence follows the agreed scheme:
 * <ul>
 *   <li><strong>High</strong> — an explicit calendar date or date range: {@code week of Nov 3},
 *       {@code 11/3-11/7}, {@code 2025-11-03 to 2025-11-07}, {@code week ending Nov 7}.</li>
 *   <li><strong>Low</strong> — a relative phrase ({@code last week}, {@code this week}) resolved
 *       against Received At, or a week number ({@code W45}, {@code week 45}).</li>
 * </ul>
 *
 * <p>Precedence: any High match beats any Low match; at equal confidence the subject beats the body.
 * Whatever is found is normalised onto the configured week start, so the returned window is always a
 * real reporting week rather than the literal dates typed by the sender.
 *
 * <p>The subject and body are treated strictly as <strong>data</strong>: they are scanned with fixed
 * regular expressions for dates only. Nothing in the text is ever interpreted as an instruction.
 */
public final class StatedPeriodDetector {

    public static final String HIGH = "High";
    public static final String LOW = "Low";

    private static final int EVIDENCE_MAX = 200;

    /** One date, in any of the forms people actually type. Non-capturing so it can nest in ranges. */
    private static final String DATE = "(?:\\d{4}-\\d{2}-\\d{2}"
            + "|[A-Za-z]{3,9}\\.?\\s+\\d{1,2}(?:st|nd|rd|th)?(?:,?\\s+\\d{4})?"
            + "|\\d{1,2}(?:st|nd|rd|th)?\\s+[A-Za-z]{3,9}\\.?(?:,?\\s+\\d{4})?"
            + "|\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?)";

    private static final Pattern RANGE = Pattern.compile(
            "(" + DATE + ")\\s*(?:to|through|thru|until|-|–|—)\\s*(" + DATE + ")", Pattern.CASE_INSENSITIVE);

    private static final Pattern WEEK_OF = Pattern.compile(
            "\\b(?:for\\s+the\\s+)?week\\s+(?:of|beginning|commencing|starting|start)\\s*:?\\s+(" + DATE + ")",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern WEEK_ENDING = Pattern.compile(
            "\\b(?:week\\s+end(?:ing|ed)?|w/e|w\\.e\\.)\\s*:?\\s+(" + DATE + ")", Pattern.CASE_INSENSITIVE);

    private static final Pattern LAST_WEEK = Pattern.compile(
            "\\b(?:last|previous|past|prior)\\s+week\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern THIS_WEEK = Pattern.compile(
            "\\b(?:this|current|the\\s+current)\\s+week\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern WEEK_NUMBER_LONG = Pattern.compile(
            "\\bwk\\s*#?\\s*(\\d{1,2})\\b(?:[,\\s]+(\\d{4}))?|\\bweek\\s*#?\\s*(\\d{1,2})\\b(?:[,\\s]+(\\d{4}))?",
            Pattern.CASE_INSENSITIVE);

    /** The {@code W45} form only, deliberately case-sensitive so ordinary words never match. */
    private static final Pattern WEEK_NUMBER_SHORT = Pattern.compile("\\bW(\\d{1,2})\\b(?:[,\\s]+(\\d{4}))?");

    private static final Pattern ISO_DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern MONTH_FIRST = Pattern.compile(
            "^([A-Za-z]{3,9})\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:,?\\s+(\\d{4}))?$");
    private static final Pattern DAY_FIRST = Pattern.compile(
            "^(\\d{1,2})(?:st|nd|rd|th)?\\s+([A-Za-z]{3,9})\\.?(?:,?\\s+(\\d{4}))?$");
    private static final Pattern NUMERIC = Pattern.compile("^(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?$");

    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("jan", 1), Map.entry("january", 1),
            Map.entry("feb", 2), Map.entry("february", 2),
            Map.entry("mar", 3), Map.entry("march", 3),
            Map.entry("apr", 4), Map.entry("april", 4),
            Map.entry("may", 5),
            Map.entry("jun", 6), Map.entry("june", 6),
            Map.entry("jul", 7), Map.entry("july", 7),
            Map.entry("aug", 8), Map.entry("august", 8),
            Map.entry("sep", 9), Map.entry("sept", 9), Map.entry("september", 9),
            Map.entry("oct", 10), Map.entry("october", 10),
            Map.entry("nov", 11), Map.entry("november", 11),
            Map.entry("dec", 12), Map.entry("december", 12));

    private StatedPeriodDetector() {
    }

    public static StatedPeriodResult detect(String subject, String bodyText, LocalDate receivedOn,
                                            DayOfWeek weekStart) {
        if (receivedOn == null) {
            throw new IllegalArgumentException("Received At is required.");
        }
        List<Source> sources = List.of(
                new Source("Subject", subject == null ? "" : subject),
                new Source("Body", bodyText == null ? "" : bodyText));

        for (Source source : sources) {
            StatedPeriodResult high = findExplicit(source, receivedOn, weekStart);
            if (high != null) {
                return high;
            }
        }
        for (Source source : sources) {
            StatedPeriodResult low = findRelative(source, receivedOn, weekStart);
            if (low != null) {
                return low;
            }
        }
        return new StatedPeriodResult(false, null, null, "", "",
                "No week or date range is stated in the subject or body.");
    }

    // ------------------------------------------------------------------ High confidence

    private static StatedPeriodResult findExplicit(Source source, LocalDate receivedOn, DayOfWeek weekStart) {
        Matcher range = RANGE.matcher(source.text());
        while (range.find()) {
            LocalDate from = parseDate(range.group(1), receivedOn);
            LocalDate to = parseDate(range.group(2), receivedOn);
            if (from != null && to != null && !to.isBefore(from)) {
                return build(from, weekStart, HIGH, source, range.group(),
                        "An explicit date range was stated.");
            }
        }
        Matcher weekOf = WEEK_OF.matcher(source.text());
        while (weekOf.find()) {
            LocalDate anchor = parseDate(weekOf.group(1), receivedOn);
            if (anchor != null) {
                return build(anchor, weekStart, HIGH, source, weekOf.group(),
                        "An explicit week start was stated.");
            }
        }
        Matcher weekEnding = WEEK_ENDING.matcher(source.text());
        while (weekEnding.find()) {
            LocalDate anchor = parseDate(weekEnding.group(1), receivedOn);
            if (anchor != null) {
                return build(anchor, weekStart, HIGH, source, weekEnding.group(),
                        "An explicit week end was stated.");
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ Low confidence

    private static StatedPeriodResult findRelative(Source source, LocalDate receivedOn, DayOfWeek weekStart) {
        Matcher last = LAST_WEEK.matcher(source.text());
        if (last.find()) {
            return build(receivedOn.minusWeeks(1), weekStart, LOW, source, last.group(),
                    "A relative period was resolved against Received At.");
        }
        Matcher current = THIS_WEEK.matcher(source.text());
        if (current.find()) {
            return build(receivedOn, weekStart, LOW, source, current.group(),
                    "A relative period was resolved against Received At.");
        }
        for (WeekNumber wn : weekNumbers(source.text())) {
            LocalDate anchor = fromIsoWeek(wn.week(), wn.year() == null ? receivedOn.getYear() : wn.year());
            if (anchor != null) {
                return build(anchor, weekStart, LOW, source, wn.evidence(),
                        "A week number was resolved to its ISO week.");
            }
        }
        return null;
    }

    private static List<WeekNumber> weekNumbers(String text) {
        List<WeekNumber> out = new ArrayList<>();
        Matcher shortForm = WEEK_NUMBER_SHORT.matcher(text);
        while (shortForm.find()) {
            out.add(new WeekNumber(Integer.parseInt(shortForm.group(1)), year(shortForm.group(2)),
                    shortForm.group()));
        }
        Matcher longForm = WEEK_NUMBER_LONG.matcher(text);
        while (longForm.find()) {
            String week = longForm.group(1) != null ? longForm.group(1) : longForm.group(3);
            String yr = longForm.group(1) != null ? longForm.group(2) : longForm.group(4);
            if (week != null) {
                out.add(new WeekNumber(Integer.parseInt(week), year(yr), longForm.group()));
            }
        }
        return out;
    }

    private static Integer year(String raw) {
        return raw == null ? null : Integer.valueOf(raw);
    }

    /** The Monday of ISO week {@code week} in {@code year}, or null when the week number is impossible. */
    private static LocalDate fromIsoWeek(int week, int year) {
        if (week < 1 || week > 53) {
            return null;
        }
        LocalDate fourthOfJanuary = LocalDate.of(year, 1, 4);
        LocalDate firstIsoMonday = fourthOfJanuary.with(WeekFields.ISO.dayOfWeek(), 1);
        LocalDate candidate = firstIsoMonday.plusWeeks(week - 1L);
        return candidate.get(WeekFields.ISO.weekOfWeekBasedYear()) == week ? candidate : null;
    }

    // ------------------------------------------------------------------ Date parsing

    /** Parse one date token. Returns null when the token is not a real date. */
    static LocalDate parseDate(String token, LocalDate receivedOn) {
        if (token == null) {
            return null;
        }
        String value = token.trim();
        if (ISO_DATE.matcher(value).matches()) {
            return Dates.dateOrNull(Integer.parseInt(value.substring(0, 4)),
                    Integer.parseInt(value.substring(5, 7)), Integer.parseInt(value.substring(8, 10)));
        }
        Matcher monthFirst = MONTH_FIRST.matcher(value);
        if (monthFirst.matches()) {
            Integer month = month(monthFirst.group(1));
            if (month == null) {
                return null;
            }
            return withYear(month, Integer.parseInt(monthFirst.group(2)), monthFirst.group(3), receivedOn);
        }
        Matcher dayFirst = DAY_FIRST.matcher(value);
        if (dayFirst.matches()) {
            Integer month = month(dayFirst.group(2));
            if (month == null) {
                return null;
            }
            return withYear(month, Integer.parseInt(dayFirst.group(1)), dayFirst.group(3), receivedOn);
        }
        Matcher numeric = NUMERIC.matcher(value);
        if (numeric.matches()) {
            String rawYear = numeric.group(3);
            String normalizedYear = rawYear == null ? null
                    : (rawYear.length() == 2 ? "20" + rawYear : rawYear);
            return withYear(Integer.parseInt(numeric.group(1)), Integer.parseInt(numeric.group(2)),
                    normalizedYear, receivedOn);
        }
        return null;
    }

    private static Integer month(String name) {
        return MONTHS.get(name.trim().toLowerCase(Locale.ENGLISH));
    }

    /**
     * Build the date, inferring the year from Received At when the sender omitted it. A date that
     * would land far in the future is pulled back a year, and one far in the past pushed forward —
     * this is what makes "week of Dec 29" in a January email resolve to the previous December.
     */
    private static LocalDate withYear(int month, int day, String explicitYear, LocalDate receivedOn) {
        if (explicitYear != null) {
            return Dates.dateOrNull(Integer.parseInt(explicitYear), month, day);
        }
        LocalDate candidate = Dates.dateOrNull(receivedOn.getYear(), month, day);
        if (candidate == null) {
            return null;
        }
        if (candidate.isAfter(receivedOn.plusDays(180))) {
            return Dates.dateOrNull(receivedOn.getYear() - 1, month, day);
        }
        if (candidate.isBefore(receivedOn.minusDays(185))) {
            return Dates.dateOrNull(receivedOn.getYear() + 1, month, day);
        }
        return candidate;
    }

    // ------------------------------------------------------------------ Assembly

    private static StatedPeriodResult build(LocalDate anchor, DayOfWeek weekStart, String confidence,
                                            Source source, String evidence, String why) {
        WeekCalculator.WeekWindowResult window = WeekCalculator.weekOf(anchor, 0, weekStart);
        return new StatedPeriodResult(true, window.weekStart(), window.weekEnd(),
                source.label() + ": " + truncate(evidence), confidence, why);
    }

    private static String truncate(String evidence) {
        String clean = evidence.replaceAll("\\s+", " ").trim();
        return clean.length() <= EVIDENCE_MAX ? clean : clean.substring(0, EVIDENCE_MAX) + "…";
    }

    private record Source(String label, String text) {
    }

    private record WeekNumber(int week, Integer year, String evidence) {
    }

    /** The detected period. {@code weekStart}/{@code weekEnd} are null when nothing was found. */
    public record StatedPeriodResult(boolean periodFound, LocalDate weekStart, LocalDate weekEnd,
                                     String evidenceText, String confidence, String message) {
    }
}
