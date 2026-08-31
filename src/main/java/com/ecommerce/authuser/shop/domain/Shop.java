package com.ecommerce.authuser.shop.domain;

import com.ecommerce.authuser.common.id.UuidV7Generator;
import com.ecommerce.authuser.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "shops",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_shops_slug",
                        columnNames = "slug"
                ),
                @UniqueConstraint(
                        name = "uk_shops_tax_code",
                        columnNames = "tax_code"
                )
        }
)
@Getter
public class Shop {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "slug", nullable = false, length = 160)
    private String slug;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Column(name = "tax_code", length = 20)
    private String taxCode;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "logo_object_key", length = 512)
    private String logoObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ShopStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 16)
    private KycStatus kycStatus;

    @Getter(AccessLevel.NONE)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "warehouse_snapshot", columnDefinition = "JSON")
    private Map<String, Object> warehouseSnapshot;

    @Column(name = "bank_code", length = 32)
    private String bankCode;

    @Column(name = "bank_name", length = 120)
    private String bankName;

    @Column(name = "bank_account_name", length = 120)
    private String bankAccountName;

    @Column(name = "bank_account_last4", length = 4, columnDefinition = "CHAR(4)")
    private String bankAccountLast4;

    @Getter(AccessLevel.NONE)
    @Column(name = "bank_account_ciphertext", columnDefinition = "VARBINARY(2048)")
    private byte[] bankAccountCiphertext;

    @Column(name = "bank_key_version", length = 32)
    private String bankKeyVersion;

    @Column(name = "bank_verified_at")
    private Instant bankVerifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Shop() {
    }

    public static Shop create(
            User owner,
            String name,
            String slug,
            String businessName,
            String taxCode,
            String description
    ) {
        Shop shop = new Shop();

        shop.id = UuidV7Generator.generate();
        shop.owner = owner;
        shop.name = name;
        shop.slug = slug;
        shop.businessName = businessName;
        shop.taxCode = taxCode;
        shop.description = description;
        shop.status = ShopStatus.DRAFT;
        shop.kycStatus = KycStatus.DRAFT;

        return shop;
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
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }

    public Map<String, Object> getWarehouseSnapshot() {
        if (warehouseSnapshot == null) {
            return null;
        }

        return Map.copyOf(warehouseSnapshot);
    }

    public void updateWarehouseSnapshot(Map<String, Object> snapshot) {
        if (snapshot == null) {
            this.warehouseSnapshot = null;
            return;
        }

        this.warehouseSnapshot = new java.util.LinkedHashMap<>(snapshot);
    }
}
