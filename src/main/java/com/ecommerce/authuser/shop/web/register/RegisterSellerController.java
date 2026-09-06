package com.ecommerce.authuser.shop.web.register;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.shop.application.register.RegisterSellerCommand;
import com.ecommerce.authuser.shop.application.register.RegisterSellerResult;
import com.ecommerce.authuser.shop.application.register.RegisterSellerService;
import com.ecommerce.authuser.shop.exception.InvalidSellerRegistrationException;

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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class RegisterSellerController {

    private final RegisterSellerService registerSellerService;

    @PostMapping("/register-seller")
    public ResponseEntity<RegisterSellerResponse> registerSeller(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody RegisterSellerRequest request,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        if (request.invalid()) {
            throw new InvalidSellerRegistrationException();
        }

        UUID userId = UUID.fromString(jwt.getSubject());

        RegisterSellerResult result =
                registerSellerService.register(
                        new RegisterSellerCommand(
                                userId,
                                request.name(),
                                request.businessName(),
                                request.taxCode(),
                                request.slug(),
                                request.description()
                        )
                );

        RegisterSellerResponse response =
                new RegisterSellerResponse(
                        new RegisterSellerResponse.Data(
                                new RegisterSellerResponse.ShopData(
                                        result.shop().id(),
                                        result.shop().name(),
                                        result.shop().slug(),
                                        result.shop().status(),
                                        result.shop().kycStatus()
                                ),
                                new RegisterSellerResponse.OnboardingData(
                                        result.onboarding().currentStep(),
                                        result.onboarding().completedSteps(),
                                        result.onboarding().blockers()
                                )
                        ),
                        new RegisterSellerResponse.Meta(
                                resolveRequestId(requestId)
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
