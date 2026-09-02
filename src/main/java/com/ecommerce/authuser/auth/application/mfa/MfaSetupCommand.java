package com.ecommerce.authuser.auth.application.mfa;

import java.util.UUID;

public record MfaSetupCommand(
        UUID userId
) {
}
