package com.ecommerce.authuser.user.application.profile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GetMyProfileResult(
        UUID id,
        String fullName,
        String email,
        boolean emailVerified,
        String phone,
        boolean phoneVerified,
        LocalDate dateOfBirth,
        List<String> roles,
        String status,
        UUID defaultShopId,
        Instant createdAt,
        Instant updatedAt
) {
}
