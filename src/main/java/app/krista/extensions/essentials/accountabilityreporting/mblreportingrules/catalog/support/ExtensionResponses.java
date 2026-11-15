package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.support;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;

import java.util.Map;

/** Builds {@link ExtensionResponse} consistently for this extension. */
public final class ExtensionResponses {

    private ExtensionResponses() {
    }

    public static ExtensionResponse ok(Map<String, Object> values) {
        return new ExtensionResponseBuilder().success(values).build();
    }
}
