package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.transformers;

import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities.SenderMatch;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.SenderMatcher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stateless {@link SenderMatcher.SenderMatchResult} → {@link SenderMatch} transformer. */
public final class SenderMatchTransformer {

    private SenderMatchTransformer() {
    }

    public static SenderMatch transform(SenderMatcher.SenderMatchResult result) {
        if (result == null) {
            return null;
        }
        SenderMatch e = new SenderMatch();
        e.fromAddress = result.fromAddress();
        e.matchType = result.matchType();
        e.reporterId = result.reporterId();
        e.reporterName = result.reporterName();
        e.companyId = result.companyId();
        e.candidateCount = (double) result.candidateCount();
        e.isReportingMailbox = result.isReportingMailbox();
        e.candidates = candidates(result.candidates());
        return e;
    }

    private static List<Map<String, Object>> candidates(List<SenderMatcher.Candidate> candidates) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (candidates == null) {
            return out;
        }
        for (SenderMatcher.Candidate c : candidates) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("Reporter Id", c.reporterId());
            row.put("Name", c.name());
            row.put("Company Id", c.companyId());
            row.put("Matched On", c.matchedOn());
            out.add(row);
        }
        return out;
    }
}
