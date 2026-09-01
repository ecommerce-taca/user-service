package com.ecommerce.authuser.shop.repository;

import com.ecommerce.authuser.shop.domain.Shop;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ShopRepository extends JpaRepository<Shop, UUID> {

    Optional<Shop> findByIdAndDeletedAtIsNull(UUID shopId);

    Optional<Shop> findByOwner_IdAndDeletedAtIsNull(UUID ownerUserId);

    boolean existsByOwner_IdAndDeletedAtIsNull(UUID ownerUserId);

    boolean existsBySlug(String slug);

    boolean existsByTaxCode(String taxCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select shop
        from Shop shop
        where shop.id = :shopId
        and shop.deletedAt is null
        """)
    Optional<Shop> findByIdForUpdate(@Param("shopId") UUID shopId);
}
