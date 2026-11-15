package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Connection-attribute parsing and validation — what VALIDATE_ATTRIBUTES and Test Connection rely on. */
class RulesConfigTest {

    private static RulesConfig valid() {
        return RulesConfig.parse("reports@mbl.example", "Monday", "America/New_York", "Friday 17:00");
    }

    @Test
    void parsesAValidConfiguration() {
        RulesConfig config = valid();
        assertEquals(DayOfWeek.MONDAY, config.getWeekStart());
        assertEquals("America/New_York", config.getZone().getId());
        assertEquals(DayOfWeek.FRIDAY, config.getDueDay());
        assertEquals(LocalTime.of(17, 0), config.getDueTime());
    }

    @Test
    void rejectsAnUnknownTimeZone() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> RulesConfig.parse("", "Monday", "America/Nowhere", "Friday 17:00"));
        assertTrue(e.getMessage().contains("America/Nowhere"));
        assertTrue(e.getMessage().contains("IANA"));
    }

    @Test
    void rejectsABlankTimeZone() {
        assertThrows(IllegalArgumentException.class,
                () -> RulesConfig.parse("", "Monday", "  ", "Friday 17:00"));
    }

    @Test
    void rejectsAWeekStartThatIsNeitherMondayNorSunday() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> RulesConfig.parse("", "Wednesday", "UTC", "Friday 17:00"));
        assertTrue(e.getMessage().contains("Monday"));
        assertTrue(e.getMessage().contains("Sunday"));
    }

    @Test
    void acceptsEitherWeekStartInAnyCase() {
        assertEquals(DayOfWeek.SUNDAY, RulesConfig.parseWeekStart("sunday"));
        assertEquals(DayOfWeek.MONDAY, RulesConfig.parseWeekStart("MONDAY"));
    }

    @Test
    void acceptsFullAndAbbreviatedDueDayNames() {
        assertEquals(DayOfWeek.FRIDAY, RulesConfig.parseDueDayAndTime("Friday 17:00").day());
        assertEquals(DayOfWeek.FRIDAY, RulesConfig.parseDueDayAndTime("fri 09:30").day());
        assertEquals(LocalTime.of(9, 30), RulesConfig.parseDueDayAndTime("fri 09:30").time());
        assertEquals(DayOfWeek.SUNDAY, RulesConfig.parseDueDayAndTime("SUN 0:00").day());
    }

    @Test
    void rejectsADueDayAndTimeThatDoesNotParse() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> RulesConfig.parseDueDayAndTime("Friday 5pm"));
        assertTrue(e.getMessage().contains("Friday 17:00"), "message should show the expected format");

        assertThrows(IllegalArgumentException.class, () -> RulesConfig.parseDueDayAndTime("Funday 17:00"));
        assertThrows(IllegalArgumentException.class, () -> RulesConfig.parseDueDayAndTime("Friday 25:00"));
        assertThrows(IllegalArgumentException.class, () -> RulesConfig.parseDueDayAndTime(""));
    }

    @Test
    void computesTheDueMomentInsideTheRequestedWeekInTheConfiguredZone() {
        RulesConfig config = valid();
        // Week beginning Monday 2025-11-03; Friday of that week is 2025-11-07 at 17:00 New York time.
        ZonedDateTime due = config.dueMomentFor(LocalDate.of(2025, 11, 3));
        assertEquals(LocalDate.of(2025, 11, 7), due.toLocalDate());
        assertEquals(LocalTime.of(17, 0), due.toLocalTime());
        assertEquals("America/New_York", due.getZone().getId());
    }

    @Test
    void computesTheDueMomentForASundayStartingWeek() {
        RulesConfig config = RulesConfig.parse("", "Sunday", "UTC", "Friday 17:00");
        // Week beginning Sunday 2025-11-02; the Friday within it is 2025-11-07.
        assertEquals(LocalDate.of(2025, 11, 7), config.dueMomentFor(LocalDate.of(2025, 11, 2)).toLocalDate());
    }

    @Test
    void dueMomentCanFallOnTheWeeksFirstDay() {
        RulesConfig config = RulesConfig.parse("", "Monday", "UTC", "Monday 08:00");
        assertEquals(LocalDate.of(2025, 11, 3), config.dueMomentFor(LocalDate.of(2025, 11, 3)).toLocalDate());
    }

    @Test
    void recognisesTheReportingMailboxCaseInsensitively() {
        RulesConfig config = valid();
        assertTrue(config.isReportingMailbox("Reports@MBL.example"));
        assertFalse(config.isReportingMailbox("someone@mbl.example"));
        assertFalse(config.isReportingMailbox(""));
    }

    @Test
    void mailboxIsNeverMatchedWhenNoneIsConfigured() {
        RulesConfig config = RulesConfig.parse("", "Monday", "UTC", "Friday 17:00");
        assertFalse(config.isReportingMailbox("anything@mbl.example"));
    }

    @Test
    void parsesFromAnAttributeMap() {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("Reporting Mailbox Address", "reports@mbl.example");
        attrs.put("Week Starts On", "Sunday");
        attrs.put("Time Zone", "UTC");
        attrs.put("Due Day and Time", "Mon 09:00");
        RulesConfig config = RulesConfig.parse(attrs, "Reporting Mailbox Address", "Week Starts On",
                "Time Zone", "Due Day and Time");
        assertEquals(DayOfWeek.SUNDAY, config.getWeekStart());
        assertEquals(DayOfWeek.MONDAY, config.getDueDay());
    }

    @Test
    void describeMentionsTheSettingsAndNoSecrets() {
        String description = valid().describe();
        assertTrue(description.contains("Monday"));
        assertTrue(description.contains("America/New_York"));
        assertTrue(description.contains("reports@mbl.example"));
    }
}
