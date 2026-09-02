package com.ecommerce.authuser.mfa.security;

public record TotpVerificationResult(
        boolean valid,
        Long matchedStep
) {

    public static TotpVerificationResult success(
            long matchedStep
    ) {
        return new TotpVerificationResult(
                true,
                matchedStep
        );
    }

    public static TotpVerificationResult invalid() {
        return new TotpVerificationResult(
                false,
                null
        );
    }
}
