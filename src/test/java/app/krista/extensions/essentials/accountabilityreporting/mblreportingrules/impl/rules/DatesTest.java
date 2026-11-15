package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ISO-8601 parsing and formatting at the request boundary. */
class DatesTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    @Test
    void parsesACalendarDate() {
        assertEquals(LocalDate.of(2025, 11, 3), Dates.parseDate("2025-11-03", "Date", NEW_YORK));
        assertEquals(LocalDate.of(2025, 11, 3), Dates.parseDate("  2025-11-03  ", "Date", NEW_YORK));
    }

    @Test
    void aTimestampIsAcceptedWhereADateIsExpectedAndConvertedInTheConfiguredZone() {
        // 2025-11-04T02:00Z is still 2025-11-03 in New York.
        assertEquals(LocalDate.of(2025, 11, 3), Dates.parseDate("2025-11-04T02:00:00Z", "Date", NEW_YORK));
    }

    @Test
    void parsesEveryCommonTimestampForm() {
        assertEquals(Dates.parseTimestamp("2025-11-07T21:00:00Z", "At", NEW_YORK).toInstant(),
                Dates.parseTimestamp("2025-11-07T16:00:00-05:00", "At", NEW_YORK).toInstant());
        assertEquals(16, Dates.parseTimestamp("2025-11-07T16:00:00", "At", NEW_YORK).getHour());
        assertEquals(0, Dates.parseTimestamp("2025-11-07", "At", NEW_YORK).getHour());
    }

    @Test
    void rejectsAnUnparseableDateNamingTheField() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseDate("not-a-date", "Week Start", NEW_YORK));
        assertTrue(e.getMessage().contains("Week Start"));
        assertTrue(e.getMessage().contains("2025-11-03"), "message should show the expected shape");
    }

    @Test
    void rejectsABlankRequiredDate() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseDate("", "Date", NEW_YORK));
    }

    @Test
    void optionalTimestampTreatsBlankAsAbsent() {
        assertNull(Dates.parseOptionalTimestamp("  ", "As Of", NEW_YORK));
        assertNull(Dates.parseOptionalTimestamp(null, "As Of", NEW_YORK));
    }

    @Test
    void formatsDatesAndTimestampsAsIso() {
        assertEquals("2025-11-03", Dates.formatDate(LocalDate.of(2025, 11, 3)));
        assertNull(Dates.formatDate(null));
        assertNull(Dates.formatTimestamp(null));
        assertEquals("2025-11-07T16:00-05:00",
                Dates.formatTimestamp(Dates.parseTimestamp("2025-11-07T16:00:00-05:00", "At", NEW_YORK)));
    }

    @Test
    void weekLabelHasNoLeadingZero() {
        assertEquals("Week of Nov 3, 2025", Dates.weekLabel(LocalDate.of(2025, 11, 3)));
        assertEquals("Week of Dec 29, 2025", Dates.weekLabel(LocalDate.of(2025, 12, 29)));
        assertNull(Dates.weekLabel(null));
    }

    @Test
    void impossibleDatesBecomeNullRatherThanThrowing() {
        assertNull(Dates.dateOrNull(2025, 2, 30));
        assertNull(Dates.dateOrNull(2025, 13, 1));
        assertEquals(LocalDate.of(2024, 2, 29), Dates.dateOrNull(2024, 2, 29));
    }
}
