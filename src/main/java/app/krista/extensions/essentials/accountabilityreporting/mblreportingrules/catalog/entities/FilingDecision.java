package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities;

import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Entity;
import app.krista.extension.impl.anno.Field;
import app.krista.extension.impl.anno.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What to do with one message: File as Report, File under Company, Hold for Review or Set Aside.
 * Reason always explains the outcome, and names both weeks when a disagreement caused the hold.
 */
@Domain(id = "catEntryDomain_56218221-d59c-4082-b138-6a13f3c2c707",
        name = "Accountability Reporting",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "af527a74-6686-4c9a-8ac5-487795bd427f")
@Entity(name = "Filing Decision", id = "localDomainEntity_8d4fae73-5cb6-5f24-a047-912e5d206f44",
        primaryKey = "Reporter Id", supportStore = false,
        description = "The filing outcome for one message. An unknown or ambiguous sender is never "
                + "auto-adopted, and a short reply is never filed.")
public class FilingDecision {

    @Field.Text(name = "Reporter Id", required = false,
            description = "The reporter the message was attributed to, empty when none was resolved.",
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String reporterId;

    @ToString
    @Field.Text(name = "Decision", required = false,
            description = "File as Report, File under Company, Hold for Review or Set Aside.",
            attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {})
    public String decision;

    @Field.Text(name = "Company Id", required = false,
            description = "The company the message would be filed under, when relevant.",
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String companyId;

    @Field.Text(name = "Week Start", required = false,
            description = "First day of the reporting week, ISO-8601 (YYYY-MM-DD).",
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String weekStart;

    @Field(name = "Reason", type = "Paragraph", required = false,
            description = "Why this decision was reached, in plain language.",
            attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    public String reason;

    /** Insertion-ordered field map keyed by the @Field display names. */
    public Map<String, Object> toFields() {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("Reporter Id", reporterId);
        f.put("Decision", decision);
        f.put("Company Id", companyId);
        f.put("Week Start", weekStart);
        f.put("Reason", reason);
        return f;
    }
}
