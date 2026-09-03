package com.ecommerce.authuser.shop.application.onboarding.bank;

public record UpdateSellerBankResult(
        String bankName,
        String maskedAccount,
        boolean verified
) {
}