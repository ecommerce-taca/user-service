package com.ecommerce.authuser.kyc.exception;

public class AdminKycPermissionDeniedException extends RuntimeException {

    public AdminKycPermissionDeniedException() {
        super("Admin does not have required KYC permission");
    }
}