package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities;

import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Entity;
import app.krista.extension.impl.anno.Field;
import app.krista.extension.impl.anno.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One reporting week: its first day, its last day, and the label people read it by. The boundaries
 * follow the Week Starts On connection attribute.
 */
@Domain(id = "catEntryDomain_56218221-d59c-4082-b138-6a13f3c2c707",
        name = "Accountability Reporting",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "af527a74-6686-4c9a-8ac5-487795bd427f")
@Entity(name = "Week Window", id = "localDomainEntity_9e50bf84-6dc7-5035-b158-a23f6e317055",
        primaryKey = "Week Start", supportStore = false,
        description = "A reporting week, bounded by the configured week start (Monday or Sunday).")
public class WeekWindow {

    @Field.Text(name = "Week Start", required = false,
            description = "First day of the week, ISO-8601 (YYYY-MM-DD).",
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String weekStart;

    @Field.Text(name = "Week End", required = false,
            description = "Last day of the week, ISO-8601 (YYYY-MM-DD).",
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String weekEnd;

    @ToString
    @Field.Text(name = "Week Label", required = false,
            description = "Display label, for example 'Week of Nov 3, 2025'.",
            attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {})
    public String weekLabel;

    /** Insertion-ordered field map keyed by the @Field display names. */
    public Map<String, Object> toFields() {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("Week Start", weekStart);
        f.put("Week End", weekEnd);
        f.put("Week Label", weekLabel);
        return f;
    }
}
