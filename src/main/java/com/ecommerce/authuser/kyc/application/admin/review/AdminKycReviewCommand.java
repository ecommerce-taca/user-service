package com.ecommerce.authuser.kyc.application.admin.review;

import java.util.UUID;

public record AdminKycReviewCommand(
        UUID actorUserId,
        UUID sessionId,
        UUID shopId,
        String decision,
        String reason,
        String stepUpToken,
        String clientIp
) {
}