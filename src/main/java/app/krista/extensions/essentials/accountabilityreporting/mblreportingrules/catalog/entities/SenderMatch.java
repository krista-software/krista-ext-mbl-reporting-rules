package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities;

import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Entity;
import app.krista.extension.impl.anno.Field;
import app.krista.extension.impl.anno.ToString;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The result of resolving an email sender against the reporter directory. From Address is the address
 * that was looked up; Reporter Id and Company Id are populated only for an Exact or Alias match.
 */
@Domain(id = "catEntryDomain_56218221-d59c-4082-b138-6a13f3c2c707",
        name = "Accountability Reporting",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "af527a74-6686-4c9a-8ac5-487795bd427f")
@Entity(name = "Sender Match", id = "localDomainEntity_5a1c7b40-2f83-5c91-9d14-6e0b2a7f3c11",
        primaryKey = "From Address", supportStore = false,
        description = "The result of resolving an email sender against the reporter directory. "
                + "Match Type is Exact, Alias, None or Ambiguous; an inactive reporter never matches.")
public class SenderMatch {

    @ToString
    @Field.Text(name = "From Address", required = false,
            description = "The sender address that was resolved, lower-cased.",
            attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {})
    public String fromAddress;

    @Field.Text(name = "Match Type", required = false,
            description = "Exact, Alias, None or Ambiguous.",
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String matchType;

    @Field.Text(name = "Reporter Id", required = false,
            description = "The matched reporter, empty unless Match Type is Exact or Alias.",
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String reporterId;

    @Field.Text(name = "Reporter Name", required = false,
            description = "The matched reporter's display name.",
            attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {})
    public String reporterName;

    @Field.Text(name = "Company Id", required = false,
            description = "The matched reporter's company, empty when they have none.",
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String companyId;

    @Field(name = "Candidate Count", type = "Number", required = false,
            description = "How many directory rows the address could refer to.",
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Double candidateCount;

    @Field.Boolean(name = "Is Reporting Mailbox", required = false,
            description = "True when the sender is the shared reporting mailbox itself, which is skipped.",
            attributes = {}, options = {})
    public Boolean isReportingMailbox;

    @Field.Desc(name = "Candidates",
            type = "[ { Reporter Id: Text, Name: Text, Company Id: Text, Matched On: Text } ]",
            required = false,
            description = "Every directory row the address could refer to. Matched On is Email, Alias "
                    + "or Inactive.")
    public List<Map<String, Object>> candidates;

    /** Insertion-ordered field map keyed by the @Field display names. */
    public Map<String, Object> toFields() {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("From Address", fromAddress);
        f.put("Match Type", matchType);
        f.put("Reporter Id", reporterId);
        f.put("Reporter Name", reporterName);
        f.put("Company Id", companyId);
        f.put("Candidate Count", candidateCount);
        f.put("Is Reporting Mailbox", isReportingMailbox);
        f.put("Candidates", candidates);
        return f;
    }
}
