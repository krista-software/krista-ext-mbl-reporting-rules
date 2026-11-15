package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Week arithmetic in both week-start modes, including across year boundaries. */
class WeekCalculatorTest {

    @Test
    void mondayModePutsAWednesdayInThatMondaysWeek() {
        WeekCalculator.WeekWindowResult week =
                WeekCalculator.weekOf(LocalDate.of(2025, 11, 5), 0, DayOfWeek.MONDAY);
        assertEquals(LocalDate.of(2025, 11, 3), week.weekStart());
        assertEquals(LocalDate.of(2025, 11, 9), week.weekEnd());
        assertEquals("Week of Nov 3, 2025", week.weekLabel());
    }

    @Test
    void sundayModePutsTheSameWednesdayInThePrecedingSundaysWeek() {
        WeekCalculator.WeekWindowResult week =
                WeekCalculator.weekOf(LocalDate.of(2025, 11, 5), 0, DayOfWeek.SUNDAY);
        assertEquals(LocalDate.of(2025, 11, 2), week.weekStart());
        assertEquals(LocalDate.of(2025, 11, 8), week.weekEnd());
        assertEquals("Week of Nov 2, 2025", week.weekLabel());
    }

    @Test
    void aDateThatIsAlreadyTheWeekStartIsUnchanged() {
        WeekCalculator.WeekWindowResult week =
                WeekCalculator.weekOf(LocalDate.of(2025, 11, 3), 0, DayOfWeek.MONDAY);
        assertEquals(LocalDate.of(2025, 11, 3), week.weekStart());
    }

    @Test
    void offsetShiftsTheWindowBySevenDaysEachWay() {
        assertEquals(LocalDate.of(2025, 10, 27),
                WeekCalculator.weekOf(LocalDate.of(2025, 11, 5), -1, DayOfWeek.MONDAY).weekStart());
        assertEquals(LocalDate.of(2025, 11, 10),
                WeekCalculator.weekOf(LocalDate.of(2025, 11, 5), 1, DayOfWeek.MONDAY).weekStart());
    }

    @Test
    void mondayModeCrossesTheYearBoundary() {
        WeekCalculator.WeekWindowResult week =
                WeekCalculator.weekOf(LocalDate.of(2025, 12, 31), 0, DayOfWeek.MONDAY);
        assertEquals(LocalDate.of(2025, 12, 29), week.weekStart());
        assertEquals(LocalDate.of(2026, 1, 4), week.weekEnd());
        assertEquals("Week of Dec 29, 2025", week.weekLabel());
    }

    @Test
    void sundayModeCrossesTheYearBoundary() {
        WeekCalculator.WeekWindowResult week =
                WeekCalculator.weekOf(LocalDate.of(2026, 1, 1), 0, DayOfWeek.SUNDAY);
        assertEquals(LocalDate.of(2025, 12, 28), week.weekStart());
        assertEquals(LocalDate.of(2026, 1, 3), week.weekEnd());
        assertEquals("Week of Dec 28, 2025", week.weekLabel());
    }

    @Test
    void offsetCrossingTheYearBoundaryStaysCorrect() {
        WeekCalculator.WeekWindowResult week =
                WeekCalculator.weekOf(LocalDate.of(2026, 1, 5), -1, DayOfWeek.MONDAY);
        assertEquals(LocalDate.of(2025, 12, 29), week.weekStart());
        assertEquals(LocalDate.of(2026, 1, 4), week.weekEnd());
    }

    @Test
    void labelHasNoLeadingZeroOnTheDay() {
        assertEquals("Week of Jan 5, 2026",
                WeekCalculator.weekOf(LocalDate.of(2026, 1, 5), 0, DayOfWeek.MONDAY).weekLabel());
    }

    @Test
    void sameWeekComparesNormalisedWeekStarts() {
        assertTrue(WeekCalculator.sameWeek(LocalDate.of(2025, 11, 3), LocalDate.of(2025, 11, 7),
                DayOfWeek.MONDAY));
        assertFalse(WeekCalculator.sameWeek(LocalDate.of(2025, 11, 3), LocalDate.of(2025, 11, 10),
                DayOfWeek.MONDAY));
        assertFalse(WeekCalculator.sameWeek(null, LocalDate.of(2025, 11, 3), DayOfWeek.MONDAY));
    }

    @Test
    void aMissingDateIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> WeekCalculator.weekOf(null, 0, DayOfWeek.MONDAY));
    }
}
