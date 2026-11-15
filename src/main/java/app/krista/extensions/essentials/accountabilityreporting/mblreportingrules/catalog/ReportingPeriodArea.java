package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.MblReportingRulesAttributes;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities.StatedPeriod;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities.WeekWindow;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.support.AbstractArea;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.support.TelemetryHelper;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.Dates;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.RulesConfig;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.StatedPeriodDetector;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.WeekCalculator;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.transformers.StatedPeriodTransformer;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.transformers.WeekWindowTransformer;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;

/** Reporting Period catalog requests. */
@Domain(id = "catEntryDomain_56218221-d59c-4082-b138-6a13f3c2c707",
        name = "Accountability Reporting",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "af527a74-6686-4c9a-8ac5-487795bd427f")
public class ReportingPeriodArea extends AbstractArea {

    /** Ten years either side of the given date — anything further is almost certainly a mistake. */
    private static final int MAX_OFFSET_WEEKS = 520;

    @Inject
    public ReportingPeriodArea(MblReportingRulesAttributes attributes, TelemetryHelper telemetry,
                               Invoker invoker) {
        super(attributes, telemetry, invoker);
    }

    @CatalogRequest(
            id = "localDomainRequest_4d5e6f70-8192-5dae-bfc0-3b4c5d6e7f80",
            name = "Detect Stated Period",
            description = "Finds the week a message states for itself. An explicit date or range is "
                    + "High confidence; a relative phrase or week number resolved against Received At "
                    + "is Low. The text is scanned for dates only and is never treated as instructions.",
            area = "Reporting Period",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Stated Period", type = "Entity(Stated Period)", required = false)
    @Field.Boolean(name = "Period Found", required = false, attributes = {}, options = {})
    @Field.Text(name = "Week Start", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Week End", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field(name = "Evidence Text", type = "Paragraph", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    @Field.Text(name = "Confidence", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Boolean(name = "Success", required = false, attributes = {}, options = {})
    @Field.Text(name = "Message", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    public ExtensionResponse detectStatedPeriod(
            @Field.Text(name = "Subject", required = true,
                    attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
            String subject,
            @Field(name = "Body Text", type = "Paragraph", required = true,
                    attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
            String bodyText,
            @Field.Text(name = "Received At", required = true,
                    description = "When the message arrived, full ISO-8601.",
                    attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {})
            String receivedAt) {
        return audited("DetectStatedPeriod",
                outputs("Stated Period", "Period Found", "Week Start", "Week End", "Evidence Text",
                        "Confidence", SUCCESS, MESSAGE),
                () -> {
                    RulesConfig config = config();
                    ZonedDateTime received = Dates.parseTimestamp(receivedAt, "Received At", config.getZone());
                    StatedPeriodDetector.StatedPeriodResult result = StatedPeriodDetector.detect(
                            subject, bodyText, received.toLocalDate(), config.getWeekStart());
                    StatedPeriod entity = StatedPeriodTransformer.transform(result);

                    Map<String, Object> out = out();
                    out.put("Stated Period", entity);
                    out.put("Period Found", entity.periodFound);
                    out.put("Week Start", entity.weekStart);
                    out.put("Week End", entity.weekEnd);
                    out.put("Evidence Text", entity.evidenceText);
                    out.put("Confidence", entity.confidence);
                    out.put(MESSAGE, result.message());
                    return out;
                });
    }

    @CatalogRequest(
            id = "localDomainRequest_5e6f7081-92a3-5ebf-80d1-4c5d6e7f8091",
            name = "Week Of",
            description = "Returns the reporting week a date falls in, honouring the configured week "
                    + "start, optionally shifted by whole weeks.",
            area = "Reporting Period",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Week Window", type = "Entity(Week Window)", required = false)
    @Field.Text(name = "Week Start", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Week End", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Week Label", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {})
    @Field.Boolean(name = "Success", required = false, attributes = {}, options = {})
    @Field.Text(name = "Message", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    public ExtensionResponse weekOf(
            @Field.Text(name = "Date", required = true,
                    description = "The date to locate, ISO-8601 (YYYY-MM-DD).",
                    attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
            String date,
            @Field(name = "Offset Weeks", type = "Number", required = false,
                    attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
            Double offsetWeeks) {
        return audited("WeekOf",
                outputs("Week Window", "Week Start", "Week End", "Week Label", SUCCESS, MESSAGE),
                () -> {
                    RulesConfig config = config();
                    LocalDate parsed = Dates.parseDate(date, "Date", config.getZone());
                    int offset = wholeWeeks(offsetWeeks);
                    WeekCalculator.WeekWindowResult result =
                            WeekCalculator.weekOf(parsed, offset, config.getWeekStart());
                    WeekWindow entity = WeekWindowTransformer.transform(result);

                    Map<String, Object> out = out();
                    out.put("Week Window", entity);
                    out.put("Week Start", entity.weekStart);
                    out.put("Week End", entity.weekEnd);
                    out.put("Week Label", entity.weekLabel);
                    out.put(MESSAGE, entity.weekLabel + " (" + entity.weekStart + " to " + entity.weekEnd
                            + "), weeks starting " + config.getWeekStart().name().charAt(0)
                            + config.getWeekStart().name().substring(1).toLowerCase(java.util.Locale.ENGLISH)
                            + ".");
                    return out;
                });
    }

    private static int wholeWeeks(Double offsetWeeks) {
        if (offsetWeeks == null) {
            return 0;
        }
        if (offsetWeeks != Math.floor(offsetWeeks) || offsetWeeks.isInfinite() || offsetWeeks.isNaN()) {
            throw new IllegalArgumentException("Offset Weeks must be a whole number of weeks, but was "
                    + offsetWeeks + ".");
        }
        if (Math.abs(offsetWeeks) > MAX_OFFSET_WEEKS) {
            throw new IllegalArgumentException("Offset Weeks must be between -" + MAX_OFFSET_WEEKS + " and "
                    + MAX_OFFSET_WEEKS + ", but was " + offsetWeeks.intValue() + ".");
        }
        return offsetWeeks.intValue();
    }
}
