package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Works out where a reporter stands for one week.
 *
 * <p>Evaluated in this order against the entry matching both Reporter Id and Week Start:
 * <ol>
 *   <li>Excused — an explicitly excused week, whatever else is recorded.</li>
 *   <li>Received — filed at or before that week's due moment.</li>
 *   <li>Late — filed after the due moment.</li>
 *   <li>Missing — nothing filed and the due moment has passed.</li>
 *   <li>Pending — nothing filed and the week is not due yet.</li>
 * </ol>
 *
 * <p>The due moment is derived from the Due Day and Time attribute in the configured time zone, for
 * the specific week being asked about — never from the JVM's default zone.
 */
public final class ReportStatusCalculator {

    public static final String RECEIVED = "Received";
    public static final String LATE = "Late";
    public static final String MISSING = "Missing";
    public static final String EXCUSED = "Excused";
    public static final String PENDING = "Pending";

    private ReportStatusCalculator() {
    }

    public static ReportStatusResult compute(String reporterId, LocalDate weekStart,
                                             List<FiledReport> filedReports, ZonedDateTime asOf,
                                             RulesConfig config) {
        String reporter = Validation.requireNonBlank(reporterId, "Reporter Id");
        if (weekStart == null) {
            throw new IllegalArgumentException("Week Start is required.");
        }
        ZonedDateTime dueMoment = config.dueMomentFor(weekStart);
        FiledReport entry = find(reporter, weekStart, filedReports, config);

        if (entry != null && entry.excused()) {
            return new ReportStatusResult(EXCUSED, entry.receivedAt(), dueMoment,
                    "Reporter " + reporter + " is excused for the week of " + Dates.formatDate(weekStart) + ".");
        }
        if (entry != null && entry.receivedAt() != null) {
            boolean onTime = !entry.receivedAt().isAfter(dueMoment);
            return new ReportStatusResult(onTime ? RECEIVED : LATE, entry.receivedAt(), dueMoment,
                    "Filed " + Dates.formatTimestamp(entry.receivedAt()) + ", which is "
                            + (onTime ? "at or before" : "after") + " the due moment "
                            + Dates.formatTimestamp(dueMoment) + ".");
        }
        if (asOf.isAfter(dueMoment)) {
            return new ReportStatusResult(MISSING, null, dueMoment,
                    "Nothing filed for the week of " + Dates.formatDate(weekStart)
                            + " and the due moment " + Dates.formatTimestamp(dueMoment) + " has passed.");
        }
        return new ReportStatusResult(PENDING, null, dueMoment,
                "Nothing filed yet for the week of " + Dates.formatDate(weekStart)
                        + "; it is not due until " + Dates.formatTimestamp(dueMoment) + ".");
    }

    /** The first entry for this reporter and week. Entries for other reporters/weeks are ignored. */
    private static FiledReport find(String reporterId, LocalDate weekStart, List<FiledReport> filedReports,
                                    RulesConfig config) {
        if (filedReports == null) {
            return null;
        }
        LocalDate normalizedWeek = WeekCalculator.startOfWeek(weekStart, config.getWeekStart());
        for (FiledReport report : filedReports) {
            if (report == null || !reporterId.equals(report.reporterId())) {
                continue;
            }
            LocalDate entryWeek = WeekCalculator.startOfWeek(report.weekStart(), config.getWeekStart());
            if (normalizedWeek.equals(entryWeek)) {
                return report;
            }
        }
        return null;
    }

    /** One already-filed report, as supplied by the caller. */
    public record FiledReport(String reporterId, LocalDate weekStart, ZonedDateTime receivedAt,
                              boolean excused) {

        public static final String F_REPORTER_ID = "Reporter Id";
        public static final String F_WEEK_START = "Week Start";
        public static final String F_RECEIVED_AT = "Received At";
        public static final String F_EXCUSED = "Excused";

        /** Build the list from the raw composite input; unusable rows are reported, never ignored. */
        public static List<FiledReport> fromInput(List<Map<String, Object>> rows, RulesConfig config) {
            List<FiledReport> out = new ArrayList<>();
            if (rows == null) {
                return out;
            }
            int index = 0;
            for (Map<String, Object> row : rows) {
                index++;
                if (row == null) {
                    continue;
                }
                String reporterId = Validation.trimToEmpty(str(row.get(F_REPORTER_ID)));
                String rawWeek = str(row.get(F_WEEK_START));
                if (reporterId.isEmpty() || Validation.isBlank(rawWeek)) {
                    throw new IllegalArgumentException("Filed Reports entry " + index
                            + " needs both a Reporter Id and a Week Start.");
                }
                LocalDate week = Dates.parseDate(rawWeek, "Filed Reports entry " + index + " Week Start",
                        config.getZone());
                ZonedDateTime receivedAt = Dates.parseOptionalTimestamp(str(row.get(F_RECEIVED_AT)),
                        "Filed Reports entry " + index + " Received At", config.getZone());
                out.add(new FiledReport(reporterId, week, receivedAt, bool(row.get(F_EXCUSED))));
            }
            return out;
        }

        private static String str(Object value) {
            return value == null ? "" : value.toString();
        }

        private static boolean bool(Object value) {
            if (value instanceof Boolean b) {
                return b;
            }
            String s = value == null ? "" : value.toString().trim();
            return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes") || s.equals("1");
        }
    }

    /** Where the reporter stands, plus the due moment the answer was measured against. */
    public record ReportStatusResult(String status, ZonedDateTime receivedAt, ZonedDateTime dueMoment,
                                     String message) {
    }
}
