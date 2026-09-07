package com.ecommerce.authuser.favorite.application.add;

import com.ecommerce.authuser.favorite.domain.UserFavorite;
import com.ecommerce.authuser.favorite.domain.UserFavoriteId;

import com.ecommerce.authuser.favorite.exception.FavoriteLimitReachedException;
import com.ecommerce.authuser.favorite.exception.InvalidFavoriteInputException;

import com.ecommerce.authuser.favorite.repository.UserFavoriteRepository;

import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.exception.profile.UserNotFoundException;
import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AddFavoriteService {

    private static final long MAX_FAVORITES_PER_USER = 500L;

    private final UserRepository userRepository;

    private final UserFavoriteRepository userFavoriteRepository;

    @Transactional
    public AddFavoriteResult add(AddFavoriteCommand command) {

        if (command == null
                || command.userId() == null
                || command.productId() == null) {

            throw new InvalidFavoriteInputException();
        }

        User user = userRepository
                .findByIdForUpdate(command.userId())
                .orElseThrow(UserNotFoundException::new);

        UserFavoriteId favoriteId =
                new UserFavoriteId(
                        user.getId(),
                        command.productId()
                );

        UserFavorite existing = userFavoriteRepository
                .findById(favoriteId)
                .orElse(null);

        if (existing != null) {

            return new AddFavoriteResult(
                    existing.getProductId(),
                    existing.getCreatedAt(),
                    false
            );
        }

        long currentCount = userFavoriteRepository
                .countById_UserId(user.getId());

        if (currentCount >= MAX_FAVORITES_PER_USER) {
            throw new FavoriteLimitReachedException();
        }

        UserFavorite favorite =
                UserFavorite.create(
                        user.getId(),
                        command.productId(),
                        Instant.now()
                );

        userFavoriteRepository.saveAndFlush(favorite);

        return new AddFavoriteResult(
                favorite.getProductId(),
                favorite.getCreatedAt(),
                true
        );
    }
}
