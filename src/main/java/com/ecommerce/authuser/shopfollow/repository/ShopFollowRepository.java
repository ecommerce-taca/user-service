package com.ecommerce.authuser.shopfollow.repository;

import com.ecommerce.authuser.shopfollow.domain.ShopFollow;
import com.ecommerce.authuser.shopfollow.domain.ShopFollowId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ShopFollowRepository extends JpaRepository<ShopFollow, ShopFollowId> {

    long countById_UserId(UUID userId);

    long countById_ShopId(UUID shopId);

    Page<ShopFollow> findAllById_UserId(
            UUID userId,
            Pageable pageable
    );
}
