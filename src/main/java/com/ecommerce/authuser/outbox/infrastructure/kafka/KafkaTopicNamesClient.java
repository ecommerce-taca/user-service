package com.ecommerce.authuser.outbox.infrastructure.kafka;

import java.time.Duration;
import java.util.Set;

public interface KafkaTopicNamesClient {

    Set<String> listTopicNames(Duration timeout);
}