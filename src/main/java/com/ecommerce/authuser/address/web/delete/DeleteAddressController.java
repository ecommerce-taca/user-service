package com.ecommerce.authuser.address.web.delete;

import com.ecommerce.authuser.address.application.delete.DeleteMyAddressCommand;
import com.ecommerce.authuser.address.application.delete.DeleteMyAddressService;
import com.ecommerce.authuser.address.exception.AddressNotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
public class DeleteAddressController {

    private final DeleteMyAddressService deleteMyAddressService;

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteMyAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String addressId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        UUID parsedAddressId =
                parseAddressId(addressId);

        deleteMyAddressService.delete(
                new DeleteMyAddressCommand(
                        userId,
                        parsedAddressId
                )
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    private UUID parseAddressId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new AddressNotFoundException();
        }
    }
}
