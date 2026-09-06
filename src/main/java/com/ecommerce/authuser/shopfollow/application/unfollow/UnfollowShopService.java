package com.ecommerce.authuser.shopfollow.application.unfollow;

import com.ecommerce.authuser.shopfollow.domain.ShopFollow;
import com.ecommerce.authuser.shopfollow.domain.ShopFollowId;
import com.ecommerce.authuser.shopfollow.exception.InvalidShopFollowInputException;
import com.ecommerce.authuser.shopfollow.repository.ShopFollowRepository;

import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.exception.profile.UserNotFoundException;
import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnfollowShopService {

    private final UserRepository userRepository;

    private final ShopFollowRepository shopFollowRepository;

    @Transactional
    public void unfollow(UnfollowShopCommand command) {
        if (command == null
                || command.userId() == null
                || command.shopId() == null) {

            throw new InvalidShopFollowInputException();
        }

        User user = userRepository
                .findByIdForUpdate(command.userId())
                .orElseThrow(UserNotFoundException::new);

        ShopFollowId followId =
                new ShopFollowId(
                        user.getId(),
                        command.shopId()
                );

        ShopFollow follow = shopFollowRepository
                .findById(followId)
                .orElse(null);

        if (follow == null) {
            return;
        }

        shopFollowRepository.delete(follow);

        shopFollowRepository.flush();
    }
}
