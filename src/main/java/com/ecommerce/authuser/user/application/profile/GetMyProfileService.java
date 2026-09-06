package com.ecommerce.authuser.user.application.profile;

import com.ecommerce.authuser.rbac.domain.UserRole;
import com.ecommerce.authuser.rbac.repository.UserRoleRepository;

import com.ecommerce.authuser.shop.repository.ShopRepository;

import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.exception.profile.UserNotFoundException;
import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetMyProfileService {

    private final UserRepository userRepository;

    private final UserRoleRepository userRoleRepository;

    private final ShopRepository shopRepository;

    @Transactional(readOnly = true)
    public GetMyProfileResult get(UUID userId) {

        User user = userRepository
                .findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(UserNotFoundException::new);

        List<String> roles = userRoleRepository
                .findAllByUser_IdAndRevokedAtIsNull(user.getId())
                .stream()
                .map(UserRole::getRole)
                .map(role -> role.getRoleKey())
                .distinct()
                .sorted()
                .toList();

        UUID defaultShopId = shopRepository
                .findFirstByOwner_IdAndDeletedAtIsNullOrderByCreatedAtDesc(user.getId())
                .map(shop -> shop.getId())
                .orElse(null);

        return new GetMyProfileResult(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getEmailVerifiedAt() != null,
                user.getPhone(),
                user.getPhoneVerifiedAt() != null,
                user.getDateOfBirth(),
                roles,
                user.getStatus().name(),
                defaultShopId,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
