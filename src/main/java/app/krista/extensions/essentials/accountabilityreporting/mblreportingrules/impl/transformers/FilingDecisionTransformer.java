package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.transformers;

import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities.FilingDecision;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.Dates;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.FilingDecisionEngine;

/** Stateless {@link FilingDecisionEngine.FilingDecisionResult} → {@link FilingDecision} transformer. */
public final class FilingDecisionTransformer {

    private FilingDecisionTransformer() {
    }

    public static FilingDecision transform(FilingDecisionEngine.FilingDecisionResult result) {
        if (result == null) {
            return null;
        }
        FilingDecision e = new FilingDecision();
        e.decision = result.decision();
        e.reporterId = result.reporterId();
        e.companyId = result.companyId();
        e.weekStart = Dates.formatDate(result.weekStart());
        e.reason = result.reason();
        return e;
    }
}
