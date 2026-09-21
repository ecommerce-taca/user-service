package com.ecommerce.authuser.outbox.infrastructure.cleanup;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "auth.outbox-cleanup")
public class OutboxCleanupProperties {

    private boolean enabled = true;

    @Min(60000)
    private long fixedDelayMs = 3600000;

    @Min(1)
    @Max(365)
    private int publishedRetentionDays = 7;

    @Min(1)
    @Max(365)
    private int failedRetentionDays = 30;

    @Min(1)
    @Max(5000)
    private int batchSize = 500;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs;
    }

    public int getPublishedRetentionDays() {
        return publishedRetentionDays;
    }

    public void setPublishedRetentionDays(int publishedRetentionDays) {
        this.publishedRetentionDays = publishedRetentionDays;
    }

    public int getFailedRetentionDays() {
        return failedRetentionDays;
    }

    public void setFailedRetentionDays(int failedRetentionDays) {
        this.failedRetentionDays = failedRetentionDays;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}