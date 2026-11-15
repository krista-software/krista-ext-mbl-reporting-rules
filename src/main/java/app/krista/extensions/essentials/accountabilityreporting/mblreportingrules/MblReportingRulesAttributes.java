package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules;

import app.krista.extension.executor.Invoker;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

/**
 * MBL Reporting Rules connection configuration.
 *
 * <p>There are no credentials here — this extension contacts no external system. These four values are
 * local policy settings that the rules need: which mailbox address to skip, where a week starts, which
 * time zone week boundaries and due moments are evaluated in, and when a week's report is due.
 *
 * <p>HK2 {@code @Service} so it can be injected into the extension class and every Area. The key
 * constants are referenced by the class-level {@code @Field.*} annotations on
 * {@link MblReportingRulesExtension}; keep the key strings stable — they are the saved-configuration
 * contract.
 */
@Service
public class MblReportingRulesAttributes {

    private static final Logger LOGGER = LoggerFactory.getLogger(MblReportingRulesAttributes.class);

    public static final String REPORTING_MAILBOX_ADDRESS = "Reporting Mailbox Address";
    public static final String WEEK_STARTS_ON = "Week Starts On";
    public static final String TIME_ZONE = "Time Zone";
    public static final String DUE_DAY_AND_TIME = "Due Day and Time";

    private String reportingMailboxAddress;
    private String weekStartsOn;
    private String timeZone;
    private String dueDayAndTime;

    @Inject
    public MblReportingRulesAttributes(Invoker invoker) {
        this(invoker.getAttributes());
    }

    public MblReportingRulesAttributes(Map<String, Object> attributes) {
        update(attributes);
    }

    public static MblReportingRulesAttributes create(Invoker invoker, Map<String, Object> attributes) {
        MblReportingRulesAttributes result = new MblReportingRulesAttributes(invoker);
        result.update(attributes);
        return result;
    }

    public void update(Map<String, Object> attributes) {
        this.reportingMailboxAddress = getString(attributes, REPORTING_MAILBOX_ADDRESS, "");
        this.weekStartsOn = getString(attributes, WEEK_STARTS_ON, "");
        this.timeZone = getString(attributes, TIME_ZONE, "");
        this.dueDayAndTime = getString(attributes, DUE_DAY_AND_TIME, "");
        // No secrets exist in this extension, so logging the configuration is safe.
        LOGGER.info("MBL Reporting Rules attributes updated - week starts on: '{}', time zone: '{}', due: '{}'",
                weekStartsOn, timeZone, dueDayAndTime);
    }

    private String getString(Map<String, Object> attributes, String key, String defaultValue) {
        Object value = attributes == null ? null : attributes.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    public String getReportingMailboxAddress() {
        return reportingMailboxAddress;
    }

    public String getWeekStartsOn() {
        return weekStartsOn;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public String getDueDayAndTime() {
        return dueDayAndTime;
    }
}
