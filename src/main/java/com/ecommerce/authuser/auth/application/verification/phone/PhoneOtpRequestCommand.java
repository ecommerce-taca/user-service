package com.ecommerce.authuser.auth.application.verification.phone;

import java.util.UUID;

public record PhoneOtpRequestCommand(
        UUID userId,
        String phone
) {
}