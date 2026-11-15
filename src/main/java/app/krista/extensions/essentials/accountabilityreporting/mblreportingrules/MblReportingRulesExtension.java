package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules;

import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.ChangeLog;
import app.krista.extension.impl.anno.Extension;
import app.krista.extension.impl.anno.Field;
import app.krista.extension.impl.anno.InvokerRequest;
import app.krista.extension.impl.anno.Java;
import app.krista.extension.impl.anno.StaticResource;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.RulesConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

/**
 * MBL Reporting Rules extension entry point — deterministic rules for filing weekly accountability
 * reports emailed to a shared reporting mailbox.
 *
 * <p><strong>No authentication.</strong> The extension contacts no external system, makes no network
 * calls and stores no credentials, so there are no secured fields and no authentication tab. The four
 * connection fields are local policy settings; Test Connection and attribute validation check them
 * locally.
 */
@Field.Text(value = MblReportingRulesAttributes.REPORTING_MAILBOX_ADDRESS, required = false)
@Field.PickOne(name = MblReportingRulesAttributes.WEEK_STARTS_ON, values = {"Monday", "Sunday"})
@Field.Text(value = MblReportingRulesAttributes.TIME_ZONE)
@Field.Text(value = MblReportingRulesAttributes.DUE_DAY_AND_TIME)
@Extension(name = "MBL Reporting Rules", version = "1.0.0",
        description = "Deterministic rules for filing weekly accountability reports that staff and "
                + "portfolio companies email to a shared reporting mailbox.")
@StaticResource(path = "docs", file = "docs")
@ChangeLog(file = "docs/pages/ReleaseNotes.md")
@Java(version = Java.Version.JAVA_21)
public final class MblReportingRulesExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(MblReportingRulesExtension.class);

    private final Invoker invoker;
    private final MblReportingRulesAttributes attributes;

    @Inject
    public MblReportingRulesExtension(Invoker invoker, MblReportingRulesAttributes attributes) {
        this.invoker = invoker;
        this.attributes = attributes;
    }

    /** Setup-tab tabs. There is no Authentication tab: this extension has no credentials. */
    @InvokerRequest(InvokerRequest.Type.CUSTOM_TABS)
    public Map<String, String> customTabs() {
        return Map.of("Documentation", "static/docs");
    }

    /**
     * Validate configuration before it is saved. Rejects an unknown time zone, a week start that is
     * neither Monday nor Sunday, and a Due Day and Time that does not parse.
     */
    @InvokerRequest(InvokerRequest.Type.VALIDATE_ATTRIBUTES)
    public void validateAttributes(Map<String, Object> connectionAttributes) {
        RulesConfig.parse(connectionAttributes,
                MblReportingRulesAttributes.REPORTING_MAILBOX_ADDRESS,
                MblReportingRulesAttributes.WEEK_STARTS_ON,
                MblReportingRulesAttributes.TIME_ZONE,
                MblReportingRulesAttributes.DUE_DAY_AND_TIME);
        LOGGER.info("MBL Reporting Rules configuration validated for invoker {}", invoker.getInvokerId());
    }

    /**
     * Test-Connection button. There is nothing to connect to, so this validates the saved settings —
     * the time zone, the week start and the due day/time — and throws if any of them is unusable.
     */
    @InvokerRequest(InvokerRequest.Type.TEST_CONNECTION)
    public void testConnection() {
        RulesConfig config = RulesConfig.parse(attributes.getReportingMailboxAddress(),
                attributes.getWeekStartsOn(), attributes.getTimeZone(), attributes.getDueDayAndTime());
        LOGGER.info("MBL Reporting Rules configuration is valid for invoker {}: {}",
                invoker.getInvokerId(), config.describe());
    }

    /** React to Setup-tab configuration changes. */
    @InvokerRequest(InvokerRequest.Type.INVOKER_UPDATED)
    public void attributesUpdated(Map<String, Object> oldAttributes, Map<String, Object> newAttributes) {
        attributes.update(newAttributes);
    }
}
