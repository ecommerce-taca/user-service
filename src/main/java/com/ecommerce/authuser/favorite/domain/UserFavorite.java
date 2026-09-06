package com.ecommerce.authuser.favorite.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "user_favorites")
public class UserFavorite {

    @EmbeddedId
    private UserFavoriteId id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserFavorite() {
    }

    public static UserFavorite create(
            UUID userId,
            UUID productId,
            Instant createdAt
    ) {

        UserFavorite favorite = new UserFavorite();

        favorite.id = new UserFavoriteId(userId, productId);

        favorite.createdAt =
                Objects.requireNonNull(
                        createdAt,
                        "createdAt must not be null"
                );

        return favorite;
    }

    public UUID getUserId() {
        return id.getUserId();
    }

    public UUID getProductId() {
        return id.getProductId();
    }
}