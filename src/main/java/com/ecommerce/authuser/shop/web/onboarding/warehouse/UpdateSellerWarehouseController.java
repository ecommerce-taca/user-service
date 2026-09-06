package com.ecommerce.authuser.shop.web.onboarding.warehouse;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.shop.application.onboarding.warehouse.UpdateSellerWarehouseCommand;
import com.ecommerce.authuser.shop.application.onboarding.warehouse.UpdateSellerWarehouseResult;
import com.ecommerce.authuser.shop.application.onboarding.warehouse.UpdateSellerWarehouseService;

import com.ecommerce.authuser.shop.exception.InvalidSellerWarehouseException;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seller/onboarding")
@RequiredArgsConstructor
public class UpdateSellerWarehouseController {

    private final UpdateSellerWarehouseService updateSellerWarehouseService;

    @PutMapping("/warehouse")
    public ResponseEntity<UpdateSellerWarehouseResponse>
    updateWarehouse(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateSellerWarehouseRequest request,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        if (request.invalid()) {
            throw new InvalidSellerWarehouseException();
        }

        UUID userId = UUID.fromString(jwt.getSubject());

        WarehouseAddressRequest address = request.address();

        UpdateSellerWarehouseCommand.AddressCommand
                addressCommand =
                address == null
                        ? null
                        : new UpdateSellerWarehouseCommand.AddressCommand(
                        address.line1(),
                        address.line2(),
                        address.ward(),
                        address.district(),
                        address.province(),
                        address.postalCode()
                );

        UpdateSellerWarehouseResult result =
                updateSellerWarehouseService.update(
                        new UpdateSellerWarehouseCommand(
                                userId,
                                request.warehouseName(),
                                request.contactName(),
                                request.contactPhone(),
                                addressCommand,
                                request.carrierPreferences(),
                                request.codEnabled()
                        )
                );

        UpdateSellerWarehouseResponse response =
                new UpdateSellerWarehouseResponse(
                        new UpdateSellerWarehouseResponse.Data(
                                result.shopId(),
                                result.warehouseName(),
                                result.contactName(),
                                result.contactPhone(),

                                new UpdateSellerWarehouseResponse.Address(
                                        result.address().line1(),
                                        result.address().line2(),
                                        result.address().ward(),
                                        result.address().district(),
                                        result.address().province(),
                                        result.address().postalCode()
                                ),

                                result.carrierPreferences(),
                                result.codEnabled(),
                                result.currentStep(),
                                result.warehouseCompleted(),
                                result.blockers()
                        ),

                        new UpdateSellerWarehouseResponse.Meta(
                                resolveRequestId(
                                        requestId
                                )
                        )
                );

        return ResponseEntity.ok(
                response
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