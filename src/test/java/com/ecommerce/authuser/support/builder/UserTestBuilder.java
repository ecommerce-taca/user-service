package com.ecommerce.authuser.support.builder;


import com.ecommerce.authuser.user.domain.User;
import java.time.LocalDate;
import com.ecommerce.authuser.support.testdata.UserTestData;

public final class UserTestBuilder {

    /*
     * BCrypt hash dùng cho test.
     * Plain-text password tương ứng được quy ước trong UserTestData.
     */
    private String passwordHash = UserTestData.DEFAULT_PASSWORD;

    private String email =
            "user@test.com";

    private String emailNormalized =
            "user@test.com";

    private String phone;

    private String phoneNormalized;

    private String fullName =
            "Test User";

    private LocalDate dateOfBirth;

    private UserTestBuilder() {
    }

    public static UserTestBuilder aUser() {
        return new UserTestBuilder();
    }

    public static UserTestBuilder activeUser() {
        return new UserTestBuilder();
    }

    public static UserTestBuilder buyer() {
        return new UserTestBuilder()
                .withEmail("buyer@test.com")
                .withEmailNormalized("buyer@test.com")
                .withFullName("Test Buyer");
    }

    public UserTestBuilder withEmail(
            String email
    ) {
        this.email = email;
        return this;
    }

    public UserTestBuilder withEmailNormalized(
            String emailNormalized
    ) {
        this.emailNormalized = emailNormalized;
        return this;
    }

    public UserTestBuilder withPhone(
            String phone
    ) {
        this.phone = phone;
        return this;
    }

    public UserTestBuilder withPhoneNormalized(
            String phoneNormalized
    ) {
        this.phoneNormalized = phoneNormalized;
        return this;
    }

    public UserTestBuilder withPasswordHash(
            String passwordHash
    ) {
        this.passwordHash = passwordHash;
        return this;
    }

    public UserTestBuilder withFullName(
            String fullName
    ) {
        this.fullName = fullName;
        return this;
    }

    public UserTestBuilder withDateOfBirth(
            LocalDate dateOfBirth
    ) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    public User build() {
        return User.create(
                email,
                emailNormalized,
                phone,
                phoneNormalized,
                passwordHash,
                fullName,
                dateOfBirth
        );
    }
}

