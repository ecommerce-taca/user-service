package com.ecommerce.authuser.shop.application.register;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;

import com.ecommerce.authuser.rbac.domain.RbacKeys;
import com.ecommerce.authuser.rbac.domain.Role;
import com.ecommerce.authuser.rbac.domain.UserRole;
import com.ecommerce.authuser.rbac.repository.RoleRepository;
import com.ecommerce.authuser.rbac.repository.UserRoleRepository;

import com.ecommerce.authuser.shop.domain.SellerOnboarding;
import com.ecommerce.authuser.shop.domain.Shop;
import com.ecommerce.authuser.shop.exception.InvalidSellerRegistrationException;
import com.ecommerce.authuser.shop.exception.SellerEmailNotVerifiedException;
import com.ecommerce.authuser.shop.exception.ShopAlreadyExistsException;
import com.ecommerce.authuser.shop.exception.ShopSlugAlreadyExistsException;
import com.ecommerce.authuser.shop.exception.TaxCodeAlreadyExistsException;
import com.ecommerce.authuser.shop.repository.SellerOnboardingRepository;
import com.ecommerce.authuser.shop.repository.ShopRepository;

import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.exception.profile.UserNotFoundException;
import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegisterSellerService {

    private static final short EVENT_SCHEMA_VERSION = 1;

    private final UserRepository userRepository;

    private final ShopRepository shopRepository;

    private final SellerOnboardingRepository sellerOnboardingRepository;

    private final RoleRepository roleRepository;

    private final UserRoleRepository userRoleRepository;

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public RegisterSellerResult register(RegisterSellerCommand command) {
        if (command == null || command.userId() == null) {
            throw new InvalidSellerRegistrationException();
        }

        User user = userRepository
                .findByIdForUpdate(command.userId())
                .orElseThrow(UserNotFoundException::new);

        if (user.getEmailVerifiedAt() == null) {
            throw new SellerEmailNotVerifiedException();
        }

        if (shopRepository.existsByOwner_IdAndDeletedAtIsNull(user.getId())) {
            throw new ShopAlreadyExistsException();
        }

        String name = requiredText(command.name(), 120);

        String businessName = requiredText(command.businessName(), 200);

        String taxCode = normalizeTaxCode(command.taxCode());

        String description =
                optionalText(
                        command.description(),
                        2000
                );

        if (shopRepository.existsByTaxCode(taxCode)) {
            throw new TaxCodeAlreadyExistsException();
        }

        String slug = resolveSlug(command.slug(), name);

        Shop shop =
                Shop.create(
                        user,
                        name,
                        slug,
                        businessName,
                        taxCode,
                        description
                );

        saveShop(shop);

        SellerOnboarding onboarding = SellerOnboarding.create(shop);

        sellerOnboardingRepository.save(onboarding);

        Role sellerRole = roleRepository
                .findByRoleKey(RbacKeys.Roles.SELLER)
                .orElseThrow(() -> new IllegalStateException(
                        "SELLER role is not configured"
                        )
                );


        UserRole sellerAssignment =
                UserRole.assign(
                        user,
                        sellerRole,
                        shop,
                        user.getId()
                );

        userRoleRepository.save(sellerAssignment);

        createShopCreatedEvent(shop, user);

        createSellerRoleChangedEvent(shop, user);

        return new RegisterSellerResult(
                new RegisterSellerResult.ShopResult(
                        shop.getId(),
                        shop.getName(),
                        shop.getSlug(),
                        shop.getStatus(),
                        shop.getKycStatus()
                ),
                new RegisterSellerResult.OnboardingResult(
                        onboarding.getCurrentStep(),
                        List.of(),
                        onboarding.getBlockers()
                )
        );
    }

    private void createShopCreatedEvent(
            Shop shop,
            User user
    ) {
        OutboxEvent event =
                OutboxEvent.create(
                        OutboxAggregateType.SHOP,
                        shop.getId(),
                        "shop.created",
                        EVENT_SCHEMA_VERSION,
                        shop.getId().toString(),
                        Map.of(
                                "shop_id",
                                shop.getId().toString(),

                                "owner_user_id",
                                user.getId().toString(),

                                "status",
                                shop.getStatus().name()
                        )
                );

        outboxEventRepository.save(event);
    }

    private void createSellerRoleChangedEvent(
            Shop shop,
            User user
    ) {

        OutboxEvent event =
                OutboxEvent.create(
                        OutboxAggregateType.USER,
                        user.getId(),
                        "user.role_changed",
                        EVENT_SCHEMA_VERSION,
                        user.getId().toString(),
                        Map.of(
                                "user_id",
                                user.getId().toString(),

                                "role",
                                RbacKeys.Roles.SELLER,

                                "shop_id",
                                shop.getId().toString(),

                                "action",
                                "GRANTED"
                        )
                );

        outboxEventRepository.save(event);
    }

    private String resolveSlug(
            String requestedSlug,
            String name
    ) {

        if (requestedSlug != null && !requestedSlug.isBlank()) {
            String slug = validateRequestedSlug(requestedSlug);

            if (shopRepository.existsBySlug(slug)) {
                throw new ShopSlugAlreadyExistsException();
            }

            return slug;
        }

        return generateUniqueSlug(name);
    }

    private String validateRequestedSlug(String value) {

        String slug = value
                .strip()
                .toLowerCase(Locale.ROOT);

        int length = slug.codePointCount(0, slug.length());

        if (length < 3
                || length > 160
                || !slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw new InvalidSellerRegistrationException();
        }

        return slug;
    }

    private String generateUniqueSlug(String name) {
        String base = normalizeGeneratedSlugBase(slugify(name), 160);

        if (!shopRepository.existsBySlug(base)) {
            return base;
        }

        String suffix =
                "-" + UuidV7Generator
                        .generate()
                        .toString()
                        .replace("-", "")
                        .substring(0, 8);

        int maxBaseLength = 160 - suffix.length();

        String shortenedBase =
                normalizeGeneratedSlugBase(base, maxBaseLength);

        return shortenedBase + suffix;
    }

    private String normalizeGeneratedSlugBase(
            String value,
            int maxLength
    ) {

        String normalized = value;

        if (normalized.length() > maxLength) {
            normalized = normalized.substring(0, maxLength);
        }

        normalized = normalized.replaceFirst("-+$", "");

        if (normalized.length() < 3) {
            return "shop";
        }

        return normalized;
    }

    private String slugify(String value) {
        String normalized =
                Normalizer.normalize(
                        value,
                        Normalizer.Form.NFD
                );

        normalized =
                normalized
                        .replace("Đ", "D")
                        .replace("đ", "d")
                        .replaceAll(
                                "\\p{M}+",
                                ""
                        )
                        .toLowerCase(Locale.ROOT)
                        .replaceAll(
                                "[^a-z0-9]+",
                                "-"
                        )
                        .replaceAll(
                                "^-+|-+$",
                                ""
                        );

        return normalized;
    }

    private String normalizeTaxCode(String value) {
        if (value == null) {
            throw new InvalidSellerRegistrationException();
        }

        String normalized = value.strip().replaceAll("[\\s.-]", "");

        if (!normalized.matches(
                "\\d{10,14}"
        )) {
            throw new InvalidSellerRegistrationException();
        }

        return normalized;
    }

    private String requiredText(
            String value,
            int maxLength
    ) {

        if (value == null) {
            throw new InvalidSellerRegistrationException();
        }

        String normalized = value.strip();

        int length = normalized.codePointCount(0, normalized.length());

        if (length < 1 || length > maxLength) {
            throw new InvalidSellerRegistrationException();
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
            throw new InvalidSellerRegistrationException();
        }

        return normalized;
    }

    private void saveShop(Shop shop) {
        try {
            shopRepository.saveAndFlush(shop);
        } catch (DataIntegrityViolationException ex) {
            if (containsConstraint(ex, "uk_shops_tax_code")) {
                throw new TaxCodeAlreadyExistsException();
            }

            if (containsConstraint(ex, "uk_shops_slug")) {
                throw new ShopSlugAlreadyExistsException();
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
}
