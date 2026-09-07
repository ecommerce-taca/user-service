package com.ecommerce.authuser.user.web.admin.status;

public record AdminUserStatusRequest(
        String status,
        String reason
) {
}
