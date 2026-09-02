package com.ecommerce.authuser.auth.application;

import java.time.Instant;

public record PhoneOtpVerifyResult(
        Instant verifiedAt
) {
}