package com.ecommerce.authuser.shop.application.profile.read;

import com.ecommerce.authuser.rbac.domain.RbacKeys;
import com.ecommerce.authuser.rbac.domain.UserRole;
import com.ecommerce.authuser.rbac.repository.UserRoleRepository;

import com.ecommerce.authuser.shop.domain.Shop;

import com.ecommerce.authuser.shop.exception.SellerPermissionDeniedException;
import com.ecommerce.authuser.shop.exception.ShopNotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetSellerShopService {

    private final UserRoleRepository userRoleRepository;

    @Transactional(readOnly = true)
    public GetSellerShopResult get(UUID userId) {
        if (userId == null) {
            throw new SellerPermissionDeniedException();
        }

        List<UserRole> assignments = userRoleRepository
                .findAllByUser_IdAndRevokedAtIsNull(userId);

        Shop shop = resolveAccessibleShop(
                assignments,
                userId
        );

        if (shop.getDeletedAt() != null) {
            throw new ShopNotFoundException();
        }

        return new GetSellerShopResult(
                shop.getId(),
                shop.getName(),
                shop.getSlug(),
                shop.getBusinessName(),
                maskTaxCode(shop.getTaxCode()),
                shop.getDescription(),
                null,
                shop.getStatus(),
                shop.getKycStatus(),
                mapWarehouseSummary(shop.getWarehouseSnapshot()),
                mapBankSummary(shop),
                shop.getCreatedAt(),
                shop.getUpdatedAt()
        );
    }

    private Shop resolveAccessibleShop(
            List<UserRole> assignments,
            UUID userId
    ) {

        List<Shop> ownedShops =
                assignments
                        .stream()
                        .filter(
                                assignment ->
                                        RbacKeys.Roles.SELLER.equals(
                                                assignment
                                                        .getRole()
                                                        .getRoleKey()
                                        )
                        )
                        .map(UserRole::getShop)
                        .filter(java.util.Objects::nonNull)
                        .filter(shop -> isOwnedBy(shop, userId))
                        .distinct()
                        .toList();

        Shop ownedShop = resolveSingleCandidate(ownedShops);

        if (ownedShop != null) {
            return ownedShop;
        }

        List<Shop> staffShops =
                assignments
                        .stream()
                        .filter(
                                assignment ->
                                        RbacKeys.Roles.SELLER_STAFF.equals(
                                                assignment
                                                        .getRole()
                                                        .getRoleKey()
                                        )
                        )
                        .map(UserRole::getShop)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList();

        Shop staffShop = resolveSingleCandidate(staffShops);

        if (staffShop != null) {
            return staffShop;
        }

        throw new SellerPermissionDeniedException();
    }

    private Shop resolveSingleCandidate(List<Shop> shops) {
        List<Shop> liveShops = shops
                .stream()
                .filter(shop -> shop.getDeletedAt() == null)
                .toList();

        if (liveShops.size() == 1) {
            return liveShops.getFirst();
        }

        if (liveShops.size() > 1) {
            throw new SellerPermissionDeniedException();
        }
        
        if (shops.size() == 1) {
            return shops.getFirst();
        }

        if (shops.size() > 1) {
            throw new SellerPermissionDeniedException();
        }

        return null;
    }

    private boolean isOwnedBy(
            Shop shop,
            UUID userId
    ) {

        return shop.getOwner() != null
                && shop.getOwner().getId() != null
                && shop.getOwner().getId().equals(userId);
    }

    private String maskTaxCode(String taxCode) {

        if (taxCode == null || taxCode.isBlank()) {
            return null;
        }

        String normalized = taxCode.strip();

        if (normalized.length() <= 4) {
            return "*".repeat(normalized.length());
        }

        return "*".repeat(
                normalized.length() - 4)
                + normalized.substring(
                normalized.length() - 4
        );
    }

    private GetSellerShopResult.WarehouseSummary mapWarehouseSummary(Map<String, Object> snapshot) {

        if (snapshot == null || snapshot.isEmpty()) {
            return null;
        }

        String warehouseName = stringValue(snapshot.get("warehouse_name"));

        String district = null;
        String province = null;

        Object addressValue = snapshot.get("address");

        if (addressValue instanceof Map<?, ?> address) {
            district = stringValue(address.get("district"));

            province = stringValue(address.get("province"));
        }

        return new GetSellerShopResult
                .WarehouseSummary(
                warehouseName,
                district,
                province
        );
    }

    private GetSellerShopResult.BankSummary mapBankSummary(Shop shop) {

        if (shop.getBankName() == null) {
            return null;
        }

        String maskedAccount = null;

        String last4 = shop.getBankAccountLast4();

        if (last4 != null && last4.matches("\\d{4}")) {

            maskedAccount = "********" + last4;
        }

        return new GetSellerShopResult.BankSummary(
                shop.getBankName(),
                maskedAccount,
                shop.getBankVerifiedAt() != null
        );
    }

    private String stringValue(Object value) {

        if (!(value instanceof String text)) {
            return null;
        }

        return text;
    }
}