package com.ecommerce.authuser.shop.port;

public interface BankAccountCryptoPort {

    EncryptionResult encrypt(
            String plaintext
    );

    record EncryptionResult(
            byte[] ciphertext,
            String keyVersion
    ) {

        public EncryptionResult {

            if (ciphertext == null || ciphertext.length == 0) {
                throw new IllegalArgumentException(
                        "ciphertext must not be empty"
                );
            }

            if (keyVersion == null || keyVersion.isBlank()) {
                throw new IllegalArgumentException(
                        "keyVersion must not be blank"
                );
            }

            ciphertext = ciphertext.clone();
        }

        @Override
        public byte[] ciphertext() {
            return ciphertext.clone();
        }
    }
}
