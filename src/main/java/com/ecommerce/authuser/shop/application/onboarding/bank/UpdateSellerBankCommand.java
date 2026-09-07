package com.ecommerce.authuser.shop.application.onboarding.bank;

import java.util.UUID;

public final class UpdateSellerBankCommand {

    private final UUID userId;

    private final String bankCode;

    private final String bankName;

    private final String accountName;

    private final String accountNumber;

    private final boolean confirmAccountName;

    public UpdateSellerBankCommand(
            UUID userId,
            String bankCode,
            String bankName,
            String accountName,
            String accountNumber,
            boolean confirmAccountName
    ) {
        this.userId = userId;
        this.bankCode = bankCode;
        this.bankName = bankName;
        this.accountName = accountName;
        this.accountNumber = accountNumber;
        this.confirmAccountName = confirmAccountName;
    }

    public UUID userId() {
        return userId;
    }

    public String bankCode() {
        return bankCode;
    }

    public String bankName() {
        return bankName;
    }

    public String accountName() {
        return accountName;
    }

    public String accountNumber() {
        return accountNumber;
    }

    public boolean confirmAccountName() {
        return confirmAccountName;
    }

    @Override
    public String toString() {
        return "UpdateSellerBankCommand{"
                + "userId=" + userId
                + ", bankCode='"
                + bankCode
                + '\''
                + ", bankName='"
                + bankName
                + '\''
                + ", accountName='[REDACTED]'"
                + ", accountNumber='[REDACTED]'"
                + ", confirmAccountName="
                + confirmAccountName
                + '}';
    }
}
