package com.ecommerce.authuser.auth.application.mfa;

import com.ecommerce.authuser.mfa.domain.MfaMethod;

import java.util.UUID;

public record MfaStepUpVerifyCommand(
        UUID userId,
        UUID sessionId,
        UUID challengeId,
        MfaMethod method,
        String code
) {
}
