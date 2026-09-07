package com.ecommerce.authuser.shop.repository;

import com.ecommerce.authuser.shop.domain.SellerOnboarding;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SellerOnboardingRepository extends JpaRepository<SellerOnboarding, UUID> {

    Optional<SellerOnboarding> findByShop_Id(UUID shopId);

    Optional<SellerOnboarding> findByShop_Owner_IdAndShop_DeletedAtIsNull(UUID ownerUserId);

    boolean existsByShop_Id(UUID shopId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select onboarding
        from SellerOnboarding onboarding
        where onboarding.shop.id = :shopId
        """)
    Optional<SellerOnboarding> findByShopIdForUpdate(
            @Param("shopId") UUID shopId
    );
}
