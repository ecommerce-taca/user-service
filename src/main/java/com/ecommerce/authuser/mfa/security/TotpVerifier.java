package com.ecommerce.authuser.mfa.security;

import java.time.Instant;

public interface TotpVerifier {

    TotpVerificationResult verifyWithStep(
            byte[] secret,
            String code,
            Instant now
    );

    default boolean verify(
            byte[] secret,
            String code,
            Instant now
    ) {
        return verifyWithStep(
                secret,
                code,
                now
        ).valid();
    }
}
