package com.ecommerce.authuser.shopfollow.application.follow;

import com.ecommerce.authuser.shop.domain.Shop;
import com.ecommerce.authuser.shop.domain.ShopStatus;
import com.ecommerce.authuser.shop.exception.ShopNotFoundException;
import com.ecommerce.authuser.shop.repository.ShopRepository;

import com.ecommerce.authuser.shopfollow.domain.ShopFollow;
import com.ecommerce.authuser.shopfollow.domain.ShopFollowId;
import com.ecommerce.authuser.shopfollow.exception.InvalidShopFollowInputException;
import com.ecommerce.authuser.shopfollow.exception.ShopFollowLimitReachedException;
import com.ecommerce.authuser.shopfollow.repository.ShopFollowRepository;

import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.exception.profile.UserNotFoundException;
import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class FollowShopService {

    private static final long MAX_FOLLOWED_SHOPS_PER_USER = 1000L;

    private final UserRepository userRepository;

    private final ShopRepository shopRepository;

    private final ShopFollowRepository shopFollowRepository;

    @Transactional
    public FollowShopResult follow(FollowShopCommand command) {
        if (command == null
                || command.userId() == null
                || command.shopId() == null) {

            throw new InvalidShopFollowInputException();
        }

        User user = userRepository
                .findByIdForUpdate(command.userId())
                .orElseThrow(UserNotFoundException::new);

        Shop shop = shopRepository
                .findByIdAndDeletedAtIsNull(command.shopId())
                .orElseThrow(ShopNotFoundException::new);

        if (shop.getStatus() == ShopStatus.DELETED) {
            throw new ShopNotFoundException();
        }

        ShopFollowId followId =
                new ShopFollowId(
                        user.getId(),
                        shop.getId()
                );

        ShopFollow existing = shopFollowRepository
                .findById(followId)
                .orElse(null);

        if (existing != null) {

            return new FollowShopResult(
                    existing.getShopId(),
                    existing.getFollowedAt(),
                    false
            );
        }

        long currentCount =
                shopFollowRepository
                        .countById_UserId(
                                user.getId()
                        );

        if (currentCount
                >= MAX_FOLLOWED_SHOPS_PER_USER) {

            throw new ShopFollowLimitReachedException();
        }

        ShopFollow follow =
                ShopFollow.create(
                        user.getId(),
                        shop.getId(),
                        Instant.now()
                );

        shopFollowRepository.saveAndFlush(follow);

        return new FollowShopResult(
                follow.getShopId(),
                follow.getFollowedAt(),
                true
        );
    }
}
