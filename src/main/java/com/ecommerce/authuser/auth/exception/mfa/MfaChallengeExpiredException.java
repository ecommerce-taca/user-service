package com.ecommerce.authuser.auth.exception.mfa;

public class MfaChallengeExpiredException extends RuntimeException {

    public MfaChallengeExpiredException() {
        super("MFA challenge expired");
    }
}
