package com.ecommerce.authuser.outbox.infrastructure.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
        prefix = "auth.outbox-publisher",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxSchedulingConfiguration {
}