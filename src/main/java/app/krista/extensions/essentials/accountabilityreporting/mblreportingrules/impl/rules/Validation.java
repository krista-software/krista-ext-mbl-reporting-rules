package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules;

/**
 * Fail-fast input validation. Every failure throws {@link IllegalArgumentException} naming the FIELD
 * and WHY — the Areas' audited wrapper turns that into a {@code Success = false} reply carrying the
 * same message, so a caller always learns what to fix.
 */
public final class Validation {

    private Validation() {
    }

    public static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    public static String requireNonBlank(String value, String field) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(field + " is required. Please provide a value for " + field + ".");
        }
        return value.trim();
    }

    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    /** Null-safe, case-insensitive comparison against a fixed value. */
    public static boolean equalsIgnoreCase(String value, String expected) {
        return value != null && value.trim().equalsIgnoreCase(expected);
    }
}
