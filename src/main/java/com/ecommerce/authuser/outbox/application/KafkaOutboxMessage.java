package com.ecommerce.authuser.outbox.application;

import java.util.Map;

public interface KafkaOutboxMessage {

    String partitionKey();

    Map<String, String> kafkaHeaders();

    Map<String, Object> toMessageBody();
}
