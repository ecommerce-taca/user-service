package com.ecommerce.authuser.token.repository;

import java.util.UUID;

public record PasswordResetTokenLookup(
        UUID tokenId,
        UUID userId
) {
}