package com.ecommerce.authuser.outbox.infrastructure.kafka;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "auth.kafka.topics")
public class KafkaTopicProperties {

    @NotBlank
    private String notificationCommands;

    @NotBlank
    private String userEvents;

    @NotBlank
    private String shopEvents;

    @NotBlank
    private String dlq;

    public String getNotificationCommands() {
        return notificationCommands;
    }

    public void setNotificationCommands(String notificationCommands) {
        this.notificationCommands = notificationCommands;
    }

    public String getUserEvents() {
        return userEvents;
    }

    public void setUserEvents(String userEvents) {
        this.userEvents = userEvents;
    }

    public String getShopEvents() {
        return shopEvents;
    }

    public void setShopEvents(String shopEvents) {
        this.shopEvents = shopEvents;
    }

    public String getDlq() {
        return dlq;
    }

    public void setDlq(String dlq) {
        this.dlq = dlq;
    }
}