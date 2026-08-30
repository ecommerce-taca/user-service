package com.ecommerce.authuser.common.id;

import java.security.SecureRandom;
import java.util.UUID;

public final class UuidV7Generator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7Generator() {
    }

    public static UUID generate() {
        long timestamp = System.currentTimeMillis() & 0xFFFFFFFFFFFFL;

        long randomA = RANDOM.nextLong() & 0x0FFFL;

        long mostSignificantBits = (timestamp << 16 ) | 0x7000l | randomA;

        long randomB = RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL;

        long leastSignificantBits = 0x8000000000000000L | randomB;

        return new UUID(
                mostSignificantBits,
                leastSignificantBits
        );
    }
}
