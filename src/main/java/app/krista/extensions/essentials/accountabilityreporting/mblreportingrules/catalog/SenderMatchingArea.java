package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.MblReportingRulesAttributes;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities.SenderMatch;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.support.AbstractArea;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.support.TelemetryHelper;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.ReporterDirectoryEntry;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.SenderMatcher;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.transformers.SenderMatchTransformer;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/** Sender Matching catalog requests. */
@Domain(id = "catEntryDomain_56218221-d59c-4082-b138-6a13f3c2c707",
        name = "Accountability Reporting",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "af527a74-6686-4c9a-8ac5-487795bd427f")
public class SenderMatchingArea extends AbstractArea {

    @Inject
    public SenderMatchingArea(MblReportingRulesAttributes attributes, TelemetryHelper telemetry,
                              Invoker invoker) {
        super(attributes, telemetry, invoker);
    }

    @CatalogRequest(
            id = "localDomainRequest_3c4d5e6f-7081-5c9d-aebf-2a3b4c5d6e7f",
            name = "Match Sender",
            description = "Resolves an email sender against the reporter directory. Matching is "
                    + "case-insensitive and by address only; inactive reporters never match, and an "
                    + "address shared by two or more active reporters is Ambiguous.",
            area = "Sender Matching",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Sender Match", type = "Entity(Sender Match)", required = false)
    @Field.Text(name = "Match Type", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Reporter Id", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Company Id", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Desc(name = "Candidates",
            type = "[ { Reporter Id: Text, Name: Text, Company Id: Text, Matched On: Text } ]",
            required = false)
    @Field.Boolean(name = "Success", required = false, attributes = {}, options = {})
    @Field.Text(name = "Message", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    public ExtensionResponse matchSender(
            @Field.Text(name = "From Address", required = true,
                    attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {})
            String fromAddress,
            @Field.Text(name = "From Name", required = false,
                    attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {})
            String fromName,
            @Field.Desc(name = "Reporter Directory",
                    type = "[ { Id: Text, Name: Text, Emails: Text, Aliases: Text, Company Id: Text, "
                            + "Active: Boolean } ]",
                    required = true,
                    description = "Emails and Aliases each hold one or more addresses separated by "
                            + "commas or semicolons.")
            List<Map<String, Object>> reporterDirectory) {
        return audited("MatchSender",
                outputs("Sender Match", "Match Type", "Reporter Id", "Company Id", "Candidates",
                        SUCCESS, MESSAGE),
                () -> {
                    List<ReporterDirectoryEntry> directory =
                            ReporterDirectoryEntry.fromInput(reporterDirectory);
                    SenderMatcher.SenderMatchResult result =
                            SenderMatcher.match(fromAddress, fromName, directory, config());
                    SenderMatch entity = SenderMatchTransformer.transform(result);

                    Map<String, Object> out = out();
                    out.put("Sender Match", entity);
                    out.put("Match Type", entity.matchType);
                    out.put("Reporter Id", entity.reporterId);
                    out.put("Company Id", entity.companyId);
                    out.put("Candidates", entity.candidates);
                    out.put(MESSAGE, result.message());
                    return out;
                });
    }
}
