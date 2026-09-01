package com.ecommerce.authuser.auth.web;

import com.ecommerce.authuser.auth.application.*;
import com.ecommerce.authuser.common.id.UuidV7Generator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SignupService signupService;

    private final SigninService signinService;

    private final RefreshService refreshService;

    private final SignoutService signoutService;

    private final EmailVerificationService emailVerificationService;

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

    @PostMapping("/signin")
    public ResponseEntity<SigninResponse> signin(
            @Valid @RequestBody SigninRequest request,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId,
            HttpServletRequest httpRequest
    ) {

        SigninResult result = signinService.signin(
                new SigninCommand(
                        request.identifier(),
                        request.password(),
                        request.resolvedRememberMe(),
                        httpRequest.getRemoteAddr(),
                        httpRequest.getHeader("User-Agent")
                )
        );

        String resolvedRequestId =
                resolveRequestId(requestId);

        SigninResponse response = new SigninResponse(
                new SigninResponse.Data(
                        new SigninResponse.UserData(
                                result.userId(),
                                result.fullName(),
                                result.email(),
                                result.emailVerified(),
                                result.phone(),
                                result.phoneVerified(),
                                result.roles(),
                                result.status()
                        ),

                        new SigninResponse.TokenData(
                                "Bearer",
                                result.accessToken(),
                                result.accessExpiresIn(),
                                result.refreshToken(),
                                result.refreshExpiresIn()
                        )
                ),

                new SigninResponse.Meta(resolvedRequestId)
        );

        return ResponseEntity.ok(
                response
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @Valid @RequestBody RefreshRequest request,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId,
            HttpServletRequest httpRequest
    ) {
        RefreshResult result = refreshService.refresh(
                new RefreshCommand(
                        request.refreshToken(),
                        httpRequest.getRemoteAddr()
                )
        );

        RefreshResponse response = new RefreshResponse(
                new RefreshResponse.Data(
                        new RefreshResponse.TokenData(
                                "Bearer",
                                result.accessToken(),
                                result.accessExpiresIn(),
                                result.refreshToken(),
                                result.refreshExpiresIn()
                        )
                ),

                new RefreshResponse.Meta(resolveRequestId(requestId)));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/signout")
    public ResponseEntity<Void> signout(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody(required = false) SignoutRequest request,
            HttpServletRequest httpRequest
    ) {

        UUID userId = UUID.fromString(jwt.getSubject());

        UUID sessionId = UUID.fromString(jwt.getClaimAsString("session_id"));

        String refreshToken =
                request == null
                        ? null
                        : request.refreshToken();

        boolean allSessions = request != null && request.resolvedAllSessions();

        signoutService.signout(
                new SignoutCommand(
                        userId,
                        sessionId,
                        refreshToken,
                        allSessions,
                        httpRequest.getRemoteAddr()
                )
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    @PostMapping("/email/verify")
    public ResponseEntity<EmailVerificationResponse> verifyEmail(
            @Valid @RequestBody EmailVerificationRequest request,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        EmailVerificationResult result = emailVerificationService.verify(
                new EmailVerificationCommand(request.token())
        );

        EmailVerificationResponse response =
                new EmailVerificationResponse(
                        new EmailVerificationResponse.Data(
                                result.userId(),
                                true,
                                result.verifiedAt()
                        ),

                        new EmailVerificationResponse.Meta(
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
