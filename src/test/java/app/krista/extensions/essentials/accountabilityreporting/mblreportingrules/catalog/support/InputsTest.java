package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.support;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reading the composite (inline-object) inputs Decide Filing and Get Report Status receive. */
class InputsTest {

    private static final ZoneId UTC = ZoneId.of("UTC");

    private static Map<String, Object> map() {
        return new LinkedHashMap<>();
    }

    @Test
    void missingKeysReadAsEmptyRatherThanThrowing() {
        Map<String, Object> source = map();
        assertEquals("", Inputs.str(source, "Match Type"));
        assertFalse(Inputs.bool(source, "Is Short Reply"));
        assertEquals(0, Inputs.integer(source, "Candidate Count"));
        assertNull(Inputs.date(source, "Week Start", "Week Start", UTC));
    }

    @Test
    void aNullCompositeIsRejectedByName() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Inputs.require(null, "Model Reading JSON"));
        assertTrue(e.getMessage().contains("Model Reading JSON"));
    }

    @Test
    void booleansAreReadFromBothRealBooleansAndText() {
        Map<String, Object> source = map();
        source.put("a", Boolean.TRUE);
        source.put("b", "true");
        source.put("c", "Yes");
        source.put("d", "false");
        assertTrue(Inputs.bool(source, "a"));
        assertTrue(Inputs.bool(source, "b"));
        assertTrue(Inputs.bool(source, "c"));
        assertFalse(Inputs.bool(source, "d"));
    }

    @Test
    void numbersAreReadFromBothNumbersAndText() {
        Map<String, Object> source = map();
        source.put("a", 3.0);
        source.put("b", "2");
        source.put("c", "not a number");
        assertEquals(3, Inputs.integer(source, "a"));
        assertEquals(2, Inputs.integer(source, "b"));
        assertEquals(0, Inputs.integer(source, "c"));
    }

    @Test
    void textIsTrimmed() {
        Map<String, Object> source = map();
        source.put("Kind", "  own report  ");
        assertEquals("own report", Inputs.str(source, "Kind"));
    }

    @Test
    void datesAreParsedAndBadOnesNameTheField() {
        Map<String, Object> source = map();
        source.put("Week", "2025-11-03");
        assertEquals(LocalDate.of(2025, 11, 3), Inputs.date(source, "Week", "Model Reading Week", UTC));

        source.put("Week", "last Tuesday");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Inputs.date(source, "Week", "Model Reading Week", UTC));
        assertTrue(e.getMessage().contains("Model Reading Week"));
    }
}
