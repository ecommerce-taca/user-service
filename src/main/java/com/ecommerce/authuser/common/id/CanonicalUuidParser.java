package com.ecommerce.authuser.common.id;

import java.util.UUID;
import java.util.function.Supplier;

public final class CanonicalUuidParser {

    private CanonicalUuidParser() {
    }

    public static UUID parse(
            String value,
            Supplier<? extends RuntimeException> invalidException
    ) {
        if (value == null || value.isBlank()) {
            throw invalidException.get();
        }

        String normalized = value.strip();

        UUID uuid;

        try {
            uuid = UUID.fromString(normalized);

        } catch (IllegalArgumentException ex) {
            throw invalidException.get();
        }

        if (!uuid.toString().equalsIgnoreCase(normalized)) {
            throw invalidException.get();
        }

        return uuid;
    }
}