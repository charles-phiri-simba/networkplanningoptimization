package com.simba.snip.npo.vendorcertification.policy;

import java.util.Locale;

/**
 * Canonical Phase 17 principal comparison. Identities are compared after
 * trim/strip and Locale.ROOT case-fold so Alice/alice cannot bypass SoD.
 */
public final class Phase17PrincipalCanon {

    private Phase17PrincipalCanon() {
    }

    public static String canonical(String principalId) {
        if (principalId == null) {
            return null;
        }
        String stripped = principalId.strip();
        if (stripped.isEmpty()) {
            return "";
        }
        return stripped.toLowerCase(Locale.ROOT);
    }

    public static boolean isBlank(String principalId) {
        return principalId == null || principalId.strip().isEmpty();
    }

    public static boolean samePrincipal(String left, String right) {
        String a = canonical(left);
        String b = canonical(right);
        return a != null && !a.isEmpty() && a.equals(b);
    }
}
