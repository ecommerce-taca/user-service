package com.ecommerce.authuser.user.application.profile;

public record UpdateMyProfileResult(
        GetMyProfileResult profile,
        boolean phoneVerificationRequired
) {
}
