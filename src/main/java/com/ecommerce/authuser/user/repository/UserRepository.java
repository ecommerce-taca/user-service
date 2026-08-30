package com.ecommerce.authuser.user.repository;

import com.ecommerce.authuser.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailNormalizedAndDeletedAtIsNull(String emailNormalized);

    Optional<User> findByPhoneNormalizedAndDeletedAtIsNull(String phoneNormalized);

    boolean existsByEmailNormalized(String emailNormalized);

    boolean existsByPhoneNormalized(String phoneNormalized);
}
