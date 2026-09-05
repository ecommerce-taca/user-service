package com.ecommerce.authuser.kyc.application.admin.detail;

import java.util.UUID;

public record AdminKycDetailQuery(
        UUID actorUserId,
        UUID shopId
) {
}
