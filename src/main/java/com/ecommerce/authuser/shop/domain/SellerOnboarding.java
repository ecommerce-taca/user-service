package com.ecommerce.authuser.shop.domain;


import com.ecommerce.authuser.common.id.UuidV7Generator;
import com.ecommerce.authuser.common.persistence.BooleanToTinyIntConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "seller_onboarding",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_seller_onboarding_shop",
                        columnNames = "shop_id"
                )
        }
)
@Getter
public class SellerOnboarding {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false, unique = true)
    private Shop shop;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false, length = 24)
    private OnboardingStep currentStep;

    @Convert(converter = BooleanToTinyIntConverter.class)
    @Column(name = "profile_completed", nullable = false)
    private boolean profileCompleted;

    @Convert(converter = BooleanToTinyIntConverter.class)
    @Column(name = "kyc_completed", nullable = false)
    private boolean kycCompleted;

    @Convert(converter = BooleanToTinyIntConverter.class)
    @Column(name = "warehouse_completed", nullable = false)
    private boolean warehouseCompleted;

    @Convert(converter = BooleanToTinyIntConverter.class)
    @Column(name = "bank_completed", nullable = false)
    private boolean bankCompleted;

    @Convert(converter = BooleanToTinyIntConverter.class)
    @Column(name = "first_product_completed", nullable = false)
    private boolean firstProductCompleted;

    @Getter(AccessLevel.NONE)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "blockers_json", nullable = false, columnDefinition = "JSON")
    private List<String> blockers = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SellerOnboarding() {
    }

    public static SellerOnboarding create(
            Shop shop
    ) {
        SellerOnboarding onboarding = new SellerOnboarding();

        onboarding.id = UuidV7Generator.generate();
        onboarding.shop = shop;
        onboarding.currentStep = OnboardingStep.PROFILE;
        onboarding.profileCompleted = false;
        onboarding.kycCompleted = false;
        onboarding.warehouseCompleted = false;
        onboarding.bankCompleted = false;
        onboarding.firstProductCompleted = false;
        onboarding.blockers = new ArrayList<>();

        return onboarding;
    }

    public void completeProfileStep() {
        profileCompleted = true;

        if (currentStep == OnboardingStep.PROFILE) {

            currentStep = OnboardingStep.KYC;

            replaceBlockers(List.of("KYC_DOCUMENT_REQUIRED"));
        }
    }

    public void completeWarehouseStep() {
        warehouseCompleted = true;

        if (currentStep == OnboardingStep.WAREHOUSE) {
            currentStep = OnboardingStep.BANK;
        }
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

    public List<String> getBlockers() {
        return List.copyOf(blockers);
    }

    public void replaceBlockers(
            List<String> blockers
    ) {
        this.blockers = blockers == null ? new ArrayList<>() : new ArrayList<>(blockers);
    }
}
