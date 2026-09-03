package com.ecommerce.authuser.shop.web.onboarding.bank;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.shop.application.onboarding.bank.UpdateSellerBankCommand;
import com.ecommerce.authuser.shop.application.onboarding.bank.UpdateSellerBankResult;
import com.ecommerce.authuser.shop.application.onboarding.bank.UpdateSellerBankService;

import com.ecommerce.authuser.shop.exception.InvalidSellerBankException;

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
public class UpdateSellerBankController {

    private final UpdateSellerBankService updateSellerBankService;

    @PutMapping("/bank")
    public ResponseEntity<UpdateSellerBankResponse> updateBank(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateSellerBankRequest request,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        if (request.invalid()) {
            throw new InvalidSellerBankException();
        }

        UUID userId = parseUserId(jwt.getSubject());

        UpdateSellerBankResult result =
                updateSellerBankService.update(
                        new UpdateSellerBankCommand(
                                userId,
                                request.bankCode(),
                                request.bankName(),
                                request.accountName(),
                                request.accountNumber(),
                                request.confirmAccountName()
                        )
                );

        UpdateSellerBankResponse response =
                new UpdateSellerBankResponse(
                        new UpdateSellerBankResponse.Data(
                                result.bankName(),
                                result.maskedAccount(),
                                result.verified()
                        ),
                        new UpdateSellerBankResponse.Meta(
                                resolveRequestId(
                                        requestId
                                )
                        )
                );

        return ResponseEntity.ok(
                response
        );
    }

    private UUID parseUserId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidSellerBankException();
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
