package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities;

import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Entity;
import app.krista.extension.impl.anno.Field;
import app.krista.extension.impl.anno.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Where one reporter stands for one week: Received, Late, Missing, Excused or Pending. Due Moment is
 * the instant the answer was measured against, derived from the Due Day and Time attribute.
 */
@Domain(id = "catEntryDomain_56218221-d59c-4082-b138-6a13f3c2c707",
        name = "Accountability Reporting",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "af527a74-6686-4c9a-8ac5-487795bd427f")
@Entity(name = "Report Status", id = "localDomainEntity_af61c095-7ed8-5146-b269-b3407f428166",
        primaryKey = "Reporter Id", supportStore = false,
        description = "A reporter's standing for one week: Received, Late, Missing, Excused or Pending.")
public class ReportStatus {

    @Field.Text(name = "Reporter Id", required = false,
            description = "The reporter this status is for.",
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String reporterId;

    @Field.Text(name = "Week Start", required = false,
            description = "First day of the week in question, ISO-8601 (YYYY-MM-DD).",
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String weekStart;

    @ToString
    @Field.Text(name = "Status", required = false,
            description = "Received, Late, Missing, Excused or Pending.",
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String status;

    @Field.Text(name = "Received At", required = false,
            description = "When the report was filed, full ISO-8601; empty when nothing was filed.",
            attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {})
    public String receivedAt;

    @Field.Text(name = "Due Moment", required = false,
            description = "The instant the report was due, full ISO-8601 in the configured time zone.",
            attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {})
    public String dueMoment;

    /** Insertion-ordered field map keyed by the @Field display names. */
    public Map<String, Object> toFields() {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("Reporter Id", reporterId);
        f.put("Week Start", weekStart);
        f.put("Status", status);
        f.put("Received At", receivedAt);
        f.put("Due Moment", dueMoment);
        return f;
    }
}
