package com.ecommerce.authuser.auth.application.verification.phone;

import java.util.UUID;

public record PhoneOtpVerifyCommand(
        UUID userId,
        UUID challengeId,
        String otp
) {
}
