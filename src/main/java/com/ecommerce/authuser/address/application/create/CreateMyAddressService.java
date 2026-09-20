package com.ecommerce.authuser.address.application.create;

import com.ecommerce.authuser.address.domain.Address;
import com.ecommerce.authuser.address.exception.AddressLimitReachedException;
import com.ecommerce.authuser.address.exception.InvalidAddressInputException;
import com.ecommerce.authuser.address.repository.AddressRepository;

import com.ecommerce.authuser.auth.application.support.IdentityNormalizer;
import com.ecommerce.authuser.auth.exception.verification.phone.InvalidPhoneFormatException;

import com.ecommerce.authuser.location.application.VietnamLocationCatalog;

import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.exception.profile.UserNotFoundException;
import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateMyAddressService {

    private static final long MAX_ADDRESSES = 20L;

    private final UserRepository userRepository;

    private final AddressRepository addressRepository;

    private final IdentityNormalizer identityNormalizer;

    private final VietnamLocationCatalog locationCatalog;

    @Transactional
    public CreateMyAddressResult create(CreateMyAddressCommand command) {

        if (command == null || command.userId() == null) {
            throw new InvalidAddressInputException();
        }

        User user = userRepository
                .findByIdForUpdate(command.userId())
                .orElseThrow(UserNotFoundException::new);

        String recipient = requiredText(command.recipient(), 120);

        String phone = normalizePhone(command.phone());

        String line1 = requiredText(command.line1(), 255);

        String line2 = optionalText(command.line2(), 255);

        VietnamLocationCatalog.ResolvedLocation location =
                locationCatalog.resolve(
                        command.countryCode(),
                        command.provinceCode(),
                        command.wardCode()
                );

        String postalCode = optionalText(command.postalCode(), 12);

        long liveCount = addressRepository
                .countByUser_IdAndDeletedAtIsNull(user.getId());

        if (liveCount >= MAX_ADDRESSES) {
            throw new AddressLimitReachedException();
        }

        List<Address> currentDefaults =
                liveCount == 0
                        ? List.of()
                        : addressRepository
                        .findAllByUser_IdAndDefaultAddressTrueAndDeletedAtIsNull(
                                user.getId()
                        );

        boolean shouldBeDefault = liveCount == 0
                || command.defaultRequested()
                || currentDefaults.isEmpty();

        if (shouldBeDefault) {
            currentDefaults.forEach(
                    Address::clearDefault
            );
        }

        Address address =
                Address.create(
                        user,
                        recipient,
                        phone,
                        line1,
                        line2,
                        location.countryCode(),
                        location.provinceCode(),
                        location.provinceName(),
                        location.wardCode(),
                        location.wardName(),
                        null,
                        postalCode,
                        shouldBeDefault
                );

        addressRepository.saveAndFlush(
                address
        );

        return new CreateMyAddressResult(
                address.getId(),
                address.getRecipient(),
                address.getPhone(),
                address.getLine1(),
                address.getLine2(),
                address.getCountryCode(),
                address.getProvinceCode(),
                address.getProvince(),
                address.getWardCode(),
                address.getWard(),
                address.getDistrict(),
                address.getPostalCode(),
                Boolean.TRUE.equals(address.getDefaultAddress()),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }

    private String normalizePhone(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidAddressInputException();
        }

        try {
            return identityNormalizer.normalizePhone(value);

        } catch (InvalidPhoneFormatException ex) {
            throw new InvalidAddressInputException();
        }
    }

    private String requiredText(
            String value,
            int maxLength
    ) {

        if (value == null) {
            throw new InvalidAddressInputException();
        }

        String normalized = value.strip();

        int length =
                normalized.codePointCount(0, normalized.length());

        if (length < 1 || length > maxLength) {
            throw new InvalidAddressInputException();
        }

        return normalized;
    }

    private String optionalText(
            String value,
            int maxLength
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.strip();

        if (normalized.isEmpty()) {
            return null;
        }

        int length = normalized.codePointCount(0, normalized.length());

        if (length > maxLength) {
            throw new InvalidAddressInputException();
        }

        return normalized;
    }
}
