package com.ecommerce.authuser.outbox.application;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Map;

@Component
public class NotificationLinkFactory {

    private final NotificationLinkProperties properties;

    public NotificationLinkFactory(NotificationLinkProperties properties) {
        this.properties = properties;
    }

    public Map<String, Object> emailVerificationData(
            String displayName,
            String rawToken,
            Duration ttl
    ) {
        return Map.of(
                "display_name", displayName,
                "verification_url", buildUrl(
                        properties.getEmailVerificationPath(),
                        rawToken
                ),
                "expires_in_minutes", ttl.toMinutes()
        );
    }

    public Map<String, Object> passwordResetData(
            String rawToken,
            Duration ttl
    ) {
        return Map.of(
                "reset_url", buildUrl(
                        properties.getPasswordResetPath(),
                        rawToken
                ),
                "expires_in_minutes", ttl.toMinutes()
        );
    }

    public Map<String, Object> phoneOtpData(
            String challengeId,
            Duration ttl
    ) {
        return Map.of(
                "challenge_id", challengeId,
                "expires_in_minutes", ttl.toMinutes()
        );
    }

    private String buildUrl(
            String path,
            String token
    ) {
        return UriComponentsBuilder
                .fromUriString(properties.getPublicBaseUrl())
                .path(path)
                .queryParam("t", token)
                .build()
                .toUriString();
    }
}