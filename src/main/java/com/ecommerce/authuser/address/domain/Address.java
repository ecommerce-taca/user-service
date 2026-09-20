package com.ecommerce.authuser.address.domain;

import com.ecommerce.authuser.common.id.UuidV7Generator;
import com.ecommerce.authuser.common.persistence.BooleanToTinyIntConverter;
import com.ecommerce.authuser.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "addresses")
@Getter
public class Address {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "recipient", nullable = false, length = 120)
    private String recipient;

    @Column(name = "phone", nullable = false, length = 16)
    private String phone;

    @Column(name = "line1", nullable = false, length = 255)
    private String line1;

    @Column(name = "line2", length = 255)
    private String line2;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "province_code", length = 2)
    private String provinceCode;

    @Column(name = "ward_code", length = 5)
    private String wardCode;

    @Column(name = "ward", nullable = false, length = 120)
    private String ward;

    @Column(name = "district", length = 120)
    private String district;

    @Column(name = "province", nullable = false, length = 120)
    private String province;

    @Column(name = "postal_code", length = 12)
    private String postalCode;

    @Convert(converter = BooleanToTinyIntConverter.class)
    @Column(name = "is_default", nullable = false)
    private Boolean defaultAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Address() {
    }

    public static Address create(
            User user,
            String recipient,
            String phone,
            String line1,
            String line2,
            String countryCode,
            String provinceCode,
            String province,
            String wardCode,
            String ward,
            String district,
            String postalCode,
            boolean defaultAddress
    ) {
        Address address = new Address();

        address.id = UuidV7Generator.generate();
        address.user = user;
        address.recipient = recipient;
        address.phone = phone;
        address.line1 = line1;
        address.line2 = line2;
        address.countryCode = countryCode;
        address.provinceCode = provinceCode;
        address.province = province;
        address.wardCode = wardCode;
        address.ward = ward;
        address.district = district;
        address.postalCode = postalCode;
        address.defaultAddress = defaultAddress;

        return address;
    }

    public void updateDetails(
            String recipient,
            String phone,
            String line1,
            String line2,
            String countryCode,
            String provinceCode,
            String province,
            String wardCode,
            String ward,
            String district,
            String postalCode
    ) {

        this.recipient = Objects.requireNonNull(recipient);
        this.phone = Objects.requireNonNull(phone);
        this.line1 = Objects.requireNonNull(line1);
        this.line2 = line2;
        this.countryCode = Objects.requireNonNull(countryCode);
        this.provinceCode = Objects.requireNonNull(provinceCode);
        this.province = Objects.requireNonNull(province);
        this.wardCode = Objects.requireNonNull(wardCode);
        this.ward = Objects.requireNonNull(ward);
        this.district = district;
        this.postalCode = postalCode;
    }

    @PrePersist
    private void prePersist() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (defaultAddress == null) {
            defaultAddress = false;
        }
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }

    public void markAsDefault() {
        this.defaultAddress = true;
    }

    public void clearDefault() {
        this.defaultAddress = false;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();

        this.defaultAddress = false;
    }
}
