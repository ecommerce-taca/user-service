package com.ecommerce.authuser.user.repository;

import com.ecommerce.authuser.user.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailNormalizedAndDeletedAtIsNull(String emailNormalized);

    Optional<User> findByPhoneNormalizedAndDeletedAtIsNull(String phoneNormalized);

    boolean existsByEmailNormalized(String emailNormalized);

    boolean existsByPhoneNormalized(String phoneNormalized);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select user
        from User user
        where user.id = :userId
            and user.deletedAt is null
        """)
    Optional<User> findByIdForUpdate(@Param("userId") UUID userId);

    @Query("""
        select u
        from User u
        where u.status = com.ecommerce.authuser.user.domain.UserStatus.LOCKED
            and u.lockedUntil is not null
            and u.lockedUntil <= :now
            and u.deletedAt is null
        order by u.lockedUntil asc
        """)
    List<User> findUnlockCandidates(
            @Param("now") Instant now,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select user
        from User user
        where user.emailNormalized = :emailNormalized
            and user.deletedAt is null
        """)
    Optional<User> findByEmailNormalizedForUpdate(
            @Param("emailNormalized") String emailNormalized
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select user
        from User user
        where user.phoneNormalized = :phoneNormalized
            and user.deletedAt is null
        """)
    Optional<User> findByPhoneNormalizedForUpdate(
            @Param("phoneNormalized") String phoneNormalized
    );
}
