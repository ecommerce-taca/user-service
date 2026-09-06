package com.ecommerce.authuser.address.web.create;

import com.ecommerce.authuser.address.application.create.CreateMyAddressCommand;
import com.ecommerce.authuser.address.application.create.CreateMyAddressResult;
import com.ecommerce.authuser.address.application.create.CreateMyAddressService;
import com.ecommerce.authuser.address.exception.InvalidAddressInputException;

import com.ecommerce.authuser.common.id.UuidV7Generator;

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
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
public class CreateAddressController {

    private final CreateMyAddressService createMyAddressService;

    @PostMapping
    public ResponseEntity<CreateMyAddressResponse> createMyAddress(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateMyAddressRequest request,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {
        if (request.invalid()) {
            throw new InvalidAddressInputException();
        }

        UUID userId = UUID.fromString(jwt.getSubject());

        CreateMyAddressResult result =
                createMyAddressService.create(
                        new CreateMyAddressCommand(
                                userId,
                                request.recipient(),
                                request.phone(),
                                request.line1(),
                                request.line2(),
                                request.ward(),
                                request.district(),
                                request.province(),
                                request.postalCode(),
                                request.defaultRequested()
                        )
                );

        CreateMyAddressResponse response =
                new CreateMyAddressResponse(
                        new CreateMyAddressResponse.Data(
                                result.id(),
                                result.recipient(),
                                result.phone(),
                                result.line1(),
                                result.line2(),
                                result.ward(),
                                result.district(),
                                result.province(),
                                result.postalCode(),
                                result.defaultAddress(),
                                result.createdAt(),
                                result.updatedAt()
                        ),

                        new CreateMyAddressResponse.Meta(
                                resolveRequestId(
                                        requestId
                                )
                        )
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    private String resolveRequestId(String requestId) {

        if (requestId != null
                && !requestId.isBlank()
                && requestId.length() <= 64) {

            return requestId;
        }

        return UuidV7Generator
                .generate()
                .toString();
    }
}
