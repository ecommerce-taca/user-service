package com.ecommerce.authuser.auth.application.verification.phone;

import java.time.Instant;

public record PhoneOtpVerifyResult(
        Instant verifiedAt
) {
}