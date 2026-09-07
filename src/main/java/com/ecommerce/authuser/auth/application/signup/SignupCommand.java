package com.ecommerce.authuser.auth.application.signup;

public record SignupCommand(

        String fullName,
        String email,
        String password,
        String phone
) {
}
