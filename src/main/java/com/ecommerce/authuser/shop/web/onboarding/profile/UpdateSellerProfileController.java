package com.ecommerce.authuser.shop.web.onboarding.profile;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.shop.application.onboarding.profile.UpdateSellerProfileCommand;
import com.ecommerce.authuser.shop.application.onboarding.profile.UpdateSellerProfileResult;
import com.ecommerce.authuser.shop.application.onboarding.profile.UpdateSellerProfileService;

import com.ecommerce.authuser.shop.exception.InvalidSellerProfileException;

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
public class UpdateSellerProfileController {

    private final UpdateSellerProfileService updateSellerProfileService;

    @PutMapping("/profile")
    public ResponseEntity<UpdateSellerProfileResponse> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateSellerProfileRequest request,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        if (request.invalid()) {
            throw new InvalidSellerProfileException();
        }

        UUID userId = UUID.fromString(jwt.getSubject());

        UpdateSellerProfileResult result =
                updateSellerProfileService.update(
                        new UpdateSellerProfileCommand(
                                userId,
                                request.name(),
                                request.businessName(),
                                request.taxCode(),
                                request.description(),
                                request.logoObjectKey()
                        )
                );

        UpdateSellerProfileResponse response =
                new UpdateSellerProfileResponse(
                        new UpdateSellerProfileResponse.Data(
                                result.shopId(),
                                result.name(),
                                result.businessName(),
                                result.taxCode(),
                                result.description(),
                                result.logoObjectKey(),
                                result.currentStep(),
                                result.profileCompleted(),
                                result.blockers()
                        ),
                        new UpdateSellerProfileResponse.Meta(
                                resolveRequestId(requestId)
                        )
                );

        return ResponseEntity.ok(response);
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
