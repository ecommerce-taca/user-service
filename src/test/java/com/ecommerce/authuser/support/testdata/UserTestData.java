package com.ecommerce.authuser.support.testdata;

import com.ecommerce.authuser.support.builder.UserTestBuilder;
import com.ecommerce.authuser.user.domain.User;

import java.time.LocalDate;

public final class UserTestData {

    public static final String DEFAULT_PASSWORD =
            "TestPassword123!";

    public static final String DEFAULT_EMAIL =
            "user@test.com";

    public static final String DEFAULT_PHONE =
            "0901234567";

    private UserTestData() {
    }

    public static UserTestBuilder defaultUser() {
        return UserTestBuilder.aUser()
                .withEmail(DEFAULT_EMAIL)
                .withEmailNormalized(DEFAULT_EMAIL)
                .withFullName("Test User");
    }

    public static UserTestBuilder activeUser() {
        return UserTestBuilder.activeUser()
                .withEmail(DEFAULT_EMAIL)
                .withEmailNormalized(DEFAULT_EMAIL)
                .withFullName("Test User");
    }

    public static UserTestBuilder buyer() {
        return UserTestBuilder.buyer()
                .withFullName("Test Buyer");
    }

    public static UserTestBuilder userWithPhone() {
        return UserTestBuilder.aUser()
                .withEmail("phone-user@test.com")
                .withEmailNormalized("phone-user@test.com")
                .withPhone(DEFAULT_PHONE)
                .withPhoneNormalized(DEFAULT_PHONE)
                .withFullName("Phone Test User");
    }

    public static UserTestBuilder userWithDateOfBirth() {
        return UserTestBuilder.aUser()
                .withEmail("dob-user@test.com")
                .withEmailNormalized("dob-user@test.com")
                .withFullName("Date Of Birth User")
                .withDateOfBirth(
                        LocalDate.of(2000, 1, 15)
                );
    }

    public static UserTestBuilder user(
            String email,
            String fullName
    ) {
        return UserTestBuilder.aUser()
                .withEmail(email)
                .withEmailNormalized(email)
                .withFullName(fullName);
    }
}


