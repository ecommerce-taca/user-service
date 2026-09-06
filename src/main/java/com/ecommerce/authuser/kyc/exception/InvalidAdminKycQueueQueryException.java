package com.ecommerce.authuser.kyc.exception;

public class InvalidAdminKycQueueQueryException extends RuntimeException {

    public InvalidAdminKycQueueQueryException() {
        super("Invalid admin KYC queue query");
    }
}
