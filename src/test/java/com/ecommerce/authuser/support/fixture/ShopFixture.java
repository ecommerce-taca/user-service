package com.ecommerce.authuser.support.fixture;

import com.ecommerce.authuser.shop.domain.SellerOnboarding;
import com.ecommerce.authuser.shop.domain.Shop;
import com.ecommerce.authuser.shop.domain.ShopStaff;
import com.ecommerce.authuser.shop.repository.SellerOnboardingRepository;
import com.ecommerce.authuser.shop.repository.ShopRepository;
import com.ecommerce.authuser.shop.repository.ShopStaffRepository;
import com.ecommerce.authuser.support.builder.SellerOnboardingTestBuilder;
import com.ecommerce.authuser.support.builder.ShopStaffTestBuilder;
import com.ecommerce.authuser.support.builder.ShopTestBuilder;
import com.ecommerce.authuser.user.domain.User;
import java.util.UUID;

public final class ShopFixture {
    private final ShopRepository shopRepository;

    private final SellerOnboardingRepository sellerOnboardingRepository;

    private final ShopStaffRepository shopStaffRepository;

    public ShopFixture(
            ShopRepository shopRepository,
            SellerOnboardingRepository sellerOnboardingRepository,
            ShopStaffRepository shopStaffRepository) {
        this.shopRepository = shopRepository;
        this.sellerOnboardingRepository = sellerOnboardingRepository;
        this.shopStaffRepository = shopStaffRepository;
    }

    public Shop create(
            User owner) {
        return create(
                owner,
                "Test Shop",
                "test-shop",
                "Test Shop Business");
    }

    public Shop create(
            User owner,
            String name,
            String slug,
            String businessName) {
        Shop shop = ShopTestBuilder.aShop()
                .withOwner(owner)
                .withName(name)
                .withSlug(slug)
                .withBusinessName(businessName)
                .build();

        return shopRepository.save(shop);
    }

    public Shop createDraft(
            User owner) {
        Shop shop = ShopTestBuilder.aShop()
                .withOwner(owner)
                .withName("Draft Shop")
                .withSlug("draft-shop")
                .withBusinessName("Draft Shop Business")
                .build();

        return shopRepository.save(shop);
    }

    public SellerOnboarding createOnboarding(
            Shop shop) {
        SellerOnboarding onboarding = SellerOnboardingTestBuilder
                .forShop(shop)
                .build();

        return sellerOnboardingRepository.save(onboarding);
    }

    public ShopStaff inviteStaff(
            Shop shop,
            User user,
            User invitedBy) {
        return inviteStaff(
                shop,
                user,
                invitedBy,
                "STAFF");
    }

    public ShopStaff inviteStaff(
            Shop shop,
            User user,
            User invitedBy,
            String staffRole) {
        ShopStaff staff = ShopStaffTestBuilder
                .aShopStaff()
                .withShop(shop)
                .withUser(user)
                .withStaffRole(staffRole)
                .withInvitedBy(invitedBy)
                .build();

        return shopStaffRepository.save(staff);
    }

    public ShopStaff inviteManager(
            Shop shop,
            User user,
            User invitedBy) {
        ShopStaff staff = ShopStaffTestBuilder
                .manager(
                        shop,
                        user,
                        invitedBy)
                .build();

        return shopStaffRepository.save(staff);
    }

    public ShopStaff inviteStaffMember(
            Shop shop,
            User user,
            User invitedBy) {
        ShopStaff staff = ShopStaffTestBuilder
                .staff(
                        shop,
                        user,
                        invitedBy)
                .build();

        return shopStaffRepository.save(staff);
    }

    public Shop findById(
            UUID shopId) {
        return shopRepository.findById(shopId)
                .orElseThrow(
                        () -> new AssertionError(
                                "Test shop not found: " + shopId));
    }

    public SellerOnboarding findOnboardingByShopId(
            UUID shopId) {
        return sellerOnboardingRepository
                .findByShop_Id(shopId)
                .orElseThrow(
                        () -> new AssertionError(
                                "Test seller onboarding not found for shop: "
                                        + shopId));
    }

    public ShopStaff findStaff(
            UUID shopId,
            UUID userId) {
        return shopStaffRepository
                .findByShop_IdAndUser_Id(
                        shopId,
                        userId)
                .orElseThrow(
                        () -> new AssertionError(
                                "Test shop staff not found. shopId="
                                        + shopId
                                        + ", userId="
                                        + userId));
    }
}
