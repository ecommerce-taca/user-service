package com.ecommerce.authuser.shop.infrastructure.storage;

import com.ecommerce.authuser.kyc.port.KycObjectStoragePort;
import com.ecommerce.authuser.shop.port.ShopAssetUrlPort;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ShopAssetUrlAdapter implements ShopAssetUrlPort {

    private static final Duration LOGO_URL_TTL = Duration.ofMinutes(10);

    private final KycObjectStoragePort objectStoragePort;

    @Override
    public String resolveLogoUrl(String logoObjectKey) {

        if (logoObjectKey == null || logoObjectKey.isBlank()) {
            return null;
        }

        return objectStoragePort
                .presignDownload(
                        logoObjectKey.strip(),
                        LOGO_URL_TTL
                )
                .downloadUrl();
    }
}
