package com.ecommerce.authuser.shop.application.onboarding.profile;

import com.ecommerce.authuser.rbac.domain.RbacKeys;
import com.ecommerce.authuser.rbac.repository.UserRoleRepository;

import com.ecommerce.authuser.shop.domain.SellerOnboarding;
import com.ecommerce.authuser.shop.domain.Shop;

import com.ecommerce.authuser.shop.exception.InvalidSellerProfileException;
import com.ecommerce.authuser.shop.exception.SellerPermissionDeniedException;
import com.ecommerce.authuser.shop.exception.ShopInvalidStateException;
import com.ecommerce.authuser.shop.exception.ShopNotFoundException;
import com.ecommerce.authuser.shop.exception.TaxCodeAlreadyExistsException;

import com.ecommerce.authuser.shop.repository.SellerOnboardingRepository;
import com.ecommerce.authuser.shop.repository.ShopRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateSellerProfileService {

    private final ShopRepository shopRepository;

    private final SellerOnboardingRepository sellerOnboardingRepository;

    private final UserRoleRepository userRoleRepository;

    @Transactional
    public UpdateSellerProfileResult update(UpdateSellerProfileCommand command) {

        if (command == null || command.userId() == null) {
            throw new InvalidSellerProfileException();
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

        if (!shop.canEditOnboarding()) {
            throw new ShopInvalidStateException();
        }

        SellerOnboarding onboarding = sellerOnboardingRepository
                .findByShopIdForUpdate(shop.getId())
                .orElseThrow(ShopNotFoundException::new);

        String name = requiredText(command.name(), 120);

        String businessName = requiredText(command.businessName(), 200);

        String taxCode = normalizeTaxCode(command.taxCode());

        String description = optionalText(command.description(), 2000);

        String logoObjectKey = validateLogoObjectKey(command.logoObjectKey());

        if (shopRepository.existsByTaxCodeAndIdNot(taxCode, shop.getId())) {
            throw new TaxCodeAlreadyExistsException();
        }

        shop.updateOnboardingProfile(
                name,
                businessName,
                taxCode,
                description,
                logoObjectKey
        );

        onboarding.completeProfileStep();

        flushShop(shop);

        return new UpdateSellerProfileResult(
                shop.getId(),
                shop.getName(),
                shop.getBusinessName(),
                shop.getTaxCode(),
                shop.getDescription(),
                shop.getLogoObjectKey(),
                onboarding.getCurrentStep(),
                onboarding.isProfileCompleted(),
                onboarding.getBlockers()
        );
    }

    private void flushShop(Shop shop) {
        try {
            shopRepository.saveAndFlush(shop);

        } catch (DataIntegrityViolationException ex) {

            if (containsConstraint(ex, "uk_shops_tax_code")) {
                throw new TaxCodeAlreadyExistsException();
            }

            throw ex;
        }
    }

    private boolean containsConstraint(
            Throwable throwable,
            String constraintName
    ) {

        Throwable current = throwable;

        while (current != null) {

            String message = current.getMessage();

            if (message != null && message.contains(constraintName)) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private String requiredText(
            String value,
            int maxLength
    ) {

        if (value == null) {
            throw new InvalidSellerProfileException();
        }

        String normalized = value.strip();

        int length = normalized.codePointCount(0, normalized.length());

        if (length < 1 || length > maxLength) {
            throw new InvalidSellerProfileException();
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
            throw new InvalidSellerProfileException();
        }

        return normalized;
    }

    private String normalizeTaxCode(String value) {
        if (value == null) {
            throw new InvalidSellerProfileException();
        }

        String normalized = value
                .strip()
                .replaceAll("[\\s.-]", "");

        if (!normalized.matches("\\d{10,14}")) {
            throw new InvalidSellerProfileException();
        }

        return normalized;
    }

    private String validateLogoObjectKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        throw new InvalidSellerProfileException();
    }
}