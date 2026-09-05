package com.ecommerce.authuser.favorite.repository;

import com.ecommerce.authuser.favorite.domain.UserFavorite;
import com.ecommerce.authuser.favorite.domain.UserFavoriteId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, UserFavoriteId> {

    Page<UserFavorite> findAllById_UserId(
            UUID userId,
            Pageable pageable
    );

    long countById_UserId(UUID userId);

    @Query("""
        select favorite.id.productId
        from UserFavorite favorite
        where favorite.id.userId = :userId
            and favorite.id.productId in :productIds
        """)
    List<UUID> findExistingProductIds(
            @Param("userId")
            UUID userId,

            @Param("productIds")
            Collection<UUID> productIds
    );
}