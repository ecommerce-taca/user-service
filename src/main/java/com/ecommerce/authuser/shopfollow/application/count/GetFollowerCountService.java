package com.ecommerce.authuser.shopfollow.application.count;

import com.ecommerce.authuser.shop.domain.Shop;
import com.ecommerce.authuser.shop.domain.ShopStatus;
import com.ecommerce.authuser.shop.exception.ShopNotFoundException;
import com.ecommerce.authuser.shop.repository.ShopRepository;

import com.ecommerce.authuser.shopfollow.exception.InvalidShopFollowQueryException;
import com.ecommerce.authuser.shopfollow.repository.ShopFollowRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetFollowerCountService {

    private final ShopRepository shopRepository;

    private final ShopFollowRepository shopFollowRepository;

    @Transactional(readOnly = true)
    public GetFollowerCountResult get(GetFollowerCountQuery query) {
        if (query == null || query.shopId() == null) {

            throw new InvalidShopFollowQueryException();
        }

        Shop shop = shopRepository
                .findByIdAndDeletedAtIsNull(query.shopId())
                .orElseThrow(ShopNotFoundException::new);

        if (shop.getStatus() == ShopStatus.DELETED) {
            throw new ShopNotFoundException();
        }

        long followerCount = shopFollowRepository
                .countById_ShopId(shop.getId());

        return new GetFollowerCountResult(
                shop.getId(),
                followerCount
        );
    }
}
