package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.transformers;

import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities.ReportStatus;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.Dates;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.ReportStatusCalculator;

import java.time.LocalDate;

/** Stateless {@link ReportStatusCalculator.ReportStatusResult} → {@link ReportStatus} transformer. */
public final class ReportStatusTransformer {

    private ReportStatusTransformer() {
    }

    public static ReportStatus transform(ReportStatusCalculator.ReportStatusResult result,
                                         String reporterId, LocalDate weekStart) {
        if (result == null) {
            return null;
        }
        ReportStatus e = new ReportStatus();
        e.reporterId = reporterId;
        e.weekStart = Dates.formatDate(weekStart);
        e.status = result.status();
        e.receivedAt = Dates.formatTimestamp(result.receivedAt());
        e.dueMoment = Dates.formatTimestamp(result.dueMoment());
        return e;
    }
}
