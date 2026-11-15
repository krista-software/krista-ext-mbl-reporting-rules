package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.support;

import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.Dates;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

/**
 * Null-safe readers for the composite (inline-object) catalog-request inputs.
 *
 * <p>Krista hands a composite input over as a {@code Map} keyed by the field display names, with
 * values already typed — but a workflow may legitimately leave any of them unset, so every read here
 * tolerates a missing key and a loosely-typed value rather than throwing a ClassCastException.
 */
public final class Inputs {

    private Inputs() {
    }

    public static Map<String, Object> require(Map<String, Object> value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required. Please provide a value for " + field + ".");
        }
        return value;
    }

    public static String str(Map<String, Object> source, String key) {
        Object value = source == null ? null : source.get(key);
        return value == null ? "" : value.toString().trim();
    }

    public static boolean bool(Map<String, Object> source, String key) {
        Object value = source == null ? null : source.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        String s = value == null ? "" : value.toString().trim();
        return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes") || s.equals("1");
    }

    public static int integer(Map<String, Object> source, String key) {
        Object value = source == null ? null : source.get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        String s = value == null ? "" : value.toString().trim();
        if (s.isEmpty()) {
            return 0;
        }
        try {
            return (int) Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** An optional ISO-8601 date inside a composite input; blank means "not stated". */
    public static LocalDate date(Map<String, Object> source, String key, String field, ZoneId zone) {
        String raw = str(source, key);
        return Dates.isBlank(raw) ? null : Dates.parseDate(raw, field, zone);
    }
}
