package com.ecommerce.authuser.auth.application;

public record SignupCommand(

        String fullName,
        String email,
        String password,
        String phone
) {
}
