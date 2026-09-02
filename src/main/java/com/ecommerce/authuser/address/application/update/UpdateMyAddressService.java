package com.ecommerce.authuser.address.application.update;

import com.ecommerce.authuser.address.domain.Address;
import com.ecommerce.authuser.address.exception.AddressDefaultRequiredException;
import com.ecommerce.authuser.address.exception.AddressNotFoundException;
import com.ecommerce.authuser.address.exception.InvalidAddressInputException;
import com.ecommerce.authuser.address.repository.AddressRepository;

import com.ecommerce.authuser.auth.application.IdentityNormalizer;
import com.ecommerce.authuser.auth.exception.InvalidPhoneFormatException;

import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.exception.profile.UserNotFoundException;
import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UpdateMyAddressService {

    private final UserRepository userRepository;

    private final AddressRepository addressRepository;

    private final IdentityNormalizer identityNormalizer;

    @Transactional
    public UpdateMyAddressResult update(UpdateMyAddressCommand command) {

        if (command == null
                || command.userId() == null
                || command.addressId() == null) {
            throw new InvalidAddressInputException();
        }

        User user = userRepository
                .findByIdForUpdate(command.userId())
                .orElseThrow(UserNotFoundException::new);

        Address address = addressRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        command.addressId(),
                        user.getId()
                )
                .orElseThrow(AddressNotFoundException::new);

        String recipient = requiredText(command.recipient(), 120);

        String phone = normalizePhone(command.phone());

        String line1 = requiredText(command.line1(), 255);

        String line2 = optionalText(command.line2(), 255);

        String ward = requiredText(command.ward(), 120);

        String district = requiredText(command.district(), 120);

        String province = requiredText(command.province(), 120);

        String postalCode = optionalText(command.postalCode(), 12);

        if (command.defaultProvided()) {
            applyDefaultChange(
                    address,
                    user,
                    command.defaultRequested()
            );
        }

        address.updateDetails(
                recipient,
                phone,
                line1,
                line2,
                ward,
                district,
                province,
                postalCode
        );

        addressRepository.flush();

        return new UpdateMyAddressResult(
                address.getId(),
                address.getRecipient(),
                address.getPhone(),
                address.getLine1(),
                address.getLine2(),
                address.getWard(),
                address.getDistrict(),
                address.getProvince(),
                address.getPostalCode(),
                Boolean.TRUE.equals(address.getDefaultAddress()),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }

    private void applyDefaultChange(
            Address address,
            User user,
            boolean requestedDefault
    ) {
        List<Address> currentDefaults = addressRepository
                .findAllByUser_IdAndDefaultAddressTrueAndDeletedAtIsNull(user.getId());

        if (requestedDefault) {
            currentDefaults
                    .stream()
                    .filter(current ->
                            !current
                                    .getId()
                                    .equals(address.getId())
                    )
                    .forEach(Address::clearDefault);

            address.markAsDefault();

            return;
        }

        if (!Boolean.TRUE.equals(address.getDefaultAddress())) {
            return;
        }

        boolean anotherDefaultExists =
                currentDefaults
                        .stream()
                        .anyMatch(current ->
                                !current
                                        .getId()
                                        .equals(address.getId())
                        );

        if (!anotherDefaultExists) {
            throw new AddressDefaultRequiredException();
        }

        address.clearDefault();
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

        int length = normalized.codePointCount(0, normalized.length());

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
