package com.ecommerce.authuser.audit.application.admin;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class AdminAuditMetadataMasker {

    private static final String REDACTED = "***";

    private static final Set<String> EXACT_SENSITIVE_KEYS =
            Set.of(
                    "password",
                    "password_hash",

                    "access_token",
                    "refresh_token",
                    "refresh_token_hash",
                    "step_up_token",
                    "token",
                    "token_hash",

                    "otp",
                    "otp_code",
                    "totp_secret",
                    "recovery_code",

                    "authorization",
                    "api_key",
                    "private_key",
                    "client_secret",
                    "secret",
                    "credential",

                    "email",
                    "email_normalized",
                    "phone",
                    "phone_normalized",
                    "tax_code",

                    "bank_account",
                    "bank_account_number",

                    "document_number",
                    "identity_number",
                    "national_id",

                    "signed_url",
                    "document_url",
                    "object_key",

                    "ip",
                    "ip_address",
                    "ip_hash",
                    "user_agent"
            );

    public Map<String, Object> mask(
            Map<String, Object> metadata
    ) {

        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }

        return maskMap(metadata);
    }

    private Map<String, Object> maskMap(
            Map<?, ?> source
    ) {

        Map<String, Object> result =
                new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : source.entrySet()) {

            String key = String.valueOf(entry.getKey());

            if (isSensitiveKey(key)) {

                result.put(key, REDACTED);

                continue;
            }

            result.put(
                    key,
                    maskValue(entry.getValue())
            );
        }

        return Collections.unmodifiableMap(result);
    }

    private Object maskValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Map<?, ?> map) {
            return maskMap(map);
        }

        if (value instanceof Collection<?> collection) {
            List<Object> result = new ArrayList<>(collection.size());

            for (Object item : collection) {
                result.add(maskValue(item));
            }

            return Collections.unmodifiableList(result);
        }
        return value;
    }

    private boolean isSensitiveKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }

        String normalized =
                key.strip()
                        .toLowerCase(Locale.ROOT)
                        .replace('-', '_')
                        .replace('.', '_');

        if (EXACT_SENSITIVE_KEYS.contains(
                normalized
        )) {

            return true;
        }

        if (normalized.contains("password")
                || normalized.contains("totp")
                || normalized.contains("recovery_code")
                || normalized.contains("authorization")
                || normalized.contains("private_key")
                || normalized.contains("client_secret")) {

            return true;
        }

        if (normalized.endsWith("_token")
                || normalized.endsWith("_token_hash")
                || normalized.endsWith("_secret")
                || normalized.endsWith("_credential")) {

            return true;
        }

        if (normalized.endsWith("_email")
                || normalized.endsWith("_phone")
                || normalized.endsWith("_tax_code")
                || normalized.endsWith("_document_number")
                || normalized.endsWith("_identity_number")
                || normalized.endsWith("_national_id")
                || normalized.endsWith("_bank_account")
                || normalized.endsWith("_account_number")) {

            return true;
        }

        return normalized.endsWith("_signed_url")
                || normalized.endsWith("_document_url")
                || normalized.endsWith("_object_key");
    }
}
