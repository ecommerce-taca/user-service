package com.ecommerce.authuser.outbox.application;

import com.ecommerce.authuser.outbox.infrastructure.kafka.KafkaTopicProperties;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxTopicResolverContractCoverageTest {

    private static final Set<String> DOCUMENTED_NOTIFICATION_COMMAND_TYPES = Set.of(
            "AUTH_VERIFICATION_REQUESTED",
            "PASSWORD_RESET_REQUESTED",
            "PHONE_OTP_REQUESTED"
    );

    private static final Set<String> DOCUMENTED_USER_EVENT_TYPES = Set.of(
            "user.created",
            "user.email_verified",
            "user.password_changed",
            "user.role_changed",
            "user.status_changed",
            "user.updated"
    );

    private static final Set<String> DOCUMENTED_SHOP_EVENT_TYPES = Set.of(
            "shop.created",
            "shop.updated",
            "shop.status_changed",
            "shop.kyc.submitted",
            "shop.kyc.approved",
            "shop.kyc.rejected",
            "shop.kyc.needs_info",
            "shop.kyc.expired"
    );

    private final OutboxTopicResolver resolver = new OutboxTopicResolver(
            topicProperties()
    );

    @Test
    void notificationCommandTypes_shouldBeDocumentedAndRoutedToNotificationTopic() {
        assertThat(OutboxTopicResolver.NOTIFICATION_COMMAND_TYPES)
                .containsExactlyInAnyOrderElementsOf(
                        DOCUMENTED_NOTIFICATION_COMMAND_TYPES
                );

        for (String eventType : DOCUMENTED_NOTIFICATION_COMMAND_TYPES) {
            assertThat(resolver.resolveTopic(eventType))
                    .isEqualTo("notification.commands.v1");

            assertThat(resolver.isNotificationCommand(eventType))
                    .isTrue();
        }
    }

    @Test
    void userEventTypes_shouldBeDocumentedAndRoutedToUserTopic() {
        assertThat(OutboxTopicResolver.USER_EVENT_TYPES)
                .containsExactlyInAnyOrderElementsOf(
                        DOCUMENTED_USER_EVENT_TYPES
                );

        for (String eventType : DOCUMENTED_USER_EVENT_TYPES) {
            assertThat(resolver.resolveTopic(eventType))
                    .isEqualTo("user.events.v1");

            assertThat(resolver.isNotificationCommand(eventType))
                    .isFalse();
        }
    }

    @Test
    void shopEventTypes_shouldBeDocumentedAndRoutedToShopTopic() {
        assertThat(OutboxTopicResolver.SHOP_EVENT_TYPES)
                .containsExactlyInAnyOrderElementsOf(
                        DOCUMENTED_SHOP_EVENT_TYPES
                );

        for (String eventType : DOCUMENTED_SHOP_EVENT_TYPES) {
            assertThat(resolver.resolveTopic(eventType))
                    .isEqualTo("shop.events.v1");

            assertThat(resolver.isNotificationCommand(eventType))
                    .isFalse();
        }
    }

    @Test
    void everyDocumentedEventType_shouldResolveToOneTopic() {
        Set<String> documentedEventTypes = new java.util.LinkedHashSet<>();

        documentedEventTypes.addAll(DOCUMENTED_NOTIFICATION_COMMAND_TYPES);
        documentedEventTypes.addAll(DOCUMENTED_USER_EVENT_TYPES);
        documentedEventTypes.addAll(DOCUMENTED_SHOP_EVENT_TYPES);

        assertThat(documentedEventTypes)
                .hasSize(
                        DOCUMENTED_NOTIFICATION_COMMAND_TYPES.size()
                                + DOCUMENTED_USER_EVENT_TYPES.size()
                                + DOCUMENTED_SHOP_EVENT_TYPES.size()
                );

        for (String eventType : documentedEventTypes) {
            assertThat(resolver.resolveTopic(eventType))
                    .isNotBlank();
        }
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