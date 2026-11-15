package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resolves an email sender against the reporter directory.
 *
 * <p>The rules, in order:
 * <ol>
 *   <li>Mail from the configured reporting mailbox is flagged and never matched to a reporter.</li>
 *   <li>Only <strong>active</strong> reporters can match; an inactive reporter never matches, however
 *       exactly its address is spelled.</li>
 *   <li>Address comparison is case-insensitive and tolerates the {@code Name <addr>} header form.</li>
 *   <li>One active reporter on the primary address is {@code Exact}; one on an alias address is
 *       {@code Alias}; two or more on either is {@code Ambiguous}; none is {@code None}.</li>
 * </ol>
 *
 * <p>From Name is informational only — it is never used to match, so a display name can never adopt an
 * email to the wrong reporter.
 */
public final class SenderMatcher {

    public static final String EXACT = "Exact";
    public static final String ALIAS = "Alias";
    public static final String NONE = "None";
    public static final String AMBIGUOUS = "Ambiguous";

    private SenderMatcher() {
    }

    public static SenderMatchResult match(String fromAddress, String fromName,
                                          List<ReporterDirectoryEntry> directory, RulesConfig config) {
        String address = Validation.requireNonBlank(fromAddress, "From Address");
        String normalized = ReporterDirectoryEntry.normalizeAddress(address);
        String displayName = fromName == null ? "" : fromName.trim();
        List<ReporterDirectoryEntry> entries = directory == null ? List.of() : directory;

        if (config != null && config.isReportingMailbox(normalized)) {
            return new SenderMatchResult(normalized, NONE, "", "", "", true, List.of(),
                    "'" + normalized + "' is the shared reporting mailbox itself, so it is never treated "
                            + "as a reporter.");
        }

        List<ReporterDirectoryEntry> exact = new ArrayList<>();
        List<ReporterDirectoryEntry> alias = new ArrayList<>();
        List<ReporterDirectoryEntry> inactive = new ArrayList<>();
        for (ReporterDirectoryEntry entry : entries) {
            boolean onEmail = entry.hasEmail(normalized);
            boolean onAlias = entry.hasAlias(normalized);
            if (!onEmail && !onAlias) {
                continue;
            }
            if (!entry.isActive()) {
                inactive.add(entry);
            } else if (onEmail) {
                exact.add(entry);
            } else {
                alias.add(entry);
            }
        }

        List<ReporterDirectoryEntry> active = new ArrayList<>(exact);
        active.addAll(alias);
        if (active.size() > 1) {
            return new SenderMatchResult(normalized, AMBIGUOUS, "", "", displayName, false,
                    candidates(exact, alias, List.of()),
                    "'" + normalized + "' matches " + active.size()
                            + " active reporters, so the sender cannot be resolved automatically.");
        }
        if (exact.size() == 1) {
            ReporterDirectoryEntry e = exact.get(0);
            return new SenderMatchResult(normalized, EXACT, e.getId(), e.getCompanyId(), e.getName(), false,
                    candidates(exact, List.of(), List.of()),
                    "'" + normalized + "' is a primary address of " + describe(e) + ".");
        }
        if (alias.size() == 1) {
            ReporterDirectoryEntry e = alias.get(0);
            return new SenderMatchResult(normalized, ALIAS, e.getId(), e.getCompanyId(), e.getName(), false,
                    candidates(List.of(), alias, List.of()),
                    "'" + normalized + "' is a known alias address of " + describe(e) + ".");
        }
        if (!inactive.isEmpty()) {
            return new SenderMatchResult(normalized, NONE, "", "", displayName, false,
                    candidates(List.of(), List.of(), inactive),
                    "'" + normalized + "' belongs only to " + (inactive.size() == 1 ? "an inactive reporter"
                            : inactive.size() + " inactive reporters") + ", which never match.");
        }
        return new SenderMatchResult(normalized, NONE, "", "", displayName, false, List.of(),
                "'" + normalized + "' does not appear in the reporter directory.");
    }

    private static String describe(ReporterDirectoryEntry e) {
        return e.getName().isEmpty() ? "reporter " + e.getId() : e.getName() + " (" + e.getId() + ")";
    }

    private static List<Candidate> candidates(List<ReporterDirectoryEntry> exact,
                                              List<ReporterDirectoryEntry> alias,
                                              List<ReporterDirectoryEntry> inactive) {
        List<Candidate> out = new ArrayList<>();
        for (ReporterDirectoryEntry e : exact) {
            out.add(new Candidate(e.getId(), e.getName(), e.getCompanyId(), "Email"));
        }
        for (ReporterDirectoryEntry e : alias) {
            out.add(new Candidate(e.getId(), e.getName(), e.getCompanyId(), "Alias"));
        }
        for (ReporterDirectoryEntry e : inactive) {
            out.add(new Candidate(e.getId(), e.getName(), e.getCompanyId(), "Inactive"));
        }
        return Collections.unmodifiableList(out);
    }

    /** One directory row the address could refer to, and how it was reached. */
    public record Candidate(String reporterId, String name, String companyId, String matchedOn) {
    }

    /** The outcome of resolving a sender. */
    public record SenderMatchResult(String fromAddress, String matchType, String reporterId, String companyId,
                                    String reporterName, boolean isReportingMailbox, List<Candidate> candidates,
                                    String message) {

        public int candidateCount() {
            return candidates == null ? 0 : candidates.size();
        }
    }
}
