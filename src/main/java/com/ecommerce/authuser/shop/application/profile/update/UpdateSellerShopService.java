package com.ecommerce.authuser.shop.application.profile.update;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;
import com.ecommerce.authuser.outbox.security.OutboxPayloadProtector;
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

import java.util.*;

@Service
@RequiredArgsConstructor
public class UpdateSellerShopService {

    private final ShopRepository shopRepository;

    private final UserRoleRepository userRoleRepository;

    private final OutboxEventRepository outboxEventRepository;

    private final OutboxPayloadProtector outboxPayloadProtector;

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

        List<String> changedFields = new ArrayList<>();

        if (command.nameProvided()
                && !Objects.equals(shop.getName(), name)) {
            changedFields.add("name");
        }

        if (command.descriptionProvided()
                && !Objects.equals(shop.getDescription(), description)) {
            changedFields.add("description");
        }

        if (command.logoObjectKeyProvided()
                && !Objects.equals(shop.getLogoObjectKey(), logoObjectKey)) {
            changedFields.add("logo_object_key");
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

        if (!changedFields.isEmpty()) {
            createShopUpdatedEvent(
                    command.userId(),
                    shop,
                    changedFields
            );
        }

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

    private void createShopUpdatedEvent(
            UUID actorUserId,
            Shop shop,
            List<String> changedFields
    ) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (changedFields.contains("name")) {
            snapshot.put("name", shop.getName());
        }

        if (changedFields.contains("description")) {
            snapshot.put("description", shop.getDescription());
        }

        if (changedFields.contains("logo_object_key")) {
            snapshot.put("logo_object_key", shop.getLogoObjectKey());
        }

        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("shop_id", shop.getId().toString());
        payload.put("changed_fields", List.copyOf(changedFields));
        payload.put("snapshot", snapshot);
        payload.put("updated_at", shop.getUpdatedAt().toString());
        payload.put("version", 1);

        OutboxEvent event =
                OutboxEvent.createWithActor(
                        OutboxAggregateType.SHOP,
                        shop.getId(),
                        actorUserId,
                        "shop.updated",
                        (short) 1,
                        shop.getId().toString(),
                        outboxPayloadProtector.protect(
                                "shop.updated",
                                payload
                        )
                );

        outboxEventRepository.save(event);
    }
}
