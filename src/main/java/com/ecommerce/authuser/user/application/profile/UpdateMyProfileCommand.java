package com.ecommerce.authuser.user.application.profile;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateMyProfileCommand(
        UUID userId,
        String fullName,
        boolean phoneProvided,
        String phone,
        boolean dateOfBirthProvided,
        LocalDate dateOfBirth
) {
}
