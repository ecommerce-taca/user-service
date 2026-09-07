package com.ecommerce.authuser.token.repository;

import java.util.UUID;

public record RefreshTokenLookup(
        UUID tokenId,
        UUID userId,
        UUID familyId
) {
}
