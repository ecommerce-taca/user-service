package com.ecommerce.authuser.kyc.web.admin.detail;

import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.kyc.application.admin.detail.AdminKycDetailQuery;
import com.ecommerce.authuser.kyc.application.admin.detail.AdminKycDetailResult;
import com.ecommerce.authuser.kyc.application.admin.detail.AdminKycDetailService;

import com.ecommerce.authuser.kyc.exception.AdminKycPermissionDeniedException;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/shops")
@RequiredArgsConstructor
public class AdminKycDetailController {

    private final AdminKycDetailService adminKycDetailService;

    @GetMapping("/{shopId}/kyc")
    public ResponseEntity<AdminKycDetailResponse> getDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("shopId") String shopId,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {

        UUID actorUserId = parseActorUserId(jwt);

        UUID parsedShopId = parseShopId(shopId);

        AdminKycDetailResult result =
                adminKycDetailService.get(
                        new AdminKycDetailQuery(
                                actorUserId,
                                parsedShopId
                        )
                );

        return ResponseEntity.ok(
                toResponse(
                        result,
                        requestId
                )
        );
    }

    private UUID parseActorUserId(Jwt jwt) {
        if (jwt == null
                || jwt.getSubject() == null
                || jwt.getSubject().isBlank()) {
            throw new AdminKycPermissionDeniedException();
        }

        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new AdminKycPermissionDeniedException();
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

    private AdminKycDetailResponse toResponse(
            AdminKycDetailResult result,
            String requestId
    ) {

        AdminKycDetailResponse.ShopData shop =
                new AdminKycDetailResponse.ShopData(
                        result.shop().id(),
                        result.shop().name(),
                        result.shop().businessName(),
                        result.shop().taxCodeMasked(),
                        result.shop().status()
                );

        AdminKycDetailResponse.KycCaseData kycCase =
                new AdminKycDetailResponse.KycCaseData(
                        result.kycCase().id(),
                        result.kycCase().status(),
                        result.kycCase().submittedAt(),

                        result.kycCase()
                                .documents()
                                .stream()
                                .map(this::toDocumentResponse)
                                .toList()
                );

        return new AdminKycDetailResponse(
                new AdminKycDetailResponse.Data(
                        shop,
                        kycCase
                ),

                new AdminKycDetailResponse.Meta(
                        resolveRequestId(requestId)
                )
        );
    }

    private AdminKycDetailResponse.DocumentData toDocumentResponse(
            AdminKycDetailResult.DocumentData document
    ) {

        return new AdminKycDetailResponse.DocumentData(
                document.id(),
                document.documentType(),
                document.originalFileName(),
                document.contentType(),
                document.sizeBytes(),
                document.status(),
                document.downloadUrl(),
                document.downloadExpiresAt()
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