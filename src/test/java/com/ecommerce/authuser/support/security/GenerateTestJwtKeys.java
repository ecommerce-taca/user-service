package com.ecommerce.authuser.support.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class GenerateTestJwtKeys {

    public static void main(String[] args) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);

        KeyPair keyPair = generator.generateKeyPair();

        String privateKeyBase64 = Base64.getEncoder()
                .encodeToString(keyPair.getPrivate().getEncoded());

        String publicKeyBase64 = Base64.getEncoder()
                .encodeToString(keyPair.getPublic().getEncoded());

        System.out.println("=== PRIVATE KEY BASE64 ===");
        System.out.println(privateKeyBase64);

        System.out.println();
        System.out.println("=== PUBLIC KEY BASE64 ===");
        System.out.println(publicKeyBase64);
    }
}
