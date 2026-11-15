package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules;

import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities.ReportStatus;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.ReportStatusCalculator.FiledReport;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.transformers.ReportStatusTransformer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Received / Late / Missing / Excused / Pending, measured against the configured due moment. */
class ReportStatusCalculatorTest {

    /** Reports are due Friday 17:00 New York; weeks start Monday. */
    private static final RulesConfig CONFIG =
            RulesConfig.parse("reports@mbl.example", "Monday", "America/New_York", "Friday 17:00");

    private static final LocalDate WEEK = LocalDate.of(2025, 11, 3);

    private static ZonedDateTime at(String iso) {
        return Dates.parseTimestamp(iso, "test", CONFIG.getZone());
    }

    private static FiledReport filed(String reporterId, LocalDate week, String receivedAt, boolean excused) {
        return new FiledReport(reporterId, week, receivedAt == null ? null : at(receivedAt), excused);
    }

    private static ReportStatusCalculator.ReportStatusResult status(List<FiledReport> reports, String asOf) {
        return ReportStatusCalculator.compute("r-1", WEEK, reports, at(asOf), CONFIG);
    }

    @Test
    void anExcusedWeekIsExcusedBeforeAnythingElse() {
        var result = status(List.of(filed("r-1", WEEK, "2025-11-10T09:00:00-05:00", true)),
                "2025-11-12T09:00:00-05:00");
        assertEquals(ReportStatusCalculator.EXCUSED, result.status());
    }

    @Test
    void filedBeforeTheDueMomentIsReceived() {
        var result = status(List.of(filed("r-1", WEEK, "2025-11-07T16:59:00-05:00", false)),
                "2025-11-08T09:00:00-05:00");
        assertEquals(ReportStatusCalculator.RECEIVED, result.status());
        assertEquals(at("2025-11-07T16:59:00-05:00"), result.receivedAt());
    }

    @Test
    void filedExactlyAtTheDueMomentIsReceived() {
        var result = status(List.of(filed("r-1", WEEK, "2025-11-07T17:00:00-05:00", false)),
                "2025-11-08T09:00:00-05:00");
        assertEquals(ReportStatusCalculator.RECEIVED, result.status());
    }

    @Test
    void filedOneMinuteAfterTheDueMomentIsLate() {
        var result = status(List.of(filed("r-1", WEEK, "2025-11-07T17:01:00-05:00", false)),
                "2025-11-08T09:00:00-05:00");
        assertEquals(ReportStatusCalculator.LATE, result.status());
        assertTrue(result.message().contains("after"));
    }

    @Test
    void nothingFiledAndPastDueIsMissing() {
        var result = status(List.of(), "2025-11-08T09:00:00-05:00");
        assertEquals(ReportStatusCalculator.MISSING, result.status());
        assertNull(result.receivedAt());
    }

    @Test
    void nothingFiledAndNotYetDueIsPending() {
        var result = status(List.of(), "2025-11-05T09:00:00-05:00");
        assertEquals(ReportStatusCalculator.PENDING, result.status());
        assertNull(result.receivedAt());
    }

    @Test
    void anEntryWithNoReceivedAtIsMissingOncePastDue() {
        var result = status(List.of(filed("r-1", WEEK, null, false)), "2025-11-08T09:00:00-05:00");
        assertEquals(ReportStatusCalculator.MISSING, result.status());
    }

    @Test
    void entriesForOtherReportersAndOtherWeeksAreIgnored() {
        var result = status(List.of(
                filed("r-2", WEEK, "2025-11-04T09:00:00-05:00", false),
                filed("r-1", WEEK.minusWeeks(1), "2025-10-29T09:00:00-04:00", false)),
                "2025-11-08T09:00:00-05:00");
        assertEquals(ReportStatusCalculator.MISSING, result.status());
    }

    @Test
    void theDueMomentUsesTheConfiguredZoneNotTheJvmDefault() {
        // 2025-11-07T22:30Z is 17:30 in New York — past the 17:00 due moment, even though it is
        // still before midnight UTC.
        var result = status(List.of(filed("r-1", WEEK, "2025-11-07T22:30:00Z", false)),
                "2025-11-08T09:00:00-05:00");
        assertEquals(ReportStatusCalculator.LATE, result.status());
        assertEquals("America/New_York", result.dueMoment().getZone().getId());
    }

    @Test
    void aSundayStartingWeekFindsItsOwnDueMoment() {
        RulesConfig sundayConfig = RulesConfig.parse("", "Sunday", "UTC", "Friday 17:00");
        var result = ReportStatusCalculator.compute("r-1", LocalDate.of(2025, 11, 2),
                List.of(filed("r-1", LocalDate.of(2025, 11, 2), "2025-11-07T16:00:00Z", false)),
                at("2025-11-10T09:00:00Z"), sundayConfig);
        assertEquals(ReportStatusCalculator.RECEIVED, result.status());
    }

    @Test
    void aBlankReporterIdIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ReportStatusCalculator.compute("  ", WEEK, List.of(), at("2025-11-08T09:00:00-05:00"),
                        CONFIG));
        assertTrue(e.getMessage().contains("Reporter Id"));
    }

    @Test
    void aMissingWeekStartIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ReportStatusCalculator.compute("r-1", null, List.of(),
                        at("2025-11-08T09:00:00-05:00"), CONFIG));
    }

    @Test
    void filedReportsAreParsedFromTheCompositeInput() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("Reporter Id", "r-1");
        row.put("Week Start", "2025-11-03");
        row.put("Received At", "2025-11-07T16:00:00-05:00");
        row.put("Excused", "false");
        List<FiledReport> reports = FiledReport.fromInput(List.of(row), CONFIG);
        assertEquals(1, reports.size());
        assertEquals(LocalDate.of(2025, 11, 3), reports.get(0).weekStart());
        assertEquals(ReportStatusCalculator.RECEIVED,
                status(reports, "2025-11-08T09:00:00-05:00").status());
    }

    @Test
    void aFiledReportsRowMissingItsKeysIsRejectedWithTheRowNumber() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("Reporter Id", "r-1");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> FiledReport.fromInput(List.of(row), CONFIG));
        assertTrue(e.getMessage().contains("entry 1"));
    }

    @Test
    void aNullFiledReportsListIsTreatedAsNothingOnFile() {
        var result = ReportStatusCalculator.compute("r-1", WEEK, null, at("2025-11-08T09:00:00-05:00"),
                CONFIG);
        assertEquals(ReportStatusCalculator.MISSING, result.status());
    }

    @Test
    void transformerEchoesTheReporterAndWeekAndFormatsTimestamps() {
        var result = status(List.of(filed("r-1", WEEK, "2025-11-07T16:00:00-05:00", false)),
                "2025-11-08T09:00:00-05:00");
        ReportStatus entity = ReportStatusTransformer.transform(result, "r-1", WEEK);
        assertEquals("r-1", entity.reporterId);
        assertEquals("2025-11-03", entity.weekStart);
        assertEquals("Received", entity.status);
        assertEquals("2025-11-07T16:00-05:00", entity.receivedAt);
        assertEquals("2025-11-07T17:00-05:00", entity.dueMoment);
        assertEquals(5, entity.toFields().size());
        assertNull(ReportStatusTransformer.transform(null, "r-1", WEEK));
    }
}
