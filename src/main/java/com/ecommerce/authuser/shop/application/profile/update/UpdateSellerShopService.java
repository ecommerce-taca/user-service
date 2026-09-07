package com.ecommerce.authuser.shop.application.profile.update;

import com.ecommerce.authuser.rbac.domain.RbacKeys;
import com.ecommerce.authuser.rbac.repository.UserRoleRepository;

import com.ecommerce.authuser.shop.domain.Shop;

import com.ecommerce.authuser.shop.exception.InvalidSellerShopProfileException;
import com.ecommerce.authuser.shop.exception.SellerPermissionDeniedException;
import com.ecommerce.authuser.shop.exception.ShopInvalidStateException;
import com.ecommerce.authuser.shop.exception.ShopNotFoundException;

import com.ecommerce.authuser.shop.repository.ShopRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateSellerShopService {

    private final ShopRepository shopRepository;

    private final UserRoleRepository userRoleRepository;

    @Transactional
    public UpdateSellerShopResult update(UpdateSellerShopCommand command) {

        if (command == null || command.userId() == null) {
            throw new InvalidSellerShopProfileException();
        }

        if (!command.nameProvided()
                && !command.descriptionProvided()
                && !command.logoObjectKeyProvided()) {
            throw new InvalidSellerShopProfileException();
        }

        String name =
                command.nameProvided()
                        ? requiredText(command.name(), 120)
                        : null;

        String description =
                command.descriptionProvided()
                        ? optionalText(command.description(), 2000)
                        : null;

        String logoObjectKey = validateLogoObjectKey(
                command.logoObjectKeyProvided(),
                command.logoObjectKey()
        );

        boolean hasSellerRole = userRoleRepository
                .existsByUser_IdAndRole_RoleKeyAndRevokedAtIsNull(
                        command.userId(),
                        RbacKeys.Roles.SELLER
                );

        if (!hasSellerRole) {
            throw new SellerPermissionDeniedException();
        }

        Shop shop = shopRepository
                .findByOwnerIdForUpdate(command.userId())
                .orElseThrow(ShopNotFoundException::new);

        boolean sellerOwner = userRoleRepository
                .existsByUser_IdAndRole_RoleKeyAndShop_IdAndRevokedAtIsNull(
                        command.userId(),
                        RbacKeys.Roles.SELLER,
                        shop.getId()
                );

        if (!sellerOwner) {
            throw new SellerPermissionDeniedException();
        }

        if (!shop.canUpdateSellerProfile()) {
            throw new ShopInvalidStateException();
        }

        try {
            shop.updateSellerProfile(
                    command.nameProvided(),
                    name,
                    command.descriptionProvided(),
                    description,
                    command.logoObjectKeyProvided(),
                    logoObjectKey
            );

        } catch (IllegalArgumentException ex) {
            throw new InvalidSellerShopProfileException();

        } catch (IllegalStateException ex) {
            throw new ShopInvalidStateException();
        }

        shopRepository.saveAndFlush(shop);

        return new UpdateSellerShopResult(
                shop.getId(),
                shop.getName(),
                shop.getSlug(),
                shop.getBusinessName(),
                shop.getDescription(),
                null,
                shop.getStatus(),
                shop.getKycStatus(),
                shop.getUpdatedAt()
        );
    }

    private String requiredText(
            String value,
            int maxLength
    ) {

        if (value == null) {
            throw new InvalidSellerShopProfileException();
        }

        String normalized = value.strip();

        int length = normalized.codePointCount(0, normalized.length());

        if (length < 1 || length > maxLength) {
            throw new InvalidSellerShopProfileException();
        }

        return normalized;
    }

    private String optionalText(
            String value,
            int maxLength
    ) {

        if (value == null) {
            return null;
        }

        String normalized = value.strip();

        if (normalized.isEmpty()) {
            return null;
        }

        int length = normalized.codePointCount(0, normalized.length());

        if (length > maxLength) {
            throw new InvalidSellerShopProfileException();
        }

        return normalized;
    }

    private String validateLogoObjectKey(
            boolean provided,
            String value
    ) {

        if (!provided) {
            return null;
        }

        if (value == null || value.isBlank()) {
            return null;
        }

        throw new InvalidSellerShopProfileException();
    }
}
