package com.ecommerce.authuser.favorite.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@Getter
public class UserFavoriteId implements Serializable {

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "product_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID productId;

    protected UserFavoriteId() {
    }

    public UserFavoriteId(
            UUID userId,
            UUID productId
    ) {

        this.userId =
                Objects.requireNonNull(
                        userId,
                        "userId must not be null"
                );

        this.productId =
                Objects.requireNonNull(
                        productId,
                        "productId must not be null"
                );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof UserFavoriteId that)) {
            return false;
        }

        return Objects.equals(
                userId,
                that.userId
        ) && Objects.equals(
                productId,
                that.productId
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                userId,
                productId
        );
    }
}
