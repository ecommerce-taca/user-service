package com.ecommerce.authuser.shop.repository;

import com.ecommerce.authuser.shop.domain.SellerOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SellerOnboardingRepository extends JpaRepository<SellerOnboarding, UUID> {

    Optional<SellerOnboarding> findByShop_Id(UUID shopId);

    Optional<SellerOnboarding> findByShop_Owner_IdAndShop_DeletedAtIsNull(UUID ownerUserId);

    boolean existsByShop_Id(UUID shopId);
}
