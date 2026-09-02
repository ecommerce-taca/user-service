package com.ecommerce.authuser.mfa.security;

import org.springframework.stereotype.Component;

@Component
public class Base32Encoder {

    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    public String encode(byte[] input) {
        if (input == null || input.length == 0) {
            throw new IllegalArgumentException(
                    "Input must not be empty"
            );
        }

        StringBuilder result = new StringBuilder();

        int buffer = 0;
        int bitsLeft = 0;

        for (byte value : input) {
            buffer = (buffer << 8) | (value & 0xFF);

            bitsLeft += 8;

            while (bitsLeft >= 5) {

                int index = (buffer >> (bitsLeft - 5)) & 0x1F;

                bitsLeft -= 5;

                result.append(ALPHABET[index]);
            }
        }

        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;

            result.append(ALPHABET[index]);
        }

        return result.toString();
    }
}
