package com.ecommerce.authuser.token.repository;

import java.util.UUID;

public record VerificationTokenLookup(
        UUID tokenId,
        UUID userId
) {
}
