package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.MblReportingRulesAttributes;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.support.AbstractArea;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.support.TelemetryHelper;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.RulesConfig;

import javax.inject.Inject;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Map;

/**
 * Setup area — connectivity checks for an extension that connects to nothing.
 *
 * <p>There is no external system and no credential, so both requests validate the saved configuration
 * locally: the IANA time zone, the week start, and the Due Day and Time format.
 */
@Domain(id = "catEntryDomain_56218221-d59c-4082-b138-6a13f3c2c707",
        name = "Accountability Reporting",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "af527a74-6686-4c9a-8ac5-487795bd427f")
public class SetupArea extends AbstractArea {

    @Inject
    public SetupArea(MblReportingRulesAttributes attributes, TelemetryHelper telemetry, Invoker invoker) {
        super(attributes, telemetry, invoker);
    }

    @CatalogRequest(
            id = "localDomainRequest_1a2b3c4d-5e6f-5a7b-8c9d-0e1f2a3b4c5d",
            name = "Test Connection",
            description = "Validates the saved configuration: the IANA time zone, the week start, and "
                    + "the Due Day and Time format. No external system is contacted.",
            area = "Setup",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Boolean(name = "Success", required = false, attributes = {}, options = {})
    @Field.Text(name = "Message", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    public ExtensionResponse testConnection() {
        return audited("TestConnection", outputs(SUCCESS, MESSAGE), () -> {
            RulesConfig config = config();
            Map<String, Object> out = out();
            out.put(MESSAGE, "Configuration is valid: " + config.describe()
                    + ". This extension performs no network calls, so there is nothing further to test.");
            return out;
        });
    }

    @CatalogRequest(
            id = "localDomainRequest_2b3c4d5e-6f70-5b8c-9daf-1f2a3b4c5d6e",
            name = "Health Check",
            description = "Reports extension health: whether the saved configuration still parses, plus "
                    + "JVM heap usage.",
            area = "Setup",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Boolean(name = "Success", required = false, attributes = {}, options = {})
    @Field.Text(name = "Message", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    @Field.Boolean(name = "Is Healthy", required = false, attributes = {}, options = {})
    @Field(name = "Heap Used MB", type = "Number", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.Text(name = "Details", required = false,
            attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    public ExtensionResponse healthCheck() {
        return audited("HealthCheck", outputs(SUCCESS, MESSAGE, "Is Healthy", "Heap Used MB", "Details"), () -> {
            MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
            double heapUsedMb = memory.getHeapMemoryUsage().getUsed() / (1024.0 * 1024.0);
            Map<String, Object> out = out();
            out.put("Heap Used MB", Math.round(heapUsedMb * 100.0) / 100.0);
            String details;
            boolean healthy;
            try {
                details = "Configuration is valid: " + config().describe() + ".";
                healthy = true;
            } catch (IllegalArgumentException e) {
                // A bad configuration is a health finding, not a request failure.
                details = "Configuration problem: " + e.getMessage();
                healthy = false;
            }
            out.put("Is Healthy", healthy);
            out.put("Details", details);
            out.put(MESSAGE, healthy ? "Extension is healthy." : "Extension is running but misconfigured.");
            return out;
        });
    }
}
