package com.ecommerce.authuser.kyc.exception;

public class KycCaseNotFoundException extends RuntimeException {

    public KycCaseNotFoundException() {
        super("KYC case was not found");
    }
}
