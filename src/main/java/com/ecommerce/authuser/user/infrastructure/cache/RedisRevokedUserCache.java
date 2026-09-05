package com.ecommerce.authuser.user.infrastructure.cache;

import com.ecommerce.authuser.user.application.admin.status.RevokedUserCache;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisRevokedUserCache implements RevokedUserCache {

    private static final String KEY_PREFIX =
            "revoked_user_id:";

    private static final Duration TTL = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void markRevoked(
            UUID userId,
            Instant suspendedAt
    ) {

        Objects.requireNonNull(
                userId,
                "userId must not be null"
        );

        Objects.requireNonNull(
                suspendedAt,
                "suspendedAt must not be null"
        );

        String key = KEY_PREFIX + userId;

        redisTemplate
                .opsForValue()
                .set(
                        key,
                        suspendedAt.toString(),
                        TTL
                );
    }
}
