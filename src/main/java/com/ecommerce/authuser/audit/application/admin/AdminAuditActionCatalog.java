package com.ecommerce.authuser.audit.application.admin;

import java.util.Set;

public final class AdminAuditActionCatalog {

    public static final String KYC_REVIEW = "KYC_REVIEW";

    public static final String ROLE_GRANTED = "ROLE_GRANTED";

    public static final String ROLE_REVOKED = "ROLE_REVOKED";

    public static final String USER_SUSPENDED = "USER_SUSPENDED";

    public static final String USER_RESTORED = "USER_RESTORED";

    private static final Set<String> ALLOWED =
            Set.of(
                    KYC_REVIEW,
                    ROLE_GRANTED,
                    ROLE_REVOKED,
                    USER_SUSPENDED,
                    USER_RESTORED
            );

    private AdminAuditActionCatalog() {
    }

    public static boolean contains(String action) {
        return action != null && ALLOWED.contains(action);
    }

    public static Set<String> values() {
        return ALLOWED;
    }
}
