package com.ecommerce.authuser.address.web.update;

import com.ecommerce.authuser.address.application.update.UpdateMyAddressCommand;
import com.ecommerce.authuser.address.application.update.UpdateMyAddressResult;
import com.ecommerce.authuser.address.application.update.UpdateMyAddressService;

import com.ecommerce.authuser.address.exception.InvalidAddressInputException;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
public class UpdateAddressController {

    private final UpdateMyAddressService updateMyAddressService;

    @PutMapping("/{addressId}")
    public ResponseEntity<UpdateMyAddressResponse> updateMyAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String addressId,
            @RequestBody UpdateMyAddressRequest request,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        if (request.invalid()) {
            throw new InvalidAddressInputException();
        }

        UUID userId = UUID.fromString(jwt.getSubject());

        UUID parsedAddressId = parseAddressId(addressId);

        UpdateMyAddressResult result = updateMyAddressService.update(
                new UpdateMyAddressCommand(
                        userId,
                        parsedAddressId,
                        request.recipient(),
                        request.phone(),
                        request.line1(),
                        request.line2(),
                        request.ward(),
                        request.district(),
                        request.province(),
                        request.postalCode(),
                        request.defaultProvided(),
                        request.defaultRequested()
                )
        );

        UpdateMyAddressResponse response =
                new UpdateMyAddressResponse(
                        new UpdateMyAddressResponse.Data(
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

                        new UpdateMyAddressResponse.Meta(resolveRequestId(requestId)
                        )
                );

        return ResponseEntity.ok(
                response
        );
    }

    private UUID parseAddressId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new InvalidAddressInputException();
        }
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
