package com.ecommerce.authuser.auth.application;

import java.util.Locale;
import java.util.regex.Pattern;

import com.ecommerce.authuser.auth.exception.InvalidPhoneFormatException;
import org.springframework.stereotype.Component;

@Component
public class IdentityNormalizer {

    private static final Pattern E164_PHONE =
            Pattern.compile("^\\+[1-9]\\d{7,14}$");

    public String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String normalized = phone.trim();

        if (normalized.isEmpty()) {
            return null;
        }

        if (!E164_PHONE.matcher(normalized).matches()) {
            throw new InvalidPhoneFormatException();
        }

        return normalized;
    }
}
