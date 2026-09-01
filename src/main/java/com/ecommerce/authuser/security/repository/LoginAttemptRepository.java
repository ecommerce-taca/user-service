package com.ecommerce.authuser.security.repository;

import com.ecommerce.authuser.security.domain.LoginAttempt;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
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

    @Query("""
        select attempt
        from LoginAttempt attempt
        where attempt.occurredAt < :before
        order by attempt.occurredAt asc
        """)
    List<LoginAttempt> findArchiveCandidates(
            @Param("before") Instant before,
            Pageable pageable
    );
}
