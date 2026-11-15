package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.MblReportingRulesAttributes;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities.ReportStatus;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.support.AbstractArea;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.support.TelemetryHelper;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.Dates;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.ReportStatusCalculator;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.RulesConfig;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.transformers.ReportStatusTransformer;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/** Report Status catalog requests. */
@Domain(id = "catEntryDomain_56218221-d59c-4082-b138-6a13f3c2c707",
        name = "Accountability Reporting",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "af527a74-6686-4c9a-8ac5-487795bd427f")
public class ReportStatusArea extends AbstractArea {

    @Inject
    public ReportStatusArea(MblReportingRulesAttributes attributes, TelemetryHelper telemetry,
                            Invoker invoker) {
        super(attributes, telemetry, invoker);
    }

    @CatalogRequest(
            id = "localDomainRequest_8192a3b4-c5d6-51e2-b304-7f8091a2b3c4",
            name = "Get Report Status",
            description = "Reports where one reporter stands for one week: Excused, Received (filed at "
                    + "or before the due moment), Late, Missing (nothing filed and past due) or Pending "
                    + "(nothing filed and not yet due).",
            area = "Report Status",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Report Status", type = "Entity(Report Status)", required = false)
    @Field.Text(name = "Status", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Received At", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {})
    @Field.Boolean(name = "Success", required = false, attributes = {}, options = {})
    @Field.Text(name = "Message", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    public ExtensionResponse getReportStatus(
            @Field.Text(name = "Reporter Id", required = true,
                    attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
            String reporterId,
            @Field.Text(name = "Week Start", required = true,
                    description = "First day of the week in question, ISO-8601 (YYYY-MM-DD).",
                    attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
            String weekStart,
            @Field.Desc(name = "Filed Reports",
                    type = "[ { Reporter Id: Text, Week Start: Text, Received At: Text, "
                            + "Excused: Boolean } ]",
                    required = false,
                    description = "Reports already on file. Week Start is ISO-8601 (YYYY-MM-DD); "
                            + "Received At is a full ISO-8601 timestamp.")
            List<Map<String, Object>> filedReports,
            @Field.Text(name = "As Of", required = false,
                    description = "The moment to evaluate against, full ISO-8601. Defaults to now.",
                    attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {})
            String asOf) {
        return audited("GetReportStatus",
                outputs("Report Status", "Status", "Received At", SUCCESS, MESSAGE),
                () -> {
                    RulesConfig config = config();
                    LocalDate week = Dates.parseDate(weekStart, "Week Start", config.getZone());
                    ZonedDateTime evaluateAt = Dates.parseOptionalTimestamp(asOf, "As Of", config.getZone());
                    if (evaluateAt == null) {
                        evaluateAt = ZonedDateTime.now(config.getZone());
                    }
                    List<ReportStatusCalculator.FiledReport> reports =
                            ReportStatusCalculator.FiledReport.fromInput(filedReports, config);

                    ReportStatusCalculator.ReportStatusResult result = ReportStatusCalculator.compute(
                            reporterId, week, reports, evaluateAt, config);
                    ReportStatus entity = ReportStatusTransformer.transform(result, reporterId.trim(), week);

                    Map<String, Object> out = out();
                    out.put("Report Status", entity);
                    out.put("Status", entity.status);
                    out.put("Received At", entity.receivedAt);
                    out.put(MESSAGE, result.message());
                    return out;
                });
    }
}
