package com.ecommerce.authuser.outbox.infrastructure.kafka;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class KafkaTopicProvisioningGuard implements SmartInitializingSingleton {

    private final KafkaTopicProperties topics;

    private final KafkaTopicProvisioningProperties properties;

    private final KafkaTopicNamesClient topicNamesClient;

    public KafkaTopicProvisioningGuard(
            KafkaTopicProperties topics,
            KafkaTopicProvisioningProperties properties,
            KafkaTopicNamesClient topicNamesClient
    ) {
        this.topics = topics;
        this.properties = properties;
        this.topicNamesClient = topicNamesClient;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!properties.isEnabled()) {
            return;
        }

        Set<String> requiredTopics = requiredTopics();

        Set<String> existingTopics = topicNamesClient.listTopicNames(
                Duration.ofSeconds(properties.getTimeoutSeconds())
        );

        Set<String> missingTopics = new LinkedHashSet<>(requiredTopics);
        missingTopics.removeAll(existingTopics);

        if (!missingTopics.isEmpty()) {
            throw new IllegalStateException(
                    "Missing required Kafka topic(s): " + missingTopics
            );
        }
    }

    private Set<String> requiredTopics() {
        return Set.of(
                topics.getNotificationCommands(),
                topics.getUserEvents(),
                topics.getShopEvents(),
                topics.getDlq()
        );
    }
}