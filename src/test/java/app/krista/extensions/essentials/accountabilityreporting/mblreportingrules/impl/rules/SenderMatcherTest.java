package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules;

import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities.SenderMatch;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.transformers.SenderMatchTransformer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sender resolution: exact, alias, inactive, ambiguous, unknown, and the reporting mailbox. */
class SenderMatcherTest {

    private static final RulesConfig CONFIG =
            RulesConfig.parse("reports@mbl.example", "Monday", "America/New_York", "Friday 17:00");

    private static Map<String, Object> row(String id, String name, String emails, String aliases,
                                           String companyId, Object active) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("Id", id);
        row.put("Name", name);
        row.put("Emails", emails);
        row.put("Aliases", aliases);
        row.put("Company Id", companyId);
        row.put("Active", active);
        return row;
    }

    private static List<ReporterDirectoryEntry> directory(Map<String, Object>... rows) {
        List<Map<String, Object>> input = new ArrayList<>(List.of(rows));
        return ReporterDirectoryEntry.fromInput(input);
    }

    @Test
    void exactMatchIgnoresLetterCase() {
        var result = SenderMatcher.match("Jane.Doe@MBL.example", "Jane Doe",
                directory(row("r-1", "Jane Doe", "jane.doe@mbl.example", "", "c-1", true)), CONFIG);
        assertEquals(SenderMatcher.EXACT, result.matchType());
        assertEquals("r-1", result.reporterId());
        assertEquals("c-1", result.companyId());
        assertEquals("jane.doe@mbl.example", result.fromAddress());
    }

    @Test
    void headerFormAddressIsUnwrapped() {
        var result = SenderMatcher.match("Jane Doe <jane.doe@mbl.example>", null,
                directory(row("r-1", "Jane Doe", "jane.doe@mbl.example", "", "c-1", true)), CONFIG);
        assertEquals(SenderMatcher.EXACT, result.matchType());
    }

    @Test
    void aliasAddressMatchesAsAlias() {
        var result = SenderMatcher.match("jd@mbl.example", null,
                directory(row("r-1", "Jane Doe", "jane.doe@mbl.example", "jd@mbl.example; j.doe@mbl.example",
                        "c-1", true)), CONFIG);
        assertEquals(SenderMatcher.ALIAS, result.matchType());
        assertEquals("r-1", result.reporterId());
        assertEquals("Alias", result.candidates().get(0).matchedOn());
    }

    @Test
    void emailsAndAliasesSplitOnCommasAndSemicolons() {
        var entry = ReporterDirectoryEntry.fromInput(List.of(
                row("r-1", "Jane", " a@x.test , b@x.test ; c@x.test ", "", "c-1", true))).get(0);
        assertEquals(3, entry.getEmails().size());
        assertTrue(entry.hasEmail("b@x.test"));
    }

    @Test
    void aListValuedEmailsFieldIsAlsoAccepted() {
        Map<String, Object> row = row("r-1", "Jane", null, null, "c-1", true);
        row.put("Emails", List.of("a@x.test", "b@x.test"));
        var entry = ReporterDirectoryEntry.fromInput(List.of(row)).get(0);
        assertTrue(entry.hasEmail("a@x.test"));
        assertTrue(entry.hasEmail("b@x.test"));
    }

    @Test
    void inactiveReporterNeverMatches() {
        var result = SenderMatcher.match("jane.doe@mbl.example", null,
                directory(row("r-1", "Jane Doe", "jane.doe@mbl.example", "", "c-1", false)), CONFIG);
        assertEquals(SenderMatcher.NONE, result.matchType());
        assertEquals("", result.reporterId());
        assertEquals("Inactive", result.candidates().get(0).matchedOn());
        assertTrue(result.message().contains("inactive"));
    }

    @Test
    void twoActiveReportersOnTheSameAddressAreAmbiguous() {
        var result = SenderMatcher.match("shared@mbl.example", null,
                directory(row("r-1", "Jane Doe", "shared@mbl.example", "", "c-1", true),
                        row("r-2", "John Roe", "shared@mbl.example", "", "c-2", true)), CONFIG);
        assertEquals(SenderMatcher.AMBIGUOUS, result.matchType());
        assertEquals("", result.reporterId());
        assertEquals(2, result.candidateCount());
        assertTrue(result.message().contains("2 active reporters"));
    }

    @Test
    void anActiveAndAnInactiveOnTheSameAddressIsNotAmbiguous() {
        var result = SenderMatcher.match("shared@mbl.example", null,
                directory(row("r-1", "Jane Doe", "shared@mbl.example", "", "c-1", true),
                        row("r-2", "John Roe", "shared@mbl.example", "", "c-2", false)), CONFIG);
        assertEquals(SenderMatcher.EXACT, result.matchType());
        assertEquals("r-1", result.reporterId());
    }

    @Test
    void unknownAddressIsNoneWithNoCandidates() {
        var result = SenderMatcher.match("stranger@elsewhere.test", "A Stranger",
                directory(row("r-1", "Jane Doe", "jane.doe@mbl.example", "", "c-1", true)), CONFIG);
        assertEquals(SenderMatcher.NONE, result.matchType());
        assertEquals(0, result.candidateCount());
        assertFalse(result.isReportingMailbox());
    }

    @Test
    void theReportingMailboxItselfIsFlaggedAndNeverMatched() {
        var result = SenderMatcher.match("Reports@MBL.example", null,
                directory(row("r-1", "Shared", "reports@mbl.example", "", "c-1", true)), CONFIG);
        assertTrue(result.isReportingMailbox());
        assertEquals(SenderMatcher.NONE, result.matchType());
        assertEquals("", result.reporterId());
    }

    @Test
    void displayNameIsNeverUsedToMatch() {
        var result = SenderMatcher.match("unknown@elsewhere.test", "Jane Doe",
                directory(row("r-1", "Jane Doe", "jane.doe@mbl.example", "", "c-1", true)), CONFIG);
        assertEquals(SenderMatcher.NONE, result.matchType());
    }

    @Test
    void blankFromAddressIsRejectedWithAnActionableMessage() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> SenderMatcher.match("  ", null, directory(), CONFIG));
        assertTrue(e.getMessage().contains("From Address"));
    }

    @Test
    void anEmptyDirectoryStillAnswers() {
        var result = SenderMatcher.match("jane.doe@mbl.example", null, List.of(), CONFIG);
        assertEquals(SenderMatcher.NONE, result.matchType());
        assertNotNull(result.message());
    }

    @Test
    void aRowWithNoActiveFlagIsTreatedAsActive() {
        Map<String, Object> row = row("r-1", "Jane", "jane@mbl.example", "", "c-1", null);
        var result = SenderMatcher.match("jane@mbl.example", null,
                ReporterDirectoryEntry.fromInput(List.of(row)), CONFIG);
        assertEquals(SenderMatcher.EXACT, result.matchType());
    }

    @Test
    void transformerMapsEveryFieldIncludingCandidates() {
        var result = SenderMatcher.match("shared@mbl.example", null,
                directory(row("r-1", "Jane Doe", "shared@mbl.example", "", "c-1", true),
                        row("r-2", "John Roe", "shared@mbl.example", "", "c-2", true)), CONFIG);
        SenderMatch entity = SenderMatchTransformer.transform(result);
        assertEquals("Ambiguous", entity.matchType);
        assertEquals(2.0, entity.candidateCount);
        assertEquals(2, entity.candidates.size());
        assertEquals("r-1", entity.candidates.get(0).get("Reporter Id"));
        assertEquals("Email", entity.candidates.get(0).get("Matched On"));
        assertEquals(8, entity.toFields().size());
    }

    @Test
    void transformerIsNullSafe() {
        org.junit.jupiter.api.Assertions.assertNull(SenderMatchTransformer.transform(null));
    }
}
