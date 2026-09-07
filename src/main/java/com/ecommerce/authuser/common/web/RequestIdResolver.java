package com.ecommerce.authuser.common.web;

import com.ecommerce.authuser.common.id.UuidV7Generator;

public final class RequestIdResolver {

    private static final int MAX_REQUEST_ID_LENGTH = 64;

    private RequestIdResolver() {
    }

    public static String resolve(String requestId) {
        if (requestId != null
                && !requestId.isBlank()
                && requestId.length() <= MAX_REQUEST_ID_LENGTH) {

            return requestId;
        }

        return UuidV7Generator
                .generate()
                .toString();
    }
}