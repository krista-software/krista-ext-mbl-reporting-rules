package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.transformers;

import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities.WeekWindow;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.Dates;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.WeekCalculator;

/** Stateless {@link WeekCalculator.WeekWindowResult} → {@link WeekWindow} transformer. */
public final class WeekWindowTransformer {

    private WeekWindowTransformer() {
    }

    public static WeekWindow transform(WeekCalculator.WeekWindowResult result) {
        if (result == null) {
            return null;
        }
        WeekWindow e = new WeekWindow();
        e.weekStart = Dates.formatDate(result.weekStart());
        e.weekEnd = Dates.formatDate(result.weekEnd());
        e.weekLabel = result.weekLabel();
        return e;
    }
}
