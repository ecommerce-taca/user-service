package com.ecommerce.authuser.shopfollow.infrastructure;

import com.ecommerce.authuser.shop.port.ShopFollowerCountPort;
import com.ecommerce.authuser.shopfollow.repository.ShopFollowRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ShopFollowerCountAdapter implements ShopFollowerCountPort {

    private final ShopFollowRepository shopFollowRepository;

    @Override
    public long countFollowers(UUID shopId) {

        Objects.requireNonNull(
                shopId,
                "shopId must not be null"
        );

        return shopFollowRepository.countById_ShopId(shopId);
    }
}
