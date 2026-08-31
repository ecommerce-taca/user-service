package com.ecommerce.authuser.shop.repository;

import com.ecommerce.authuser.shop.domain.ShopStaff;
import com.ecommerce.authuser.shop.domain.ShopStaffStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShopStaffRepository extends JpaRepository<ShopStaff, UUID> {

    Optional<ShopStaff>
    findByShop_IdAndUser_Id(UUID shopId, UUID userId);

    List<ShopStaff> findAllByShop_IdAndStatus(
            UUID shopId,
            ShopStaffStatus status
    );

    List<ShopStaff> findAllByUser_IdAndStatus(
            UUID userId,
            ShopStaffStatus status
    );

    boolean
    existsByShop_IdAndUser_IdAndStatus(
            UUID shopId,
            UUID userId,
            ShopStaffStatus status
    );
}
