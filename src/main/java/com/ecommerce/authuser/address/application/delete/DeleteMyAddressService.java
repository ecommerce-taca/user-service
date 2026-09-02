package com.ecommerce.authuser.address.application.delete;

import com.ecommerce.authuser.address.domain.Address;
import com.ecommerce.authuser.address.exception.AddressDefaultRequiredException;
import com.ecommerce.authuser.address.exception.AddressNotFoundException;
import com.ecommerce.authuser.address.repository.AddressRepository;

import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.user.exception.profile.UserNotFoundException;
import com.ecommerce.authuser.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeleteMyAddressService {

    private final UserRepository userRepository;

    private final AddressRepository addressRepository;

    @Transactional
    public void delete(DeleteMyAddressCommand command) {
        if (command == null
                || command.userId() == null
                || command.addressId() == null) {

            throw new AddressNotFoundException();
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

        long liveCount = addressRepository
                .countByUser_IdAndDeletedAtIsNull(user.getId());

        if (liveCount <= 1) {
            throw new AddressDefaultRequiredException();
        }

        address.softDelete();

        addressRepository.flush();

        repairDefaultInvariant(user);

        addressRepository.flush();
    }

    private void repairDefaultInvariant(User user) {
        List<Address> currentDefaults = addressRepository
                .findAllByUser_IdAndDefaultAddressTrueAndDeletedAtIsNull(
                        user.getId()
                );

        if (currentDefaults.size() == 1) {
            return;
        }

        Address replacement = addressRepository
                .findFirstByUser_IdAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(
                        user.getId()
                )
                .orElseThrow(AddressDefaultRequiredException::new);

        currentDefaults.forEach(Address::clearDefault);

        replacement.markAsDefault();
    }
}
