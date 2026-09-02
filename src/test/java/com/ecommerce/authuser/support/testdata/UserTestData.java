package com.ecommerce.authuser.support.testdata;

import com.ecommerce.authuser.support.builder.UserTestBuilder;
import com.ecommerce.authuser.user.domain.User;

import java.time.LocalDate;

public final class UserTestData {

    /*
     * Plain-text password dùng cho integration test signin.
     *
     * Giá trị này phải tương ứng chính xác với DEFAULT_PASSWORD_HASH.
     */
    public static final String DEFAULT_PASSWORD =
            "test-password";

    public static final String DEFAULT_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoO6W0m1Y6fQm1M0Q6M4q7YQ4J8JxV7Q5K";

    public static final String DEFAULT_EMAIL =
            "user@test.com";

    public static final String DEFAULT_FULL_NAME =
            "Test User";

    private UserTestData() {
    }

    public static User defaultUser() {
        return UserTestBuilder.aUser()
                .withEmail(DEFAULT_EMAIL)
                .withEmailNormalized(DEFAULT_EMAIL)
                .withFullName(DEFAULT_FULL_NAME)
                .withPasswordHash(DEFAULT_PASSWORD_HASH)
                .build();
    }

    public static User buyer() {
        return UserTestBuilder.buyer()
                .withPasswordHash(DEFAULT_PASSWORD_HASH)
                .build();
    }

    public static User user(
            String email,
            String fullName
    ) {
        return UserTestBuilder.aUser()
                .withEmail(email)
                .withEmailNormalized(email)
                .withFullName(fullName)
                .withPasswordHash(DEFAULT_PASSWORD_HASH)
                .build();
    }

    public static User userWithPhone(
            String email,
            String phone,
            String fullName
    ) {
        return UserTestBuilder.aUser()
                .withEmail(email)
                .withEmailNormalized(email)
                .withPhone(phone)
                .withPhoneNormalized(phone)
                .withFullName(fullName)
                .withPasswordHash(DEFAULT_PASSWORD_HASH)
                .build();
    }

    public static User userWithDateOfBirth(
            String email,
            String fullName,
            LocalDate dateOfBirth
    ) {
        return UserTestBuilder.aUser()
                .withEmail(email)
                .withEmailNormalized(email)
                .withFullName(fullName)
                .withDateOfBirth(dateOfBirth)
                .withPasswordHash(DEFAULT_PASSWORD_HASH)
                .build();
    }

    public static User userWithPasswordHash(
            String email,
            String fullName,
            String passwordHash
    ) {
        return UserTestBuilder.aUser()
                .withEmail(email)
                .withEmailNormalized(email)
                .withFullName(fullName)
                .withPasswordHash(passwordHash)
                .build();
    }
}
