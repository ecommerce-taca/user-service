package com.ecommerce.authuser.auth.application;

import java.util.UUID;

public record PhoneOtpRequestCommand(
        UUID userId,
        String phone
) {
}