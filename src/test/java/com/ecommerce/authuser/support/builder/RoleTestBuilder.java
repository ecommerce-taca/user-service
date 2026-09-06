package com.ecommerce.authuser.support.builder;

import com.ecommerce.authuser.rbac.domain.Role;
import com.ecommerce.authuser.rbac.domain.ScopeType;

public final class RoleTestBuilder {

    private String roleKey = "BUYER";
    private ScopeType scopeType = ScopeType.USER;
    private String description = "Test buyer role";
    private boolean system = true;

    private RoleTestBuilder() {
    }

    public static RoleTestBuilder aRole() {
        return new RoleTestBuilder();
    }

    public static RoleTestBuilder buyer() {
        return new RoleTestBuilder()
                .withRoleKey("BUYER")
                .withScopeType(ScopeType.USER)
                .withDescription("Buyer role")
                .asSystemRole();
    }

    public static RoleTestBuilder userRole(
            String roleKey
    ) {
        return new RoleTestBuilder()
                .withRoleKey(roleKey)
                .withScopeType(ScopeType.USER)
                .withDescription(
                        "Test user role: " + roleKey
                );
    }

    public RoleTestBuilder withRoleKey(String roleKey) {
        this.roleKey = roleKey;
        return this;
    }

    public RoleTestBuilder withScopeType(ScopeType scopeType) {
        this.scopeType = scopeType;
        return this;
    }

    public RoleTestBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public RoleTestBuilder asSystemRole() {
        this.system = true;
        return this;
    }

    public RoleTestBuilder asNonSystemRole() {
        this.system = false;
        return this;
    }

    public Role build() {
        return Role.create(
                roleKey,
                scopeType,
                description,
                system
        );
    }
}
