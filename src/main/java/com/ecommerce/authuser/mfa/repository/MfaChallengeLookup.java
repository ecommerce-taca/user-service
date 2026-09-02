package com.ecommerce.authuser.mfa.repository;

import com.ecommerce.authuser.mfa.domain.MfaPurpose;

import java.util.UUID;

public record MfaChallengeLookup(
        UUID challengeId,
        UUID userId,
        MfaPurpose purpose
) {
}
