package com.ecommerce.authuser.kyc.exception;

public class InvalidKycReviewRequestException extends RuntimeException {

    public InvalidKycReviewRequestException() {
        super("Invalid KYC review request");
    }
}
