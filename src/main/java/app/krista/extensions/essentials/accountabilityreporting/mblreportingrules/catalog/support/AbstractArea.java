package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.support;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.Invoker;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.MblReportingRulesAttributes;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.RulesConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared base for every Area in this extension.
 *
 * <p>Owns the one {@code audited(...)} boundary so each {@code @CatalogRequest} method stays thin:
 * read inputs → call a pure rule class → transform → return. The wrapper writes timing, telemetry and
 * the uniform try/catch exactly once.
 *
 * <p><strong>Never an empty reply.</strong> Every request declares {@code Success} and {@code Message}
 * alongside its own outputs, and the wrapper guarantees both are populated on every path: on failure
 * it still returns a success-shaped response carrying {@code Success = false}, the actionable message,
 * and an explicit null for each declared output, so a workflow can always branch on the result instead
 * of hitting a missing field.
 */
public abstract class AbstractArea {

    public static final String SUCCESS = "Success";
    public static final String MESSAGE = "Message";

    protected static final String PREFIX = "mblreportingrules";

    private static final Logger LOG = LoggerFactory.getLogger(AbstractArea.class);

    protected final MblReportingRulesAttributes attributes;
    protected final TelemetryHelper telemetry;
    protected final String invokerId;

    protected AbstractArea(MblReportingRulesAttributes attributes, TelemetryHelper telemetry, Invoker invoker) {
        this.attributes = attributes;
        this.telemetry = telemetry;
        this.invokerId = invoker.getInvokerId();
    }

    /** The parsed, validated connection configuration. Throws with an actionable message if invalid. */
    protected RulesConfig config() {
        return RulesConfig.parse(attributes.getReportingMailboxAddress(), attributes.getWeekStartsOn(),
                attributes.getTimeZone(), attributes.getDueDayAndTime());
    }

    @FunctionalInterface
    protected interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    /**
     * The uniform boundary: timing + telemetry + try/catch → a fully populated response.
     *
     * @param name       operation name for logs and metrics (low cardinality)
     * @param outputKeys every output field the request declares, so failures still answer with all of
     *                   them present
     * @param action     the work, returning the populated output map
     */
    protected ExtensionResponse audited(String name, Collection<String> outputKeys,
                                        CheckedSupplier<Map<String, Object>> action) {
        long started = telemetry.start(PREFIX + "." + name);
        LOG.info("Catalog '{}' started for invoker {}", name, invokerId);
        try {
            Map<String, Object> values = action.get();
            Map<String, Object> out = shape(outputKeys, values);
            out.put(SUCCESS, true);
            if (!out.containsKey(MESSAGE) || out.get(MESSAGE) == null) {
                out.put(MESSAGE, "OK.");
            }
            LOG.info("Catalog '{}' ok in {}ms for invoker {}", name, System.currentTimeMillis() - started,
                    invokerId);
            telemetry.recordSuccess(PREFIX + "." + name, started);
            return ExtensionResponses.ok(out);
        } catch (IllegalArgumentException e) {
            LOG.warn("Catalog '{}' input error for invoker {}: {}", name, invokerId, e.getMessage());
            telemetry.recordError(PREFIX + "." + name, started, e);
            return failure(outputKeys, e.getMessage());
        } catch (Exception e) {
            LOG.error("Catalog '{}' failed for invoker {}", name, invokerId, e);
            telemetry.recordError(PREFIX + "." + name, started, e);
            String detail = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return failure(outputKeys, "The request could not be completed: " + detail);
        }
    }

    private ExtensionResponse failure(Collection<String> outputKeys, String message) {
        Map<String, Object> out = shape(outputKeys, Map.of());
        out.put(SUCCESS, false);
        out.put(MESSAGE, message);
        return ExtensionResponses.ok(out);
    }

    /** Every declared output key present, in declaration order, defaulting to null. */
    private Map<String, Object> shape(Collection<String> outputKeys, Map<String, Object> values) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (outputKeys != null) {
            for (String key : outputKeys) {
                out.put(key, values.get(key));
            }
        }
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            out.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return out;
    }

    /** A fresh, insertion-ordered output map. */
    protected static Map<String, Object> out() {
        return new LinkedHashMap<>();
    }

    protected static List<String> outputs(String... keys) {
        return List.of(keys);
    }
}
