package com.ecommerce.authuser.kyc.web.admin.review;

public record AdminKycReviewRequest(
        String decision,
        String reason
) {
}
