package com.ecommerce.authuser.user.application.admin.status;

import java.time.Instant;
import java.util.UUID;

public interface RevokedUserCache {

    void markRevoked(
            UUID userId,
            Instant suspendedAt
    );
}
