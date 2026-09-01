package com.ecommerce.authuser.auth.web;

import com.ecommerce.authuser.auth.application.SignupCommand;
import com.ecommerce.authuser.auth.application.SignupResult;
import com.ecommerce.authuser.auth.application.SignupService;
import com.ecommerce.authuser.common.id.UuidV7Generator;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SignupService signupService;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(
            @Valid @RequestBody SignupRequest request,

            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        SignupResult result = signupService.signup(
                new SignupCommand(
                        request.fullName(),
                        request.email(),
                        request.password(),
                        request.phone()
                )
        );

        String resolvedRequestId = resolveRequestId(requestId);

        SignupResponse response = new SignupResponse(
                new SignupResponse.Data(
                        new SignupResponse.UserData(
                                result.userId(),
                                result.fullName(),
                                result.email(),
                                false,
                                result.phone(),
                                false,
                                List.of("BUYER"),
                                "ACTIVE"
                        ),

                        new SignupResponse.TokenData(
                                "Bearer",
                                result.accessToken(),
                                result.accessExpiresIn(),
                                result.refreshToken(),
                                result.refreshExpiresIn()
                        ),

                        new SignupResponse.VerificationData(
                                true,
                                result.verificationExpiresAt()
                        )
                ),

                new SignupResponse.Meta(resolvedRequestId)
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
