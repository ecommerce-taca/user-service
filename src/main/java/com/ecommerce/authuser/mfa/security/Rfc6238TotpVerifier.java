package com.ecommerce.authuser.mfa.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import java.time.Instant;

import java.util.Locale;

@Component
public class Rfc6238TotpVerifier implements TotpVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA1";

    private static final long TIME_STEP_SECONDS = 30L;

    private static final int DIGITS = 6;

    private static final int WINDOW = 1;

    @Override
    public TotpVerificationResult verifyWithStep(
            byte[] secret,
            String code,
            Instant now
    ) {

        if (secret == null
                || secret.length == 0
                || code == null
                || !code.matches("^\\d{6}$")
                || now == null) {

            return TotpVerificationResult.invalid();
        }

        long counter =
                Math.floorDiv(
                        now.getEpochSecond(),
                        TIME_STEP_SECONDS
                );

        for (int offset = -WINDOW;
             offset <= WINDOW;
             offset++) {

            long candidateCounter =
                    counter + offset;

            if (candidateCounter < 0) {
                continue;
            }

            String expected =
                    generateCode(
                            secret,
                            candidateCounter
                    );

            if (constantTimeEquals(
                    expected,
                    code
            )) {
                return TotpVerificationResult.success(
                        candidateCounter
                );
            }
        }

        return TotpVerificationResult.invalid();
    }

    private String generateCode(
            byte[] secret,
            long counter
    ) {

        try {

            byte[] counterBytes =
                    ByteBuffer
                            .allocate(Long.BYTES)
                            .putLong(counter)
                            .array();

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);

            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));

            byte[] hash = mac.doFinal(counterBytes);

            int offset = hash[hash.length - 1] & 0x0F;

            int binary =
                    ((hash[offset] & 0x7F) << 24)
                            | ((hash[offset + 1] & 0xFF) << 16)
                            | ((hash[offset + 2] & 0xFF) << 8)
                            | (hash[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, DIGITS);

            return String.format(
                    Locale.ROOT,
                    "%06d",
                    otp
            );

        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(
                    "Cannot calculate TOTP",
                    ex
            );
        }
    }

    private boolean constantTimeEquals(
            String expected,
            String actual
    ) {

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII)
        );
    }
}
