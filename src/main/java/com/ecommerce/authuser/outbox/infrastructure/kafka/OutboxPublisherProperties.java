package com.ecommerce.authuser.outbox.infrastructure.kafka;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "auth.outbox-publisher")
public class OutboxPublisherProperties {

    private boolean enabled = true;

    @Min(1)
    @Max(500)
    private int batchSize = 50;

    @Min(100)
    private long fixedDelayMs = 2000;

    @Min(1)
    @Max(3)
    private int maxRetries = 3;

    @Min(1)
    private long retryBackoffSeconds = 2;

    @Min(1000)
    @Max(300000)
    private long sendTimeoutMs = 35000;

    @Min(1)
    private long maxPendingEvents = 1000;

    @Min(1)
    private long maxOldestPendingAgeSeconds = 300;

    @Min(0)
    private long maxFailedEvents = 0;

    public long getMaxPendingEvents() {
        return maxPendingEvents;
    }

    public void setMaxPendingEvents(long maxPendingEvents) {
        this.maxPendingEvents = maxPendingEvents;
    }

    public long getMaxOldestPendingAgeSeconds() {
        return maxOldestPendingAgeSeconds;
    }

    public void setMaxOldestPendingAgeSeconds(long maxOldestPendingAgeSeconds) {
        this.maxOldestPendingAgeSeconds = maxOldestPendingAgeSeconds;
    }

    public long getMaxFailedEvents() {
        return maxFailedEvents;
    }

    public void setMaxFailedEvents(long maxFailedEvents) {
        this.maxFailedEvents = maxFailedEvents;
    }

    public long getSendTimeoutMs() {
        return sendTimeoutMs;
    }

    public void setSendTimeoutMs(long sendTimeoutMs) {
        this.sendTimeoutMs = sendTimeoutMs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getRetryBackoffSeconds() {
        return retryBackoffSeconds;
    }

    public void setRetryBackoffSeconds(long retryBackoffSeconds) {
        this.retryBackoffSeconds = retryBackoffSeconds;
    }
}