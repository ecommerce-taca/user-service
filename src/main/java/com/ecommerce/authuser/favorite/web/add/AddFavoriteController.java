package com.ecommerce.authuser.favorite.web.add;

import com.ecommerce.authuser.common.id.CanonicalUuidParser;

import com.ecommerce.authuser.common.web.RequestIdResolver;
import com.ecommerce.authuser.favorite.application.add.AddFavoriteCommand;
import com.ecommerce.authuser.favorite.application.add.AddFavoriteResult;
import com.ecommerce.authuser.favorite.application.add.AddFavoriteService;

import com.ecommerce.authuser.favorite.exception.InvalidFavoriteInputException;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/favorites")
@RequiredArgsConstructor
public class AddFavoriteController {

    private final AddFavoriteService addFavoriteService;

    @PostMapping
    public ResponseEntity<AddFavoriteResponse> addFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody AddFavoriteRequest request,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        if (request == null || request.invalid()) {
            throw new InvalidFavoriteInputException();
        }

        UUID productId = CanonicalUuidParser.parse(
                request.productId(),
                InvalidFavoriteInputException::new
        );;

        UUID userId = UUID.fromString(jwt.getSubject());

        AddFavoriteResult result =
                addFavoriteService.add(
                        new AddFavoriteCommand(
                                userId,
                                productId
                        )
                );

        AddFavoriteResponse response =
                new AddFavoriteResponse(

                        new AddFavoriteResponse.Data(
                                result.productId(),
                                result.createdAt()
                        ),

                        new AddFavoriteResponse.Meta(
                                RequestIdResolver.resolve(requestId)
                        )
                );

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;

        return ResponseEntity
                .status(status)
                .body(response);
    }
}
