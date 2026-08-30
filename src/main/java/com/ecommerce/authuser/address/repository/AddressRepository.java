package com.ecommerce.authuser.address.repository;

import com.ecommerce.authuser.address.domain.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findAllByUser_IdAndDeletedAtIsNullOrderByUpdatedAtDesc(UUID userId);

    Optional<Address> findByIdAndUser_IdAndDeletedAtIsNull(
            UUID addressId,
            UUID userId
    );

    Optional<Address> findByUser_IdAndDefaultAddressTrueAndDeletedAtIsNull(UUID userId);

    long countByUser_IdAndDeletedAtIsNull(UUID userId);
}
