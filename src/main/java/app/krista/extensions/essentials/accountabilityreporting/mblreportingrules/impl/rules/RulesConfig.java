package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The validated, parsed form of the four connection attributes.
 *
 * <p>Framework-free and immutable, so every rule class can take it as a plain argument and every rule
 * is unit-testable without the Krista runtime. Construction is strict: {@link #parse} throws
 * {@link IllegalArgumentException} with an actionable message for anything it cannot interpret, which
 * is what both {@code VALIDATE_ATTRIBUTES} (reject before save) and the catalog requests (report a
 * failure message instead of misbehaving silently) rely on.
 */
public final class RulesConfig {

    /** Accepted shape of the Due Day and Time attribute, e.g. {@code Friday 17:00}. */
    private static final Pattern DUE_PATTERN = Pattern.compile(
            "^\\s*([A-Za-z]+)\\s*[ @,]\\s*([01]?\\d|2[0-3]):([0-5]\\d)\\s*$");

    public static final String DUE_FORMAT_HELP =
            "Expected '<Day> HH:mm' in 24-hour time, for example 'Friday 17:00'. "
                    + "The day may be the full English name or its three-letter abbreviation (Mon-Sun).";

    private final String reportingMailboxAddress;
    private final DayOfWeek weekStart;
    private final ZoneId zone;
    private final DayOfWeek dueDay;
    private final LocalTime dueTime;

    private RulesConfig(String reportingMailboxAddress, DayOfWeek weekStart, ZoneId zone,
                        DayOfWeek dueDay, LocalTime dueTime) {
        this.reportingMailboxAddress = reportingMailboxAddress;
        this.weekStart = weekStart;
        this.zone = zone;
        this.dueDay = dueDay;
        this.dueTime = dueTime;
    }

    /**
     * Parse and validate the four raw attribute values.
     *
     * @throws IllegalArgumentException naming the field and why, for any invalid value
     */
    public static RulesConfig parse(String reportingMailboxAddress, String weekStartsOn,
                                    String timeZone, String dueDayAndTime) {
        ZoneId parsedZone = parseZone(timeZone);
        DayOfWeek parsedWeekStart = parseWeekStart(weekStartsOn);
        DueMoment due = parseDueDayAndTime(dueDayAndTime);
        String mailbox = reportingMailboxAddress == null ? "" : reportingMailboxAddress.trim();
        return new RulesConfig(mailbox, parsedWeekStart, parsedZone, due.day(), due.time());
    }

    /** Convenience overload for a raw attribute map (used by VALIDATE_ATTRIBUTES). */
    public static RulesConfig parse(Map<String, Object> attributes, String mailboxKey, String weekStartKey,
                                    String zoneKey, String dueKey) {
        return parse(str(attributes, mailboxKey), str(attributes, weekStartKey),
                str(attributes, zoneKey), str(attributes, dueKey));
    }

    private static String str(Map<String, Object> attributes, String key) {
        Object v = attributes == null ? null : attributes.get(key);
        return v == null ? "" : v.toString();
    }

    public static ZoneId parseZone(String timeZone) {
        if (isBlank(timeZone)) {
            throw new IllegalArgumentException(
                    "Time Zone is required. Provide an IANA time-zone id, for example 'America/New_York'.");
        }
        String candidate = timeZone.trim();
        if (!ZoneId.getAvailableZoneIds().contains(candidate)) {
            throw new IllegalArgumentException("Time Zone '" + candidate
                    + "' is not a recognized IANA time-zone id. Use a value such as 'America/New_York'.");
        }
        return ZoneId.of(candidate);
    }

    public static DayOfWeek parseWeekStart(String weekStartsOn) {
        if (isBlank(weekStartsOn)) {
            throw new IllegalArgumentException("Week Starts On is required. Choose either 'Monday' or 'Sunday'.");
        }
        String candidate = weekStartsOn.trim();
        if (candidate.equalsIgnoreCase("Monday")) {
            return DayOfWeek.MONDAY;
        }
        if (candidate.equalsIgnoreCase("Sunday")) {
            return DayOfWeek.SUNDAY;
        }
        throw new IllegalArgumentException("Week Starts On must be either 'Monday' or 'Sunday', but was '"
                + candidate + "'.");
    }

    /** The parsed Due Day and Time attribute. */
    public record DueMoment(DayOfWeek day, LocalTime time) {
    }

    public static DueMoment parseDueDayAndTime(String dueDayAndTime) {
        if (isBlank(dueDayAndTime)) {
            throw new IllegalArgumentException("Due Day and Time is required. " + DUE_FORMAT_HELP);
        }
        Matcher m = DUE_PATTERN.matcher(dueDayAndTime);
        if (!m.matches()) {
            throw new IllegalArgumentException("Due Day and Time '" + dueDayAndTime.trim()
                    + "' could not be understood. " + DUE_FORMAT_HELP);
        }
        DayOfWeek day = parseDayName(m.group(1));
        if (day == null) {
            throw new IllegalArgumentException("Due Day and Time '" + dueDayAndTime.trim()
                    + "' does not start with a recognized day name. " + DUE_FORMAT_HELP);
        }
        int hour = Integer.parseInt(m.group(2));
        int minute = Integer.parseInt(m.group(3));
        return new DueMoment(day, LocalTime.of(hour, minute));
    }

    private static DayOfWeek parseDayName(String raw) {
        String name = raw.trim().toUpperCase(Locale.ENGLISH);
        for (DayOfWeek d : DayOfWeek.values()) {
            String full = d.name();
            if (full.equals(name) || full.startsWith(name) && name.length() == 3) {
                return d;
            }
        }
        return null;
    }

    /**
     * The exact instant a report for the week beginning {@code weekStart} is due, in the configured
     * zone. The due day is located within that week, counting forward from the week's first day.
     */
    public ZonedDateTime dueMomentFor(LocalDate weekStart) {
        int offset = Math.floorMod(dueDay.getValue() - weekStart.getDayOfWeek().getValue(), 7);
        return weekStart.plusDays(offset).atTime(dueTime).atZone(zone);
    }

    public boolean isReportingMailbox(String emailAddress) {
        if (isBlank(reportingMailboxAddress) || isBlank(emailAddress)) {
            return false;
        }
        return reportingMailboxAddress.trim().equalsIgnoreCase(emailAddress.trim());
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public String getReportingMailboxAddress() {
        return reportingMailboxAddress;
    }

    public DayOfWeek getWeekStart() {
        return weekStart;
    }

    public ZoneId getZone() {
        return zone;
    }

    public DayOfWeek getDueDay() {
        return dueDay;
    }

    public LocalTime getDueTime() {
        return dueTime;
    }

    /** Human-readable summary used by Test Connection / Health Check. Contains no secrets. */
    public String describe() {
        return "week starts on " + title(weekStart) + ", time zone " + zone.getId()
                + ", reports due " + title(dueDay) + " " + dueTime
                + (isBlank(reportingMailboxAddress) ? ", no reporting mailbox address set"
                        : ", reporting mailbox " + reportingMailboxAddress);
    }

    private static String title(DayOfWeek d) {
        String n = d.name();
        return n.charAt(0) + n.substring(1).toLowerCase(Locale.ENGLISH);
    }
}
