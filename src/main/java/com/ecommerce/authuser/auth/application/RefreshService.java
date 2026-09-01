package com.ecommerce.authuser.auth.application;

import com.ecommerce.authuser.audit.domain.AuditLog;
import com.ecommerce.authuser.audit.domain.AuditTargetType;
import com.ecommerce.authuser.audit.repository.AuditLogRepository;
import com.ecommerce.authuser.auth.exception.AccountSuspendedException;
import com.ecommerce.authuser.auth.exception.ExpiredRefreshTokenException;
import com.ecommerce.authuser.auth.exception.InvalidRefreshTokenException;
import com.ecommerce.authuser.auth.exception.ReusedRefreshTokenException;
import com.ecommerce.authuser.auth.security.AccessTokenService;
import com.ecommerce.authuser.auth.security.SecureTokenGenerator;
import com.ecommerce.authuser.auth.security.TokenHasher;
import com.ecommerce.authuser.rbac.domain.UserRole;
import com.ecommerce.authuser.rbac.repository.UserRoleRepository;
import com.ecommerce.authuser.security.service.AuditValueHasher;
import com.ecommerce.authuser.token.domain.RefreshToken;
import com.ecommerce.authuser.token.domain.TokenRevokeReason;
import com.ecommerce.authuser.token.repository.RefreshTokenLookup;
import com.ecommerce.authuser.token.repository.RefreshTokenRepository;
import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.domain.UserStatus;
import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshService {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    private final RefreshTokenRepository refreshTokenRepository;

    private final UserRepository userRepository;

    private final UserRoleRepository userRoleRepository;

    private final SecureTokenGenerator tokenGenerator;

    private final TokenHasher tokenHasher;

    private final AccessTokenService accessTokenService;

    private final AuditLogRepository auditLogRepository;

    private final AuditValueHasher auditValueHasher;

    @Transactional(
            noRollbackFor = {
                    ReusedRefreshTokenException.class,
                    ExpiredRefreshTokenException.class,
                    AccountSuspendedException.class
            }
    )
    public RefreshResult refresh(RefreshCommand command) {
        Instant now = Instant.now();

        String clientIp = command.clientIp() == null
                || command.clientIp().isBlank()
                ? "unknown"
                : command.clientIp().trim();

        String ipHash = auditValueHasher.hash(clientIp);

        String rawRefreshToken = command.refreshToken();

        String tokenHash = tokenHasher.hash(rawRefreshToken);

        RefreshTokenLookup lookup = refreshTokenRepository
                .findLookupByTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        UUID userId = lookup.userId();

        User user = userRepository
                .findByIdForUpdate(userId)
                .orElseThrow(AccountSuspendedException::new);

        List<RefreshToken> familyTokens = refreshTokenRepository
                .findAllByFamilyIdForUpdate(lookup.familyId());

        RefreshToken currentToken = familyTokens
                .stream()
                .filter(token -> token.getId().equals(lookup.tokenId()))
                .findFirst()
                .orElseThrow(InvalidRefreshTokenException::new);


        if (user.getStatus() == UserStatus.SUSPENDED
                || user.getStatus() == UserStatus.DELETED) {
            revokeActiveFamily(
                    familyTokens,
                    TokenRevokeReason.SUSPEND,
                    now
            );

            throw new AccountSuspendedException();
        }

        if (currentToken.getRevokeReason()
                == TokenRevokeReason.ROTATED) {

            long activeTokenCount =
                    familyTokens.stream()
                            .filter(token ->
                                    !token.isRevoked()
                            )
                            .count();

            revokeActiveFamily(
                    familyTokens,
                    TokenRevokeReason.REUSE,
                    now
            );

            AuditLog auditLog = AuditLog.create(user.getId(),

                    "AUTH_REFRESH_TOKEN_REUSE",

                    AuditTargetType.USER,

                    user.getId(),

                    "Previously rotated refresh token was reused",

                    Map.of(
                            "family_id", currentToken
                                    .getFamilyId()
                                    .toString(),

                            "reused_token_id", currentToken
                                    .getId()
                                    .toString(),

                            "active_tokens_revoked", activeTokenCount
                            ),

                            ipHash,
                            now
                    );

            auditLogRepository.saveAndFlush(auditLog);

            throw new ReusedRefreshTokenException();
        }

        if (currentToken.isRevoked()) {
            throw new InvalidRefreshTokenException();
        }

        if (currentToken.isExpired(now)) {
            currentToken.revoke(TokenRevokeReason.EXPIRED, now);

            throw new ExpiredRefreshTokenException();
        }

        String newRawRefreshToken = tokenGenerator.generate();

        String newTokenHash = tokenHasher.hash(newRawRefreshToken);

        RefreshToken newRefreshToken =
                RefreshToken.issue(
                        user,
                        newTokenHash,
                        currentToken.getFamilyId(),
                        now,
                        now.plus(REFRESH_TOKEN_TTL)
                );

        refreshTokenRepository.save(newRefreshToken);

        currentToken.markSeen(now);

        currentToken.markRotated(newRefreshToken.getId(), now);

        List<String> roles = loadRoles(user.getId());

        boolean emailVerified = user.getEmailVerifiedAt() != null;

        String accessToken = accessTokenService.issue(
                user.getId(),
                currentToken.getFamilyId(),
                roles,
                emailVerified,
                now,
                now.plus(ACCESS_TOKEN_TTL)
        );

        return new RefreshResult(
                accessToken,
                newRawRefreshToken,
                ACCESS_TOKEN_TTL.toSeconds(),
                REFRESH_TOKEN_TTL.toSeconds()
        );
    }

    private void revokeActiveFamily(
            List<RefreshToken> familyTokens,
            TokenRevokeReason reason,
            Instant now
    ) {
        familyTokens.stream()
                .filter(token -> !token.isRevoked())
                .forEach(token -> token.revoke(reason, now));
    }

    private List<String> loadRoles(
            UUID userId
    ) {

        return userRoleRepository
                .findAllByUser_IdAndRevokedAtIsNull(userId)
                .stream()
                .map(UserRole::getRole)
                .map(role -> role.getRoleKey())
                .distinct()
                .sorted()
                .toList();
    }
}
