package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.MblReportingRulesAttributes;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities.FilingDecision;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities.ShortReplyAssessment;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.support.AbstractArea;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.support.Inputs;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.support.TelemetryHelper;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.FilingDecisionEngine;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.RulesConfig;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.ShortReplyDetector;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.transformers.FilingDecisionTransformer;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.transformers.ShortReplyAssessmentTransformer;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/** Email Triage catalog requests. */
@Domain(id = "catEntryDomain_56218221-d59c-4082-b138-6a13f3c2c707",
        name = "Accountability Reporting",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "af527a74-6686-4c9a-8ac5-487795bd427f")
public class EmailTriageArea extends AbstractArea {

    @Inject
    public EmailTriageArea(MblReportingRulesAttributes attributes, TelemetryHelper telemetry,
                           Invoker invoker) {
        super(attributes, telemetry, invoker);
    }

    @CatalogRequest(
            id = "localDomainRequest_6f708192-a3b4-5fc0-91e2-5d6e7f8091a2",
            name = "Is Short Reply",
            description = "Reports whether a message is a throwaway acknowledgement and whether it "
                    + "replies to mail we sent. Quoted history, signatures and disclaimers are stripped "
                    + "before the sender's own words are counted.",
            area = "Email Triage",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Short Reply Assessment", type = "Entity(Short Reply Assessment)", required = false)
    @Field.Boolean(name = "Is Short Reply", required = false, attributes = {}, options = {})
    @Field.Boolean(name = "Is Reply To Our Mail", required = false, attributes = {}, options = {})
    @Field(name = "Reason", type = "Paragraph", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    @Field.Boolean(name = "Success", required = false, attributes = {}, options = {})
    @Field.Text(name = "Message", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    public ExtensionResponse isShortReply(
            @Field.Text(name = "Subject", required = true,
                    attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
            String subject,
            @Field(name = "Body Text", type = "Paragraph", required = true,
                    attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
            String bodyText,
            @Field.Text(name = "In Reply To", required = false,
                    attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {})
            String inReplyTo,
            @Field.Text(name = "References", required = false,
                    attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
            String references,
            @Field.Desc(name = "Our Message Ids", type = "[ Text ]", required = false,
                    description = "Message ids of mail this mailbox sent, with or without angle brackets.")
            List<String> ourMessageIds) {
        return audited("IsShortReply",
                outputs("Short Reply Assessment", "Is Short Reply", "Is Reply To Our Mail", "Reason",
                        SUCCESS, MESSAGE),
                () -> {
                    ShortReplyDetector.ShortReplyResult result = ShortReplyDetector.evaluate(
                            subject, bodyText, inReplyTo, references, ourMessageIds);
                    ShortReplyAssessment entity = ShortReplyAssessmentTransformer.transform(result);

                    Map<String, Object> out = out();
                    out.put("Short Reply Assessment", entity);
                    out.put("Is Short Reply", entity.isShortReply);
                    out.put("Is Reply To Our Mail", entity.isReplyToOurMail);
                    out.put("Reason", entity.reason);
                    out.put(MESSAGE, result.reason());
                    return out;
                });
    }

    @CatalogRequest(
            id = "localDomainRequest_708192a3-b4c5-50d1-a2f3-6e7f8091a2b3",
            name = "Decide Filing",
            description = "Combines the sender, period, short-reply and model signals into one filing "
                    + "decision. An unknown or ambiguous sender is never auto-adopted; short replies and "
                    + "replies to our own mail are never filed; a week disagreement is held for review.",
            area = "Email Triage",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Filing Decision", type = "Entity(Filing Decision)", required = false)
    @Field.Text(name = "Decision", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "M")}, options = {})
    @Field.Text(name = "Reporter Id", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Company Id", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Week Start", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field(name = "Reason", type = "Paragraph", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    @Field.Boolean(name = "Success", required = false, attributes = {}, options = {})
    @Field.Text(name = "Message", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    public ExtensionResponse decideFiling(
            @Field.Desc(name = "Sender Match JSON",
                    type = "{ Match Type: Text, Reporter Id: Text, Company Id: Text, "
                            + "Candidate Count: Number, Is Reporting Mailbox: Boolean }",
                    required = true,
                    description = "The Match Sender result for this message.")
            Map<String, Object> senderMatch,
            @Field.Desc(name = "Period JSON",
                    type = "{ Period Found: Boolean, Week Start: Text, Week End: Text, "
                            + "Evidence Text: Text, Confidence: Text }",
                    required = true,
                    description = "The Detect Stated Period result for this message.")
            Map<String, Object> period,
            @Field.Desc(name = "Short Reply JSON",
                    type = "{ Is Short Reply: Boolean, Is Reply To Our Mail: Boolean, Reason: Text }",
                    required = true,
                    description = "The Is Short Reply result for this message.")
            Map<String, Object> shortReply,
            @Field.Desc(name = "Model Reading JSON",
                    type = "{ Kind: Text, Week: Text, Confidence: Text }",
                    required = true,
                    description = "The model's reading: Kind is 'own report', 'company update' or "
                            + "'not a report'; Week is an ISO-8601 date; Confidence is High or Low.")
            Map<String, Object> modelReading) {
        return audited("DecideFiling",
                outputs("Filing Decision", "Decision", "Reporter Id", "Company Id", "Week Start", "Reason",
                        SUCCESS, MESSAGE),
                () -> {
                    RulesConfig config = config();
                    Inputs.require(senderMatch, "Sender Match JSON");
                    Inputs.require(period, "Period JSON");
                    Inputs.require(shortReply, "Short Reply JSON");
                    Inputs.require(modelReading, "Model Reading JSON");

                    FilingDecisionEngine.SenderSignal sender = new FilingDecisionEngine.SenderSignal(
                            Inputs.str(senderMatch, "Match Type"),
                            Inputs.str(senderMatch, "Reporter Id"),
                            Inputs.str(senderMatch, "Company Id"),
                            Inputs.integer(senderMatch, "Candidate Count"),
                            Inputs.bool(senderMatch, "Is Reporting Mailbox"));
                    FilingDecisionEngine.PeriodSignal periodSignal = new FilingDecisionEngine.PeriodSignal(
                            Inputs.bool(period, "Period Found"),
                            Inputs.date(period, "Week Start", "Period JSON Week Start", config.getZone()),
                            Inputs.date(period, "Week End", "Period JSON Week End", config.getZone()),
                            Inputs.str(period, "Evidence Text"),
                            Inputs.str(period, "Confidence"));
                    FilingDecisionEngine.ShortReplySignal shortReplySignal =
                            new FilingDecisionEngine.ShortReplySignal(
                                    Inputs.bool(shortReply, "Is Short Reply"),
                                    Inputs.bool(shortReply, "Is Reply To Our Mail"),
                                    Inputs.str(shortReply, "Reason"));
                    FilingDecisionEngine.ModelReadingSignal model = new FilingDecisionEngine.ModelReadingSignal(
                            Inputs.str(modelReading, "Kind"),
                            Inputs.date(modelReading, "Week", "Model Reading JSON Week", config.getZone()),
                            Inputs.str(modelReading, "Confidence"));

                    FilingDecisionEngine.FilingDecisionResult result = FilingDecisionEngine.decide(
                            sender, periodSignal, shortReplySignal, model, config.getWeekStart());
                    FilingDecision entity = FilingDecisionTransformer.transform(result);

                    Map<String, Object> out = out();
                    out.put("Filing Decision", entity);
                    out.put("Decision", entity.decision);
                    out.put("Reporter Id", entity.reporterId);
                    out.put("Company Id", entity.companyId);
                    out.put("Week Start", entity.weekStart);
                    out.put("Reason", entity.reason);
                    out.put(MESSAGE, entity.decision + ". " + entity.reason);
                    return out;
                });
    }
}
