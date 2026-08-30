package com.ecommerce.authuser.support.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@TestConfiguration
public class TestClockConfig {

    private static final Instant DEFAULT_INSTANT =
            Instant.parse("2026-08-30T00:00:00Z");

    @Bean
    public Clock testClock() {
        return Clock.fixed(DEFAULT_INSTANT, ZoneOffset.UTC);
    }
}
