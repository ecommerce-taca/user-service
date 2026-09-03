package com.ecommerce.authuser.kyc.application.submit;

import java.util.UUID;

public record SubmitKycCommand(
        UUID userId
) {
}