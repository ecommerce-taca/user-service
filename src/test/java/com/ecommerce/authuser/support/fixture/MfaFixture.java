package com.ecommerce.authuser.support.fixture;

import com.ecommerce.authuser.mfa.domain.MfaChallenge;
import com.ecommerce.authuser.mfa.domain.MfaPurpose;
import com.ecommerce.authuser.mfa.repository.MfaChallengeRepository;
import com.ecommerce.authuser.support.builder.MfaChallengeTestBuilder;
import com.ecommerce.authuser.user.domain.User;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MfaFixture {

    private final MfaChallengeRepository repository;

    public MfaFixture(
            MfaChallengeRepository repository
    ) {
        this.repository = repository;
    }

    public MfaChallenge loginChallenge(
            User user
    ) {
        return save(
                MfaChallengeTestBuilder
                        .anMfaChallenge()
                        .forUser(user)
                        .withPurpose(MfaPurpose.LOGIN)
                        .build()
        );
    }

    public MfaChallenge challenge(
            User user,
            MfaPurpose purpose
    ) {
        return save(
                MfaChallengeTestBuilder
                        .anMfaChallenge()
                        .forUser(user)
                        .withPurpose(purpose)
                        .build()
        );
    }

    public MfaChallenge save(
            MfaChallenge challenge
    ) {
        return repository.save(challenge);
    }

    public void deleteAll() {
        repository.deleteAll();
    }

    public void revoke(
            MfaChallenge challenge,
            Instant revokedAt
    ) {
        challenge.revoke(revokedAt);
        repository.save(challenge);
    }
}
