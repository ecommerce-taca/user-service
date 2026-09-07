package com.ecommerce.authuser.support.builder;

import com.ecommerce.authuser.mfa.domain.TwoFactorCredential;
import com.ecommerce.authuser.support.testdata.MfaTestData;
import com.ecommerce.authuser.user.domain.User;

import java.util.Arrays;

public final class TwoFactorCredentialTestBuilder {

    private User user;

    private byte[] secretCiphertext =
            Arrays.copyOf(
                    MfaTestData.ENCRYPTED_SECRET,
                    MfaTestData.ENCRYPTED_SECRET.length
            );

    private String keyVersion =
            MfaTestData.KEY_VERSION;

    private TwoFactorCredentialTestBuilder() {
    }

    public static TwoFactorCredentialTestBuilder
    aTwoFactorCredential() {
        return new TwoFactorCredentialTestBuilder();
    }

    public TwoFactorCredentialTestBuilder forUser(User user) {
        this.user = user;
        return this;
    }

    public TwoFactorCredentialTestBuilder withSecretCiphertext(
            byte[] secretCiphertext
    ) {
        this.secretCiphertext =
                Arrays.copyOf(
                        secretCiphertext,
                        secretCiphertext.length
                );
        return this;
    }

    public TwoFactorCredentialTestBuilder withKeyVersion(
            String keyVersion
    ) {
        this.keyVersion = keyVersion;
        return this;
    }

    public TwoFactorCredential build() {
        return TwoFactorCredential.createEnrollment(
                user,
                secretCiphertext,
                keyVersion
        );
    }
}