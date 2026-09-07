package com.ecommerce.authuser.support.security;

import java.util.UUID;

public record TestUserToken(
        String value,
        UUID userId
) {
}