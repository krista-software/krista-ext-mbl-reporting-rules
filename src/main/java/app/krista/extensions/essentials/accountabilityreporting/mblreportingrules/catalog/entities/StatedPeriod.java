package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities;

import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Entity;
import app.krista.extension.impl.anno.Field;
import app.krista.extension.impl.anno.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The reporting period a message states for itself, normalised onto the configured week start.
 * Week Start and Week End are ISO-8601 dates (YYYY-MM-DD) and are empty when no period was found.
 */
@Domain(id = "catEntryDomain_56218221-d59c-4082-b138-6a13f3c2c707",
        name = "Accountability Reporting",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "af527a74-6686-4c9a-8ac5-487795bd427f")
@Entity(name = "Stated Period", id = "localDomainEntity_6b2d8c51-3a94-5d02-8e25-7f1c3b804d22",
        primaryKey = "Week Start", supportStore = false,
        description = "The week a message states for itself. Confidence is High for an explicit date "
                + "or range and Low for a relative phrase or week number.")
public class StatedPeriod {

    @ToString
    @Field.Text(name = "Week Start", required = false,
            description = "First day of the stated week, ISO-8601 (YYYY-MM-DD).",
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String weekStart;

    @Field.Boolean(name = "Period Found", required = false,
            description = "True when the message states a week or date range.",
            attributes = {}, options = {})
    public Boolean periodFound;

    @Field.Text(name = "Week End", required = false,
            description = "Last day of the stated week, ISO-8601 (YYYY-MM-DD).",
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String weekEnd;

    @Field(name = "Evidence Text", type = "Paragraph", required = false,
            description = "The exact wording the period was read from, prefixed with Subject or Body.",
            attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    public String evidenceText;

    @Field.Text(name = "Confidence", required = false,
            description = "High or Low.",
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String confidence;

    /** Insertion-ordered field map keyed by the @Field display names. */
    public Map<String, Object> toFields() {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("Week Start", weekStart);
        f.put("Period Found", periodFound);
        f.put("Week End", weekEnd);
        f.put("Evidence Text", evidenceText);
        f.put("Confidence", confidence);
        return f;
    }
}
