package com.ecommerce.authuser.auth.application.verification.email;

import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@Validated
@ConfigurationProperties(prefix = "auth.email-verification")
public class EmailVerificationProperties {

    @Min(1)
    private long ttlMinutes = 30;

    public long getTtlMinutes() {
        return ttlMinutes;
    }

    public void setTtlMinutes(long ttlMinutes) {
        this.ttlMinutes = ttlMinutes;
    }

    public Duration tokenTtl() {
        return Duration.ofMinutes(ttlMinutes);
    }
}
