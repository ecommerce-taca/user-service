package com.ecommerce.authuser.user.web.profile;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.user.application.profile.GetMyProfileResult;
import com.ecommerce.authuser.user.application.profile.GetMyProfileService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class MyProfileController {

    private final GetMyProfileService getMyProfileService;

    @GetMapping("/me")
    public ResponseEntity<MyProfileResponse> getMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        UUID userId = UUID.fromString(jwt.getSubject());

        GetMyProfileResult result = getMyProfileService.get(userId);

        MyProfileResponse response =
                new MyProfileResponse(
                        new MyProfileResponse.Data(
                                result.id(),
                                result.fullName(),
                                result.email(),
                                result.emailVerified(),
                                result.phone(),
                                result.phoneVerified(),
                                result.dateOfBirth(),
                                result.roles(),
                                result.status(),
                                result.defaultShopId(),
                                result.createdAt(),
                                result.updatedAt()
                        ),

                        new MyProfileResponse.Meta(
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
