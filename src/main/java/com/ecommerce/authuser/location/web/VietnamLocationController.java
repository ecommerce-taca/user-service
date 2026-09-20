package com.ecommerce.authuser.location.web;

import com.ecommerce.authuser.common.web.RequestIdResolver;
import com.ecommerce.authuser.location.application.VietnamLocationCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/locations/vn")
@RequiredArgsConstructor
public class VietnamLocationController {

    private static final CacheControl CACHE_CONTROL = CacheControl
            .maxAge(Duration.ofDays(1))
            .cachePublic();

    private final VietnamLocationCatalog locationCatalog;

    @GetMapping("/provinces")
    public ResponseEntity<ProvinceListResponse> listProvinces(
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {
        List<ProvinceListResponse.Data> data = locationCatalog
                .findAllProvinces()
                .stream()
                .map(province -> new ProvinceListResponse.Data(
                        province.code(),
                        province.name(),
                        province.shortName(),
                        province.placeType()
                ))
                .toList();

        return ResponseEntity
                .ok()
                .cacheControl(CACHE_CONTROL)
                .body(new ProvinceListResponse(
                        data,
                        new ProvinceListResponse.Meta(
                                RequestIdResolver.resolve(requestId)
                        )
                ));
    }

    @GetMapping("/provinces/{provinceCode}/wards")
    public ResponseEntity<WardListResponse> listWards(
            @PathVariable String provinceCode,
            @RequestHeader(name = "X-Request-ID", required = false) String requestId
    ) {
        List<WardListResponse.Data> data = locationCatalog
                .findWardsByProvinceCode(provinceCode)
                .stream()
                .map(ward -> new WardListResponse.Data(
                        ward.code(),
                        ward.name(),
                        ward.provinceCode()
                ))
                .toList();

        return ResponseEntity
                .ok()
                .cacheControl(CACHE_CONTROL)
                .body(new WardListResponse(
                        data,
                        new WardListResponse.Meta(
                                RequestIdResolver.resolve(requestId)
                        )
                ));
    }
}
