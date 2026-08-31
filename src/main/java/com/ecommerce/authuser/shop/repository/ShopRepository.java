package com.ecommerce.authuser.shop.repository;

import com.ecommerce.authuser.shop.domain.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShopRepository extends JpaRepository<Shop, UUID> {

    Optional<Shop> findByIdAndDeletedAtIsNull(UUID shopId);

    Optional<Shop> findByOwner_IdAndDeletedAtIsNull(UUID ownerUserId);

    boolean existsByOwner_IdAndDeletedAtIsNull(UUID ownerUserId);

    boolean existsBySlug(String slug);

    boolean existsByTaxCode(String taxCode);
}
