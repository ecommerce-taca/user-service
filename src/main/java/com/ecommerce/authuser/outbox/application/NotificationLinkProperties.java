package com.ecommerce.authuser.outbox.application;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "auth.notification-links")
public class NotificationLinkProperties {

    @NotBlank
    private String publicBaseUrl;

    @NotBlank
    private String emailVerificationPath = "/verify";

    @NotBlank
    private String passwordResetPath = "/reset-password";

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String getEmailVerificationPath() {
        return emailVerificationPath;
    }

    public void setEmailVerificationPath(String emailVerificationPath) {
        this.emailVerificationPath = emailVerificationPath;
    }

    public String getPasswordResetPath() {
        return passwordResetPath;
    }

    public void setPasswordResetPath(String passwordResetPath) {
        this.passwordResetPath = passwordResetPath;
    }
}