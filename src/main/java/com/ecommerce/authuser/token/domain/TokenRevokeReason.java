package com.ecommerce.authuser.token.domain;

public enum TokenRevokeReason {

    ROTATED,
    SIGNOUT,
    RESET,
    REUSE,
    SUSPEND,
    EXPIRED
}
