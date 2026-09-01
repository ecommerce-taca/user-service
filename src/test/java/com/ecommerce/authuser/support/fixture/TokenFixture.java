package com.ecommerce.authuser.support.fixture;

import com.ecommerce.authuser.support.builder.PasswordResetTokenTestBuilder;
import com.ecommerce.authuser.support.builder.RefreshTokenTestBuilder;
import com.ecommerce.authuser.support.builder.VerificationTokenTestBuilder;
import com.ecommerce.authuser.token.domain.PasswordResetToken;
import com.ecommerce.authuser.token.domain.RefreshToken;
import com.ecommerce.authuser.token.domain.VerificationToken;
import com.ecommerce.authuser.token.repository.PasswordResetTokenRepository;
import com.ecommerce.authuser.token.repository.RefreshTokenRepository;
import com.ecommerce.authuser.token.repository.VerificationTokenRepository;
import com.ecommerce.authuser.user.domain.User;
import org.springframework.stereotype.Component;

@Component
public final class TokenFixture {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;

    public TokenFixture(
            PasswordResetTokenRepository passwordResetTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            VerificationTokenRepository verificationTokenRepository
    ) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.verificationTokenRepository = verificationTokenRepository;
    }

    public PasswordResetToken persistPasswordResetToken(
            User user
    ) {
        return persistPasswordResetToken(
                PasswordResetTokenTestBuilder
                        .aPasswordResetToken()
                        .forUser(user)
                        .build()
        );
    }

    public PasswordResetToken persistPasswordResetToken(
            PasswordResetToken token
    ) {
        return passwordResetTokenRepository.saveAndFlush(token);
    }

    public RefreshToken persistRefreshToken(
            User user
    ) {
        return persistRefreshToken(
                RefreshTokenTestBuilder
                        .aRefreshToken()
                        .forUser(user)
                        .build()
        );
    }

    public RefreshToken persistRefreshToken(
            RefreshToken token
    ) {
        return refreshTokenRepository.saveAndFlush(token);
    }

    public VerificationToken persistVerificationToken(
            User user
    ) {
        return persistVerificationToken(
                VerificationTokenTestBuilder
                        .aVerificationToken()
                        .forUser(user)
                        .build()
        );
    }

    public VerificationToken persistVerificationToken(
            VerificationToken token
    ) {
        return verificationTokenRepository.saveAndFlush(token);
    }
}
