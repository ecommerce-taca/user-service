package com.ecommerce.authuser.address.application.list;


import com.ecommerce.authuser.address.domain.Address;
import com.ecommerce.authuser.address.exception.InvalidAddressQueryException;
import com.ecommerce.authuser.address.repository.AddressRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ListMyAddressesService {

    private static final int DEFAULT_PAGE = 1;

    private static final int DEFAULT_SIZE = 20;

    private static final int MAX_SIZE = 100;

    private static final String DEFAULT_SORT = "created_at,desc";

    private final AddressRepository addressRepository;

    @Transactional(readOnly = true)
    public ListMyAddressesResult list(ListMyAddressesQuery query) {

        Objects.requireNonNull(
                query,
                "query must not be null"
        );

        Objects.requireNonNull(
                query.userId(),
                "userId must not be null"
        );

        int page = resolvePage(query.page());

        int size = resolveSize(query.size());

        Sort sort = resolveSort(query.sort());

        Pageable pageable = PageRequest.of(
                page - 1,
                size,
                sort
        );

        Page<Address> resultPage = addressRepository
                .findAllByUser_IdAndDeletedAtIsNull(
                        query.userId(),
                        pageable
                );

        List<AddressResult> items = resultPage
                .getContent()
                .stream()
                .map(this::toResult)
                .toList();

        return new ListMyAddressesResult(
                items,
                page,
                size,
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    private int resolvePage(Integer value) {
        if (value == null) {
            return DEFAULT_PAGE;
        }

        if (value < 1) {
            throw new InvalidAddressQueryException();
        }

        return value;
    }

    private int resolveSize(Integer value) {

        if (value == null) {
            return DEFAULT_SIZE;
        }

        if (value < 1 || value > MAX_SIZE) {
            throw new InvalidAddressQueryException();
        }

        return value;
    }

    private Sort resolveSort(String value) {

        String raw = value == null ? DEFAULT_SORT : value.trim();

        if (raw.isEmpty()) {
            throw new InvalidAddressQueryException();
        }

        String[] parts = raw.split(",", -1);

        if (parts.length != 2) {
            throw new InvalidAddressQueryException();
        }

        String field = parts[0]
                .trim()
                .toLowerCase(Locale.ROOT);

        String direction = parts[1]
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!field.equals("created_at")) {
            throw new InvalidAddressQueryException();
        }

        Sort.Direction sortDirection =
                switch (direction) {
                    case "asc" -> Sort.Direction.ASC;

                    case "desc" -> Sort.Direction.DESC;

                    default -> throw new InvalidAddressQueryException();
                };

        return Sort.by(
                new Sort.Order(sortDirection, "createdAt"),
                new Sort.Order(sortDirection, "id")
        );
    }

    private AddressResult toResult(Address address) {
        return new AddressResult(
                address.getId(),
                address.getRecipient(),
                address.getPhone(),
                address.getLine1(),
                address.getLine2(),
                address.getWard(),
                address.getDistrict(),
                address.getProvince(),
                address.getPostalCode(),
                Boolean.TRUE.equals(
                        address.getDefaultAddress()
                ),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}