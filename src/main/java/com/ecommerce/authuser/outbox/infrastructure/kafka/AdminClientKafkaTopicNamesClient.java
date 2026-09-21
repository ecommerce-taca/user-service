package com.ecommerce.authuser.outbox.infrastructure.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class AdminClientKafkaTopicNamesClient implements KafkaTopicNamesClient {

    private final Environment environment;

    public AdminClientKafkaTopicNamesClient(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Set<String> listTopicNames(Duration timeout) {
        String bootstrapServers = environment.getProperty(
                "spring.kafka.bootstrap-servers"
        );

        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            throw new IllegalStateException(
                    "Missing required Kafka property: spring.kafka.bootstrap-servers"
            );
        }

        Map<String, Object> properties = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        try (AdminClient adminClient = AdminClient.create(properties)) {
            return adminClient
                    .listTopics()
                    .names()
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Cannot list Kafka topics",
                    ex
            );
        }
    }
}