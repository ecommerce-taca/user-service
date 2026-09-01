package com.ecommerce.authuser.support.base;

import com.ecommerce.authuser.rbac.repository.PermissionRepository;
import com.ecommerce.authuser.rbac.repository.RolePermissionRepository;
import com.ecommerce.authuser.rbac.repository.RoleRepository;
import com.ecommerce.authuser.rbac.repository.UserRoleRepository;
import com.ecommerce.authuser.user.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected PermissionRepository permissionRepository;

    @Autowired
    protected RolePermissionRepository rolePermissionRepository;

    @Autowired
    protected UserRoleRepository userRoleRepository;
}
