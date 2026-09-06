package com.ecommerce.authuser.auth.application.support;

import java.time.Duration;

public final class AuthTokenPolicy {

    public static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    public static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    private AuthTokenPolicy() {
    }
}