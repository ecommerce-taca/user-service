package com.ecommerce.authuser.auth.application;

import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class IdentityNormalizer {

    public String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }

        String normalized = phone.trim();

        return normalized.isEmpty() ? null : normalized;
    }
}
