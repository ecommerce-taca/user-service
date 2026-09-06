package com.ecommerce.authuser.shop.application.onboarding.bank;

import com.ecommerce.authuser.rbac.domain.RbacKeys;
import com.ecommerce.authuser.rbac.repository.UserRoleRepository;

import com.ecommerce.authuser.shop.domain.SellerOnboarding;
import com.ecommerce.authuser.shop.domain.Shop;

import com.ecommerce.authuser.shop.exception.BankAccountInvalidException;
import com.ecommerce.authuser.shop.exception.InvalidSellerBankException;
import com.ecommerce.authuser.shop.exception.SellerPermissionDeniedException;
import com.ecommerce.authuser.shop.exception.ShopInvalidStateException;
import com.ecommerce.authuser.shop.exception.ShopNotFoundException;

import com.ecommerce.authuser.shop.port.BankAccountCryptoPort;
import com.ecommerce.authuser.shop.port.BankCatalogPort;

import com.ecommerce.authuser.shop.repository.SellerOnboardingRepository;
import com.ecommerce.authuser.shop.repository.ShopRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UpdateSellerBankService {

    private final ShopRepository shopRepository;

    private final SellerOnboardingRepository sellerOnboardingRepository;

    private final UserRoleRepository userRoleRepository;

    private final BankCatalogPort bankCatalogPort;

    private final BankAccountCryptoPort bankAccountCryptoPort;

    @Transactional
    public UpdateSellerBankResult update(UpdateSellerBankCommand command) {

        if (command == null || command.userId() == null) {

            throw new InvalidSellerBankException();
        }

        String bankCode = normalizeBankCode(command.bankCode());

        String requestedBankName =
                requiredText(command.bankName(), 120);

        String accountName =
                normalizeAccountName(command.accountName());

        String accountNumber =
                normalizeAccountNumber(command.accountNumber());

        if (!command.confirmAccountName()) {
            throw new BankAccountInvalidException();
        }

        Shop shop = shopRepository
                .findByOwnerIdForUpdate(command.userId())
                .orElseThrow(ShopNotFoundException::new);

        boolean sellerOwner = userRoleRepository
                .existsByUser_IdAndRole_RoleKeyAndShop_IdAndRevokedAtIsNull(
                        command.userId(),
                        RbacKeys.Roles.SELLER,
                        shop.getId()
                );

        if (!sellerOwner) {
            throw new SellerPermissionDeniedException();
        }

        if (!shop.canEditOnboarding()) {
            throw new ShopInvalidStateException();
        }

        SellerOnboarding onboarding = sellerOnboardingRepository
                .findByShopIdForUpdate(shop.getId())
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Seller onboarding not found for shop"
                        )
                );

        BankCatalogPort.BankInfo bankInfo = bankCatalogPort
                .findByCode(bankCode)
                .orElseThrow(BankAccountInvalidException::new);

        if (!requestedBankName.equalsIgnoreCase(
                bankInfo.name()
        )) {

            throw new BankAccountInvalidException();
        }

        String last4 =
                accountNumber.substring(accountNumber.length() - 4);


        BankAccountCryptoPort.EncryptionResult
                encryptionResult = bankAccountCryptoPort.encrypt(accountNumber);

        try {
            shop.updateBankAccount(
                    bankInfo.code(),
                    bankInfo.name(),
                    accountName,
                    last4,
                    encryptionResult.ciphertext(),
                    encryptionResult.keyVersion(),

                    null
            );

        } catch (IllegalArgumentException ex) {
            throw new InvalidSellerBankException();

        } catch (IllegalStateException ex) {
            throw new ShopInvalidStateException();
        }

        onboarding.completeBankStep();

        shopRepository.saveAndFlush(shop);

        sellerOnboardingRepository.saveAndFlush(onboarding);

        return new UpdateSellerBankResult(
                shop.getBankName(),
                maskAccount(shop.getBankAccountLast4()),
                shop.getBankVerifiedAt() != null
        );
    }

    private String normalizeBankCode(String value) {
        String normalized =
                requiredText(value, 32)
                        .toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z0-9_\\-]+")) {
            throw new InvalidSellerBankException();
        }

        return normalized;
    }

    private String normalizeAccountName(String value) {

        String normalized = requiredText(value, 120);

        return normalized.toUpperCase(
                Locale.ROOT
        );
    }

    private String normalizeAccountNumber(String value) {
        if (value == null) {
            throw new InvalidSellerBankException();
        }

        String normalized = value.strip();

        if (!normalized.matches("\\d{8,20}")) {
            throw new InvalidSellerBankException();
        }

        return normalized;
    }

    private String requiredText(
            String value,
            int maxLength
    ) {

        if (value == null) {
            throw new InvalidSellerBankException();
        }

        String normalized = value.strip();

        int length = normalized.codePointCount(0, normalized.length());

        if (length < 1 || length > maxLength) {
            throw new InvalidSellerBankException();
        }

        return normalized;
    }

    private String maskAccount(String last4) {

        if (last4 == null || !last4.matches("\\d{4}")) {
            throw new IllegalStateException(
                    "Invalid persisted bank account last4"
            );
        }

        return "********" + last4;
    }
}
