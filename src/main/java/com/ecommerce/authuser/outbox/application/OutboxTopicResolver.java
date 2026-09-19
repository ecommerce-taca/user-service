package com.ecommerce.authuser.outbox.application;

import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.infrastructure.kafka.KafkaTopicProperties;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class OutboxTopicResolver {

    public static final Set<String> NOTIFICATION_COMMAND_TYPES = Set.of(
            "AUTH_VERIFICATION_REQUESTED",
            "PASSWORD_RESET_REQUESTED",
            "PHONE_OTP_REQUESTED"
    );

    private final KafkaTopicProperties topics;

    public OutboxTopicResolver(KafkaTopicProperties topics) {
        this.topics = topics;
    }

    public String resolveTopic(OutboxEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("outbox event must not be null");
        }

        return resolveTopic(event.getEventType());
    }

    public String resolveTopic(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("event type must not be blank");
        }

        if (NOTIFICATION_COMMAND_TYPES.contains(eventType)) {
            return topics.getNotificationCommands();
        }

        if (eventType.startsWith("user.")) {
            return topics.getUserEvents();
        }

        if (eventType.startsWith("shop.")) {
            return topics.getShopEvents();
        }

        throw new IllegalArgumentException(
                "Unsupported outbox event type: " + eventType
        );
    }

    public String resolveDlqTopic() {
        return topics.getDlq();
    }

    public boolean isNotificationCommand(String eventType) {
        return NOTIFICATION_COMMAND_TYPES.contains(eventType);
    }
}