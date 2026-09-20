package com.ecommerce.authuser.kyc.application.submit;

import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@Validated
@ConfigurationProperties(prefix = "auth.kyc-submission")
public class KycSubmissionProperties {

    @Min(1)
    private long expiryDays = 30;

    public long getExpiryDays() {
        return expiryDays;
    }

    public void setExpiryDays(long expiryDays) {
        this.expiryDays = expiryDays;
    }

    public Duration expiryTtl() {
        return Duration.ofDays(expiryDays);
    }
}