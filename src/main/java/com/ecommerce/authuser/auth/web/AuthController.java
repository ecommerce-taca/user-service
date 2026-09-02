package com.ecommerce.authuser.auth.web;

import com.ecommerce.authuser.auth.application.*;
import com.ecommerce.authuser.auth.application.password.*;
import com.ecommerce.authuser.auth.web.password.PasswordForgotRequest;
import com.ecommerce.authuser.auth.web.password.PasswordForgotResponse;
import com.ecommerce.authuser.auth.web.password.PasswordResetRequest;
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

    private final EmailVerificationResendService emailVerificationResendService;

    private final PhoneOtpRequestService phoneOtpRequestService;

    private final PhoneOtpVerifyService phoneOtpVerifyService;

    private final PasswordForgotService passwordForgotService;

    private final PasswordResetService passwordResetService;

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

    @PostMapping("/email/resend")
    public ResponseEntity<EmailResendResponse>
    resendVerificationEmail(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody(required = false) EmailResendRequest request,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId,
            HttpServletRequest httpRequest
    ) {

        UUID userId = UUID.fromString(jwt.getSubject());

        EmailResendResult result = emailVerificationResendService.resend(
                new EmailResendCommand(
                        userId,
                        httpRequest.getRemoteAddr()
                )
        );

        EmailResendResponse response = new EmailResendResponse(
                new EmailResendResponse.Data(true, result.expiresAt()),
                new EmailResendResponse.Meta(resolveRequestId(requestId)));

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
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

    @PostMapping("/phone/request-otp")
    public ResponseEntity<PhoneOtpRequestResponse> requestPhoneOtp(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PhoneOtpRequest request,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        UUID userId = UUID.fromString(jwt.getSubject());

        PhoneOtpRequestResult result = phoneOtpRequestService.request(
                new PhoneOtpRequestCommand(
                        userId,
                        request.phone()
                )
        );

        PhoneOtpRequestResponse response = new PhoneOtpRequestResponse(
                new PhoneOtpRequestResponse.Data(
                        result.challengeId(),
                        result.maskedPhone(),
                        result.expiresAt(),
                        result.maxAttempts()
                ),
                new PhoneOtpRequestResponse.Meta(
                        resolveRequestId(requestId)
                )
        );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
    }

    @PostMapping("/phone/verify-otp")
    public ResponseEntity<PhoneOtpVerifyResponse> verifyPhoneOtp(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PhoneOtpVerifyRequest request,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        PhoneOtpVerifyResult result = phoneOtpVerifyService.verify(
                new PhoneOtpVerifyCommand(
                        userId,
                        request.challengeId(),
                        request.otp()
                )
        );

        PhoneOtpVerifyResponse response = new PhoneOtpVerifyResponse(
                new PhoneOtpVerifyResponse.Data(
                        true,
                        result.verifiedAt()
                ),

                new PhoneOtpVerifyResponse.Meta(
                        resolveRequestId(requestId)
                )
        );

        return ResponseEntity.ok(
                response
        );
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<PasswordForgotResponse> forgotPassword(
            @Valid @RequestBody PasswordForgotRequest request,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {
        PasswordForgotResult result =
                passwordForgotService.forgot(
                        new PasswordForgotCommand(request.identifier())
                );

        PasswordForgotResponse response = new PasswordForgotResponse(
                new PasswordForgotResponse.Data(
                        result.accepted(),
                        "Nếu tài khoản tồn tại, hướng dẫn đặt lại mật khẩu sẽ được gửi."
                ),

                new PasswordForgotResponse.Meta(
                        resolveRequestId(requestId))
                );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        passwordResetService.reset(
                new PasswordResetCommand(
                        request.token(),
                        request.newPassword()
                )
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}
