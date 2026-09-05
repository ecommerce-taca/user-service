package com.ecommerce.authuser.kyc.exception;

public class KycDecisionConflictException extends RuntimeException {

    public KycDecisionConflictException() {
        super("KYC case cannot receive this decision");
    }
}