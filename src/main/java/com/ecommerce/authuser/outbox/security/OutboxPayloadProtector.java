package com.ecommerce.authuser.outbox.security;

import java.util.Map;

public interface OutboxPayloadProtector {

    Map<String, Object> protect(
            String context,
            Map<String, Object> payload
    );

    Map<String, Object> unprotect(
            String context,
            Map<String, Object> protectedPayload
    );
}
