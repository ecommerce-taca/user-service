package com.ecommerce.authuser.kyc.infrastructure.scheduler;

import com.ecommerce.authuser.kyc.application.expiry.KycExpiryService;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KycExpiryScheduler {

    private final KycExpiryService kycExpiryService;

    @Scheduled(
            fixedDelayString =
                    "${auth.kyc-expiry.fixed-delay-ms:60000}"
    )
    public void expirePendingKycCases() {
        kycExpiryService.expirePendingCases();
    }
}