package com.ecommerce.authuser.address.repository;

import com.ecommerce.authuser.address.domain.Address;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    Page<Address> findAllByUser_IdAndDeletedAtIsNull(
            UUID userId,
            Pageable pageable
    );

    Optional<Address> findByIdAndUser_IdAndDeletedAtIsNull(
            UUID addressId,
            UUID userId
    );

    Optional<Address> findByUser_IdAndDefaultAddressTrueAndDeletedAtIsNull(
            UUID userId
    );

    Optional<Address> findFirstByUser_IdAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(
            UUID userId
    );

    long countByUser_IdAndDeletedAtIsNull(UUID userId);

    List<Address> findAllByUser_IdAndDefaultAddressTrueAndDeletedAtIsNull(
            UUID userId
    );
}
