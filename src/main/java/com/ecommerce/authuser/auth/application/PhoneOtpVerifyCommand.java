package com.ecommerce.authuser.auth.application;

import java.util.UUID;

public record PhoneOtpVerifyCommand(
        UUID userId,
        UUID challengeId,
        String otp
) {
}
