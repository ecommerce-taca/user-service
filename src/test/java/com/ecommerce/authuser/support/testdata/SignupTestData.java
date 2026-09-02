package com.ecommerce.authuser.support.testdata;

import com.ecommerce.authuser.auth.web.SignupRequest;

public final class SignupTestData {

    public static final String DEFAULT_FULL_NAME = "Signup Test User";
    public static final String DEFAULT_EMAIL = "signup@test.com";
    public static final String DEFAULT_PASSWORD = "Password123456";
    public static final String DEFAULT_PHONE = "+84901234567";
    public static final String DUPLICATE_EMAIL = "duplicate@test.com";
    public static final String DUPLICATE_PHONE = "+84909876543";
    public static final String SECOND_EMAIL = "second@test.com";
    public static final String SECOND_PHONE = "+84901112233";

    private SignupTestData() {
    }

    public static SignupRequest defaultRequest() {

        return new SignupRequest(
                DEFAULT_FULL_NAME,
                DEFAULT_EMAIL,
                DEFAULT_PASSWORD,
                DEFAULT_PHONE);
    }

    public static SignupRequest request(
            String fullName,
            String email,
            String password,
            String phone) {

        return new SignupRequest(
                fullName,
                email,
                password,
                phone);
    }

    public static SignupRequest requestWithoutPhone() {

        return new SignupRequest(
                DEFAULT_FULL_NAME,
                DEFAULT_EMAIL,
                DEFAULT_PASSWORD,
                null);
    }

    public static SignupRequest requestWithEmail(
            String email) {

        return new SignupRequest(
                DEFAULT_FULL_NAME,
                email,
                DEFAULT_PASSWORD,
                DEFAULT_PHONE);
    }

    public static SignupRequest requestWithPhone(
            String phone) {

        return new SignupRequest(
                DEFAULT_FULL_NAME,
                DEFAULT_EMAIL,
                DEFAULT_PASSWORD,
                phone);
    }

    public static SignupRequest requestWithCredentials(
            String email,
            String password) {

        return new SignupRequest(
                DEFAULT_FULL_NAME,
                email,
                password,
                DEFAULT_PHONE);
    }
}
