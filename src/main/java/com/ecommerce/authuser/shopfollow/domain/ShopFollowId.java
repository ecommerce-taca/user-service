package com.ecommerce.authuser.shopfollow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@Getter
public class ShopFollowId implements Serializable {

    @Column(
            name = "user_id",
            nullable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID userId;

    @Column(
            name = "shop_id",
            nullable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID shopId;

    protected ShopFollowId() {
    }

    public ShopFollowId(
            UUID userId,
            UUID shopId
    ) {

        this.userId =
                Objects.requireNonNull(
                        userId,
                        "userId must not be null"
                );

        this.shopId =
                Objects.requireNonNull(
                        shopId,
                        "shopId must not be null"
                );
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }

        if (!(object instanceof ShopFollowId other)) {
            return false;
        }

        return Objects.equals(
                userId,
                other.userId
        ) && Objects.equals(
                shopId,
                other.shopId
        );
    }

    @Override
    public int hashCode() {

        return Objects.hash(
                userId,
                shopId
        );
    }
}
