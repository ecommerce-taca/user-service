package com.ecommerce.authuser.shopfollow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "shop_follows")
@Getter
public class ShopFollow {

    @EmbeddedId
    private ShopFollowId id;

    @Column(
            name = "followed_at",
            nullable = false,
            updatable = false
    )
    private Instant followedAt;

    protected ShopFollow() {
    }

    private ShopFollow(
            ShopFollowId id,
            Instant followedAt
    ) {

        this.id =
                Objects.requireNonNull(
                        id,
                        "id must not be null"
                );

        this.followedAt =
                Objects.requireNonNull(
                        followedAt,
                        "followedAt must not be null"
                );
    }

    public static ShopFollow create(
            UUID userId,
            UUID shopId,
            Instant followedAt
    ) {

        return new ShopFollow(
                new ShopFollowId(
                        userId,
                        shopId
                ),
                followedAt
        );
    }

    public UUID getUserId() {
        return id.getUserId();
    }

    public UUID getShopId() {
        return id.getShopId();
    }
}
