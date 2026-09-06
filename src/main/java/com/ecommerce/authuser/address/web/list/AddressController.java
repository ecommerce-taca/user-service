package com.ecommerce.authuser.address.web.list;

import com.ecommerce.authuser.address.application.list.AddressResult;
import com.ecommerce.authuser.address.application.list.ListMyAddressesQuery;
import com.ecommerce.authuser.address.application.list.ListMyAddressesResult;
import com.ecommerce.authuser.address.application.list.ListMyAddressesService;
import com.ecommerce.authuser.address.exception.InvalidAddressQueryException;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final ListMyAddressesService listMyAddressesService;

    @GetMapping
    public ResponseEntity<ListMyAddressesResponse> listMyAddresses(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "size", required = false) String size,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        UUID userId = UUID.fromString(jwt.getSubject());

        ListMyAddressesResult result =
                listMyAddressesService.list(
                        new ListMyAddressesQuery(
                                userId,
                                parseInteger(page),
                                parseInteger(size),
                                sort
                        )
                );

        List<ListMyAddressesResponse.Data> items =
                result.items()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        ListMyAddressesResponse response =
                new ListMyAddressesResponse(
                        items,

                        new ListMyAddressesResponse.Meta(
                                result.page(),
                                result.size(),
                                result.total(),
                                result.totalPages(),
                                resolveRequestId(
                                        requestId
                                )
                        )
                );

        return ResponseEntity.ok(
                response
        );
    }

    private Integer parseInteger(String value) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new InvalidAddressQueryException();
        }

        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new InvalidAddressQueryException();
        }
    }

    private ListMyAddressesResponse.Data toResponse(AddressResult address) {

        return new ListMyAddressesResponse.Data(
                address.id(),
                address.recipient(),
                address.phone(),
                address.line1(),
                address.line2(),
                address.ward(),
                address.district(),
                address.province(),
                address.postalCode(),
                address.defaultAddress(),
                address.createdAt(),
                address.updatedAt()
        );
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
