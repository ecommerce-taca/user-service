package com.ecommerce.authuser.outbox.infrastructure.kafka;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KafkaTopicProvisioningGuardTest {

    private final KafkaTopicNamesClient topicNamesClient =
            mock(KafkaTopicNamesClient.class);

    @Test
    void afterSingletonsInstantiated_shouldPassWhenAllRequiredTopicsExist() {
        when(topicNamesClient.listTopicNames(any(Duration.class)))
                .thenReturn(Set.of(
                        "notification.commands.v1",
                        "user.events.v1",
                        "shop.events.v1",
                        "auth-user.events.dlq.v1",
                        "other.topic"
                ));

        KafkaTopicProvisioningGuard guard = new KafkaTopicProvisioningGuard(
                topics(),
                enabledProperties(),
                topicNamesClient
        );

        guard.afterSingletonsInstantiated();

        verify(topicNamesClient).listTopicNames(Duration.ofSeconds(10));
    }

    @Test
    void afterSingletonsInstantiated_shouldFailWhenRequiredTopicIsMissing() {
        when(topicNamesClient.listTopicNames(any(Duration.class)))
                .thenReturn(Set.of(
                        "notification.commands.v1",
                        "user.events.v1",
                        "auth-user.events.dlq.v1"
                ));

        KafkaTopicProvisioningGuard guard = new KafkaTopicProvisioningGuard(
                topics(),
                enabledProperties(),
                topicNamesClient
        );

        assertThatThrownBy(guard::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing required Kafka topic(s):")
                .hasMessageContaining("shop.events.v1");
    }

    @Test
    void afterSingletonsInstantiated_shouldSkipWhenGuardDisabled() {
        KafkaTopicProvisioningProperties properties =
                enabledProperties();
        properties.setEnabled(false);

        KafkaTopicProvisioningGuard guard = new KafkaTopicProvisioningGuard(
                topics(),
                properties,
                topicNamesClient
        );

        guard.afterSingletonsInstantiated();

        verifyNoInteractions(topicNamesClient);
    }

    private static KafkaTopicProperties topics() {
        KafkaTopicProperties properties = new KafkaTopicProperties();
        properties.setNotificationCommands("notification.commands.v1");
        properties.setUserEvents("user.events.v1");
        properties.setShopEvents("shop.events.v1");
        properties.setDlq("auth-user.events.dlq.v1");
        return properties;
    }

    private static KafkaTopicProvisioningProperties enabledProperties() {
        KafkaTopicProvisioningProperties properties =
                new KafkaTopicProvisioningProperties();
        properties.setEnabled(true);
        properties.setTimeoutSeconds(10);
        return properties;
    }
}