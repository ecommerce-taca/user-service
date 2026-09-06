package com.ecommerce.authuser.mfa.security;

import java.util.UUID;

public interface TotpSecretProtector {

    byte[] encrypt(UUID userId, byte[] rawSecret);

    byte[] decrypt(
            UUID userId,
            byte[] encryptedSecret,
            String keyVersion
    );

    String keyVersion();
}
