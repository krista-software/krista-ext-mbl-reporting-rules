package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * ISO-8601 parsing and formatting helpers.
 *
 * <p>Every date and timestamp crosses the catalog-request boundary as ISO-8601 text — dates as
 * {@code YYYY-MM-DD}, timestamps in full ISO-8601 — so a calendar date is never silently reinterpreted
 * as an instant (which would shift week boundaries by a day near midnight). Parsing is strict and
 * reports the field name on failure; formatting is the single place output strings are produced.
 */
public final class Dates {

    /** "Week of Nov 3, 2025" — English month abbreviation, no leading zero on the day. */
    private static final DateTimeFormatter WEEK_LABEL_DAY =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private Dates() {
    }

    /** Parse a {@code YYYY-MM-DD} calendar date. A full timestamp is also accepted and truncated. */
    public static LocalDate parseDate(String value, String fieldName, ZoneId zone) {
        String raw = requireText(value, fieldName);
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException ignored) {
            // fall through to the timestamp forms
        }
        ZonedDateTime instant = tryParseTimestamp(raw, zone);
        if (instant != null) {
            return instant.toLocalDate();
        }
        throw new IllegalArgumentException(fieldName + " must be an ISO-8601 date such as 2025-11-03, but was '"
                + raw + "'.");
    }

    /** Parse a full ISO-8601 timestamp; a bare date is accepted and taken as midnight in {@code zone}. */
    public static ZonedDateTime parseTimestamp(String value, String fieldName, ZoneId zone) {
        String raw = requireText(value, fieldName);
        ZonedDateTime parsed = tryParseTimestamp(raw, zone);
        if (parsed != null) {
            return parsed;
        }
        throw new IllegalArgumentException(fieldName
                + " must be an ISO-8601 timestamp such as 2025-11-07T16:30:00Z, but was '" + raw + "'.");
    }

    /** Same as {@link #parseTimestamp} but returns {@code null} for blank input instead of throwing. */
    public static ZonedDateTime parseOptionalTimestamp(String value, String fieldName, ZoneId zone) {
        if (isBlank(value)) {
            return null;
        }
        return parseTimestamp(value, fieldName, zone);
    }

    private static ZonedDateTime tryParseTimestamp(String raw, ZoneId zone) {
        try {
            return OffsetDateTime.parse(raw).atZoneSameInstant(zone);
        } catch (DateTimeParseException ignored) {
            // not an offset timestamp
        }
        try {
            return ZonedDateTime.parse(raw).withZoneSameInstant(zone);
        } catch (DateTimeParseException ignored) {
            // not a zoned timestamp
        }
        try {
            return LocalDateTime.parse(raw).atZone(zone);
        } catch (DateTimeParseException ignored) {
            // not a local timestamp
        }
        try {
            return Instant.parse(raw).atZone(zone);
        } catch (DateTimeParseException ignored) {
            // not an instant
        }
        try {
            return LocalDate.parse(raw).atStartOfDay(zone);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    /** {@code YYYY-MM-DD}, or {@code null} for a null date. */
    public static String formatDate(LocalDate date) {
        return date == null ? null : date.toString();
    }

    /** Full ISO-8601 with offset, or {@code null}. */
    public static String formatTimestamp(ZonedDateTime timestamp) {
        return timestamp == null ? null : timestamp.toOffsetDateTime().toString();
    }

    /** "Week of Nov 3, 2025". */
    public static String weekLabel(LocalDate weekStart) {
        return weekStart == null ? null : "Week of " + WEEK_LABEL_DAY.format(weekStart);
    }

    /** Build a date defensively, returning {@code null} rather than throwing on an impossible date. */
    public static LocalDate dateOrNull(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            return null;
        }
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String requireText(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }
}
