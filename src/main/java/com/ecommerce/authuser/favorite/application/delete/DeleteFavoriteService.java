package com.ecommerce.authuser.favorite.application.delete;

import com.ecommerce.authuser.favorite.domain.UserFavorite;
import com.ecommerce.authuser.favorite.domain.UserFavoriteId;

import com.ecommerce.authuser.favorite.exception.InvalidFavoriteInputException;

import com.ecommerce.authuser.favorite.repository.UserFavoriteRepository;

import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.exception.profile.UserNotFoundException;
import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteFavoriteService {

    private final UserRepository userRepository;

    private final UserFavoriteRepository userFavoriteRepository;

    @Transactional
    public void delete(DeleteFavoriteCommand command) {

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

        UserFavorite favorite = userFavoriteRepository
                .findById(favoriteId)
                .orElse(null);

        if (favorite == null) {
            return;
        }

        userFavoriteRepository.delete(favorite);

        userFavoriteRepository.flush();
    }
}
