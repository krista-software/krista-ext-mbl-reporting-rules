package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Week arithmetic honouring the configured week start (Monday or Sunday).
 *
 * <p>All week reasoning in the extension funnels through here so a week start is computed exactly one
 * way. Deliberately date-only: no instants, no zone conversions, so results cannot drift by a day.
 */
public final class WeekCalculator {

    private WeekCalculator() {
    }

    /** The window a date falls in, shifted by {@code offsetWeeks} whole weeks. */
    public static WeekWindowResult weekOf(LocalDate date, int offsetWeeks, DayOfWeek weekStart) {
        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }
        LocalDate start = date.with(TemporalAdjusters.previousOrSame(weekStart)).plusWeeks(offsetWeeks);
        LocalDate end = start.plusDays(6);
        return new WeekWindowResult(start, end, Dates.weekLabel(start));
    }

    /** The first day of the week a date falls in. */
    public static LocalDate startOfWeek(LocalDate date, DayOfWeek weekStart) {
        return date == null ? null : date.with(TemporalAdjusters.previousOrSame(weekStart));
    }

    /** True when both dates fall in the same configured week. Null on either side is never a match. */
    public static boolean sameWeek(LocalDate a, LocalDate b, DayOfWeek weekStart) {
        if (a == null || b == null) {
            return false;
        }
        return startOfWeek(a, weekStart).equals(startOfWeek(b, weekStart));
    }

    /** The computed week window. */
    public record WeekWindowResult(LocalDate weekStart, LocalDate weekEnd, String weekLabel) {
    }
}
