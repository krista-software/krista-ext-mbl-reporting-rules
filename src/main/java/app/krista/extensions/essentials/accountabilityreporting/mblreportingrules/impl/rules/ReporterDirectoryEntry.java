package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * One row of the Reporter Directory supplied to Match Sender.
 *
 * <p>Krista passes the directory as a list of inline objects, so each row arrives as a
 * {@code Map<String, Object>} keyed by the field display names. {@code Emails} and {@code Aliases}
 * hold a comma- or semicolon-separated list of addresses; a real list is also accepted, so a caller
 * that already has one does not have to flatten it first.
 */
public final class ReporterDirectoryEntry {

    public static final String F_ID = "Id";
    public static final String F_NAME = "Name";
    public static final String F_EMAILS = "Emails";
    public static final String F_ALIASES = "Aliases";
    public static final String F_COMPANY_ID = "Company Id";
    public static final String F_ACTIVE = "Active";

    private final String id;
    private final String name;
    private final Set<String> emails;
    private final Set<String> aliases;
    private final String companyId;
    private final boolean active;

    public ReporterDirectoryEntry(String id, String name, Collection<String> emails,
                                  Collection<String> aliases, String companyId, boolean active) {
        this.id = trimToEmpty(id);
        this.name = trimToEmpty(name);
        this.emails = normalizeAll(emails);
        this.aliases = normalizeAll(aliases);
        this.companyId = trimToEmpty(companyId);
        this.active = active;
    }

    /** Build the directory from the raw composite input. Rows that are not objects are skipped. */
    public static List<ReporterDirectoryEntry> fromInput(List<Map<String, Object>> rows) {
        List<ReporterDirectoryEntry> out = new ArrayList<>();
        if (rows == null) {
            return out;
        }
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            out.add(new ReporterDirectoryEntry(
                    str(row.get(F_ID)),
                    str(row.get(F_NAME)),
                    splitAddresses(row.get(F_EMAILS)),
                    splitAddresses(row.get(F_ALIASES)),
                    str(row.get(F_COMPANY_ID)),
                    bool(row.get(F_ACTIVE))));
        }
        return out;
    }

    /** Split a comma/semicolon-separated address list, or flatten an already-structured list. */
    public static List<String> splitAddresses(Object value) {
        List<String> out = new ArrayList<>();
        if (value == null) {
            return out;
        }
        if (value instanceof Collection<?> collection) {
            for (Object element : collection) {
                if (element != null) {
                    out.addAll(splitAddresses(element));
                }
            }
            return out;
        }
        for (String part : value.toString().split("[,;]")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    /**
     * Canonical comparison form of an email address: lower-cased, trimmed, and unwrapped from a
     * {@code Display Name <user@example.com>} header form.
     */
    public static String normalizeAddress(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        int open = value.lastIndexOf('<');
        int close = value.lastIndexOf('>');
        if (open >= 0 && close > open) {
            value = value.substring(open + 1, close).trim();
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static Set<String> normalizeAll(Collection<String> values) {
        Set<String> out = new LinkedHashSet<>();
        if (values != null) {
            for (String v : values) {
                String n = normalizeAddress(v);
                if (!n.isEmpty()) {
                    out.add(n);
                }
            }
        }
        return Collections.unmodifiableSet(out);
    }

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }

    private static boolean bool(Object value) {
        if (value == null) {
            // Absent means active: a directory row with no Active flag is treated as a live reporter.
            return true;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        String s = value.toString().trim();
        return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes") || s.equals("1");
    }

    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    public boolean hasEmail(String normalizedAddress) {
        return emails.contains(normalizedAddress);
    }

    public boolean hasAlias(String normalizedAddress) {
        return aliases.contains(normalizedAddress);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<String> getEmails() {
        return emails;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public String getCompanyId() {
        return companyId;
    }

    public boolean isActive() {
        return active;
    }
}
