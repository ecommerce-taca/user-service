package com.ecommerce.authuser.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class HmacSha256OtpHasher implements OtpHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] key;

    public HmacSha256OtpHasher(
            @Value("${auth.otp.hash-key-base64}") String keyBase64
    ) {

        this.key = Base64.getDecoder().decode(keyBase64);

        if (key.length < 32) {
            throw new IllegalArgumentException(
                    "OTP hash key must be at least 32 bytes"
            );
        }
    }

    @Override
    public String hash(String rawOtp) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);

            mac.init(new SecretKeySpec(key, ALGORITHM));

            byte[] result = mac.doFinal(
                    rawOtp.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat
                    .of()
                    .formatHex(result);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Cannot hash OTP",
                    ex
            );
        }
    }
}
