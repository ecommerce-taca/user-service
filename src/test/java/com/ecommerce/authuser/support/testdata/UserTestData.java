package com.ecommerce.authuser.support.testdata;

import com.ecommerce.authuser.support.builder.UserTestBuilder;
import com.ecommerce.authuser.user.domain.User;
import java.time.LocalDate;

public final class UserTestData {
        private UserTestData() {
        }

        public static User defaultUser() {
                return UserTestBuilder.aUser()
                                .withEmail("user@test.com")
                                .withEmailNormalized("user@test.com")
                                .withFullName("Test User")
                                .build();
        }

        public static User buyer() {
                return UserTestBuilder.buyer()
                                .build();
        }

        public static User user(
                        String email,
                        String fullName) {
                return UserTestBuilder.aUser()
                                .withEmail(email)
                                .withEmailNormalized(email)
                                .withFullName(fullName)
                                .build();
        }

        public static User userWithPhone(
                        String email,
                        String phone,
                        String fullName) {
                return UserTestBuilder.aUser()
                                .withEmail(email)
                                .withEmailNormalized(email)
                                .withPhone(phone)
                                .withPhoneNormalized(phone)
                                .withFullName(fullName)
                                .build();
        }

        public static User userWithDateOfBirth(
                        String email,
                        String fullName,
                        LocalDate dateOfBirth) {
                return UserTestBuilder.aUser()
                                .withEmail(email)
                                .withEmailNormalized(email)
                                .withFullName(fullName)
                                .withDateOfBirth(dateOfBirth)
                                .build();
        }

        public static User userWithPasswordHash(
                        String email,
                        String fullName,
                        String passwordHash) {
                return UserTestBuilder.aUser()
                                .withEmail(email)
                                .withEmailNormalized(email)
                                .withFullName(fullName)
                                .withPasswordHash(passwordHash)
                                .build();
        }
}
