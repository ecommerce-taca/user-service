package com.ecommerce.authuser.rbac.repository;

import com.ecommerce.authuser.rbac.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByRoleKey(String roleKey);

    boolean existsByRoleKey(String roleKey);
}
