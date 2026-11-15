package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.transformers;

import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities.StatedPeriod;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.Dates;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.StatedPeriodDetector;

/** Stateless {@link StatedPeriodDetector.StatedPeriodResult} → {@link StatedPeriod} transformer. */
public final class StatedPeriodTransformer {

    private StatedPeriodTransformer() {
    }

    public static StatedPeriod transform(StatedPeriodDetector.StatedPeriodResult result) {
        if (result == null) {
            return null;
        }
        StatedPeriod e = new StatedPeriod();
        e.periodFound = result.periodFound();
        e.weekStart = Dates.formatDate(result.weekStart());
        e.weekEnd = Dates.formatDate(result.weekEnd());
        e.evidenceText = result.evidenceText();
        e.confidence = result.confidence();
        return e;
    }
}
