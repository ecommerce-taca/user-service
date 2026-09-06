package com.ecommerce.authuser.shop.application.publicprofile;

import com.ecommerce.authuser.shop.domain.KycStatus;
import com.ecommerce.authuser.shop.domain.Shop;
import com.ecommerce.authuser.shop.domain.ShopStatus;

import com.ecommerce.authuser.shop.exception.InvalidPublicShopQueryException;
import com.ecommerce.authuser.shop.exception.ShopNotFoundException;

import com.ecommerce.authuser.shop.port.ShopFollowerCountPort;
import com.ecommerce.authuser.shop.repository.ShopRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetPublicShopService {

    private final ShopRepository shopRepository;

    private final ShopFollowerCountPort shopFollowerCountPort;

    @Transactional(readOnly = true)
    public GetPublicShopResult get(GetPublicShopQuery query) {
        if (query == null || query.shopId() == null) {
            throw new InvalidPublicShopQueryException();
        }

        Shop shop = shopRepository
                .findByIdAndDeletedAtIsNull(query.shopId())
                .orElseThrow(ShopNotFoundException::new);

        if (shop.getStatus() == ShopStatus.DELETED) {
            throw new ShopNotFoundException();
        }

        boolean verified = shop.getKycStatus() == KycStatus.APPROVED;

        long followerCount = shopFollowerCountPort
                .countFollowers(shop.getId());

        return new GetPublicShopResult(
                shop.getId(),
                shop.getName(),
                shop.getSlug(),
                shop.getLogoObjectKey(),
                shop.getDescription(),
                verified,
                shop.getStatus(),
                followerCount,
                shop.getCreatedAt()
        );
    }
}
