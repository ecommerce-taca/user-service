package com.ecommerce.authuser.user.web.profile;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.user.application.profile.*;

import com.ecommerce.authuser.user.exception.profile.ProfileInvalidException;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class MyProfileController {

    private final GetMyProfileService getMyProfileService;

    private final UpdateMyProfileService updateMyProfileService;

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

    @PutMapping("/me")
    public ResponseEntity<UpdateMyProfileResponse> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId,
            @RequestBody UpdateMyProfileRequest request
    ) {

        if (request.invalid()) {
            throw new ProfileInvalidException();
        }

        LocalDate dateOfBirth = parseDateOfBirth(request);

        UUID userId = UUID.fromString(jwt.getSubject());

        UpdateMyProfileResult result =
                updateMyProfileService.update(
                        new UpdateMyProfileCommand(
                                userId,

                                request.fullName(),

                                request.phoneProvided(),
                                request.phone(),

                                request.dateOfBirthProvided(),
                                dateOfBirth
                        )
                );

        GetMyProfileResult profile = result.profile();

        UpdateMyProfileResponse response =
                new UpdateMyProfileResponse(
                        new UpdateMyProfileResponse.Data(
                                profile.id(),
                                profile.fullName(),
                                profile.email(),
                                profile.emailVerified(),
                                profile.phone(),
                                profile.phoneVerified(),
                                profile.dateOfBirth(),
                                profile.roles(),
                                profile.status(),
                                profile.defaultShopId(),
                                profile.createdAt(),
                                profile.updatedAt(),
                                result.phoneVerificationRequired()
                        ),

                        new UpdateMyProfileResponse.Meta(
                                resolveRequestId(
                                        requestId
                                )
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

    private LocalDate parseDateOfBirth(UpdateMyProfileRequest request) {
        if (!request.dateOfBirthProvided()) {
            return null;
        }

        if (request.dateOfBirth() == null) {
            return null;
        }

        try {
            return LocalDate.parse(
                    request.dateOfBirth()
            );

        } catch (DateTimeParseException ex) {
            throw new ProfileInvalidException();
        }
    }
}
