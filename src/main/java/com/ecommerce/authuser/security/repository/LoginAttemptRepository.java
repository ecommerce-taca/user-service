package com.ecommerce.authuser.security.repository;

import com.ecommerce.authuser.security.domain.LoginAttempt;

import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.UUID;

public interface LoginAttemptRepository extends Repository<LoginAttempt, Long> {

    LoginAttempt save(LoginAttempt attempt);

    long countByIdentifierHashAndSucceededFalseAndOccurredAtAfter(
            String identifierHash,
            Instant occurredAfter
    );

    long countByUser_IdAndSucceededFalseAndOccurredAtAfter(
            UUID userId,
            Instant occurredAfter
    );
}
