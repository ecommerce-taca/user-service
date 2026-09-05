package com.ecommerce.authuser.kyc.web.admin.review;

import com.ecommerce.authuser.auth.exception.mfa.MfaAuthenticationRequiredException;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.kyc.application.admin.review.AdminKycReviewCommand;
import com.ecommerce.authuser.kyc.application.admin.review.AdminKycReviewResult;
import com.ecommerce.authuser.kyc.application.admin.review.AdminKycReviewService;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/shops")
@RequiredArgsConstructor
public class AdminKycReviewController {

    private final AdminKycReviewService adminKycReviewService;

    @PostMapping("/{shopId}/kyc/review")
    public ResponseEntity<AdminKycReviewResponse> review(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("shopId") String shopId,
            @RequestBody(required = false) AdminKycReviewRequest request,
            @RequestHeader(name = "X-MFA-Step-Up", required = false) String stepUpToken,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId,
            HttpServletRequest httpRequest
    ) {
        UUID actorUserId = parseActorUserId(jwt);

        UUID sessionId = parseSessionId(jwt);

        UUID parsedShopId = parseShopId(shopId);

        String decision = request == null ? null : request.decision();

        String reason = request == null ? null : request.reason();

        AdminKycReviewResult result =
                adminKycReviewService.review(
                        new AdminKycReviewCommand(
                                actorUserId,
                                sessionId,
                                parsedShopId,
                                decision,
                                reason,
                                stepUpToken,
                                httpRequest.getRemoteAddr()
                        )
                );

        return ResponseEntity.ok(
                new AdminKycReviewResponse(
                        new AdminKycReviewResponse.Data(
                                result.shopId(),
                                result.kycCaseId(),
                                result.status(),
                                result.reviewedAt()
                        ),

                        new AdminKycReviewResponse.Meta(
                                resolveRequestId(
                                        requestId
                                )
                        )
                )
        );
    }

    private UUID parseActorUserId(Jwt jwt) {
        if (jwt == null
                || jwt.getSubject() == null
                || jwt.getSubject().isBlank()) {
            throw new MfaAuthenticationRequiredException();
        }

        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException ex) {
            throw new MfaAuthenticationRequiredException();
        }
    }

    private UUID parseSessionId(Jwt jwt) {
        if (jwt == null) {
            throw new MfaAuthenticationRequiredException();
        }

        String value = jwt.getClaimAsString("session_id");

        if (value == null || value.isBlank()) {
            throw new MfaAuthenticationRequiredException();
        }

        try {
            return UUID.fromString(value);
        } catch (RuntimeException ex) {
            throw new MfaAuthenticationRequiredException();
        }
    }

    private UUID parseShopId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
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
