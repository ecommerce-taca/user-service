package com.ecommerce.authuser.shop.application.onboarding.get;

import com.ecommerce.authuser.rbac.domain.RbacKeys;
import com.ecommerce.authuser.rbac.repository.UserRoleRepository;

import com.ecommerce.authuser.shop.domain.OnboardingStep;
import com.ecommerce.authuser.shop.domain.SellerOnboarding;
import com.ecommerce.authuser.shop.domain.Shop;

import com.ecommerce.authuser.shop.exception.SellerPermissionDeniedException;
import com.ecommerce.authuser.shop.exception.ShopNotFoundException;

import com.ecommerce.authuser.shop.repository.SellerOnboardingRepository;
import com.ecommerce.authuser.shop.repository.ShopRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetSellerOnboardingService {

    private final ShopRepository shopRepository;

    private final SellerOnboardingRepository sellerOnboardingRepository;

    private final UserRoleRepository userRoleRepository;

    @Transactional(readOnly = true)
    public GetSellerOnboardingResult get(
            GetSellerOnboardingQuery query
    ) {

        if (query == null || query.userId() == null) {
            throw new ShopNotFoundException();
        }

        Shop shop = shopRepository
                .findByOwner_IdAndDeletedAtIsNull(query.userId())
                .orElseThrow(ShopNotFoundException::new);

        boolean sellerOwner = userRoleRepository
                .existsByUser_IdAndRole_RoleKeyAndShop_IdAndRevokedAtIsNull(
                        query.userId(),
                        RbacKeys.Roles.SELLER,
                        shop.getId()
                );

        if (!sellerOwner) {
            throw new SellerPermissionDeniedException();
        }

        SellerOnboarding onboarding = sellerOnboardingRepository
                .findByShop_Id(shop.getId())
                .orElseThrow(ShopNotFoundException::new);

        List<GetSellerOnboardingResult.StepResult> steps =
                List.of(
                        new GetSellerOnboardingResult.StepResult(
                                OnboardingStep.PROFILE,
                                onboarding.isProfileCompleted()
                        ),

                        new GetSellerOnboardingResult.StepResult(
                                OnboardingStep.KYC,
                                onboarding.isKycCompleted()
                        ),

                        new GetSellerOnboardingResult.StepResult(
                                OnboardingStep.WAREHOUSE,
                                onboarding.isWarehouseCompleted()
                        ),

                        new GetSellerOnboardingResult.StepResult(
                                OnboardingStep.BANK,
                                onboarding.isBankCompleted()
                        ),

                        new GetSellerOnboardingResult.StepResult(
                                OnboardingStep.FIRST_PRODUCT,
                                onboarding.isFirstProductCompleted()
                        )
                );

        return new GetSellerOnboardingResult(
                shop.getId(),
                onboarding.getCurrentStep(),
                steps,
                shop.getStatus(),
                shop.getKycStatus(),
                onboarding.getBlockers()
        );
    }
}
