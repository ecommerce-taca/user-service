package com.ecommerce.authuser.outbox.application;

import com.ecommerce.authuser.outbox.infrastructure.kafka.KafkaTopicProperties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxTopicResolverTest {

    private final OutboxTopicResolver resolver = new OutboxTopicResolver(
            topicProperties()
    );

    @ParameterizedTest
    @ValueSource(strings = {
            "AUTH_VERIFICATION_REQUESTED",
            "PASSWORD_RESET_REQUESTED",
            "PHONE_OTP_REQUESTED"
    })
    void resolveTopic_shouldRouteNotificationCommandsToNotificationTopic(
            String eventType
    ) {
        assertThat(resolver.resolveTopic(eventType))
                .isEqualTo("notification.commands.v1");

        assertThat(resolver.isNotificationCommand(eventType))
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "user.created",
            "user.email_verified",
            "user.password_changed",
            "user.role_changed",
            "user.status_changed",
            "user.updated"
    })
    void resolveTopic_shouldRouteUserEventsToUserTopic(String eventType) {
        assertThat(resolver.resolveTopic(eventType))
                .isEqualTo("user.events.v1");

        assertThat(resolver.isNotificationCommand(eventType))
                .isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "shop.created",
            "shop.updated",
            "shop.status_changed",
            "shop.kyc.submitted",
            "shop.kyc.approved",
            "shop.kyc.rejected",
            "shop.kyc.needs_info",
            "shop.kyc.expired"
    })
    void resolveTopic_shouldRouteShopEventsToShopTopic(String eventType) {
        assertThat(resolver.resolveTopic(eventType))
                .isEqualTo("shop.events.v1");

        assertThat(resolver.isNotificationCommand(eventType))
                .isFalse();
    }

    @Test
    void resolveDlqTopic_shouldReturnDlqTopic() {
        assertThat(resolver.resolveDlqTopic())
                .isEqualTo("auth-user.events.dlq.v1");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "user.unknown",
            "shop.kyc.expried",
            "notification.unknown",
            "ORDER_CREATED"
    })
    void resolveTopic_shouldRejectUnsupportedEventTypes(String eventType) {
        assertThatThrownBy(() -> resolver.resolveTopic(eventType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported outbox event type: " + eventType);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "\t"
    })
    void resolveTopic_shouldRejectBlankEventType(String eventType) {
        assertThatThrownBy(() -> resolver.resolveTopic(eventType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("event type must not be blank");
    }

    @Test
    void resolveTopic_shouldRejectNullEventType() {
        assertThatThrownBy(() -> resolver.resolveTopic((String) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("event type must not be blank");
    }

    private static KafkaTopicProperties topicProperties() {
        KafkaTopicProperties properties = new KafkaTopicProperties();
        properties.setNotificationCommands("notification.commands.v1");
        properties.setUserEvents("user.events.v1");
        properties.setShopEvents("shop.events.v1");
        properties.setDlq("auth-user.events.dlq.v1");
        return properties;
    }
}