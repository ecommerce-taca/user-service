package com.ecommerce.authuser.location.application;

import com.ecommerce.authuser.location.exception.InvalidAdministrativeLocationException;
import com.ecommerce.authuser.location.exception.ProvinceNotFoundException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class VietnamLocationCatalog {

    public static final String COUNTRY_CODE = "VN";

    private static final String DATA_PATH = "data/vietnam-address.json";

    private final List<Province> provinces;

    private final Map<String, Province> provinceByCode;

    private final Map<String, Ward> wardByCode;

    private final Map<String, List<Ward>> wardsByProvinceCode;

    public VietnamLocationCatalog(ObjectMapper objectMapper) {
        CatalogData catalogData = load(objectMapper);

        provinces = catalogData.provinces();
        provinceByCode = catalogData.provinceByCode();
        wardByCode = catalogData.wardByCode();
        wardsByProvinceCode = catalogData.wardsByProvinceCode();
    }

    public List<Province> findAllProvinces() {
        return provinces;
    }

    public List<Ward> findWardsByProvinceCode(String provinceCode) {
        String normalizedProvinceCode = normalizeCode(provinceCode);

        if (!provinceByCode.containsKey(normalizedProvinceCode)) {
            throw new ProvinceNotFoundException();
        }

        return wardsByProvinceCode.get(normalizedProvinceCode);
    }

    public ResolvedLocation resolve(
            String countryCode,
            String provinceCode,
            String wardCode
    ) {
        if (!COUNTRY_CODE.equals(normalizeCode(countryCode))) {
            throw new InvalidAdministrativeLocationException();
        }

        String normalizedProvinceCode = normalizeCode(provinceCode);
        String normalizedWardCode = normalizeCode(wardCode);

        Province province = provinceByCode.get(normalizedProvinceCode);
        Ward ward = wardByCode.get(normalizedWardCode);

        if (province == null
                || ward == null
                || !ward.provinceCode().equals(province.code())) {
            throw new InvalidAdministrativeLocationException();
        }

        return new ResolvedLocation(
                COUNTRY_CODE,
                province.code(),
                province.name(),
                ward.code(),
                ward.name()
        );
    }

    private CatalogData load(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource(DATA_PATH);

        try (InputStream inputStream = resource.getInputStream()) {
            ProvinceSource[] sources = objectMapper.readValue(
                    inputStream,
                    ProvinceSource[].class
            );

            return buildCatalog(sources);
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException(
                    "Unable to load Vietnam location catalog",
                    ex
            );
        }
    }

    private CatalogData buildCatalog(ProvinceSource[] sources) {
        if (sources == null || sources.length == 0) {
            throw invalidCatalog("Catalog must contain at least one province");
        }

        List<Province> provinceList = new ArrayList<>();
        Map<String, Province> provincesByCode = new LinkedHashMap<>();
        Map<String, Ward> wardsByCode = new LinkedHashMap<>();
        Map<String, List<Ward>> wardsByProvince = new LinkedHashMap<>();

        for (ProvinceSource source : sources) {
            String provinceCode = requiredCatalogText(
                    source.provinceCode(),
                    "province_code"
            );

            requiredCatalogText(source.code(), "province.code");

            Province province = new Province(
                    provinceCode,
                    requiredCatalogText(source.name(), "province.name"),
                    requiredCatalogText(source.shortName(), "province.short_name"),
                    requiredCatalogText(source.placeType(), "province.place_type")
            );

            if (provincesByCode.putIfAbsent(provinceCode, province) != null) {
                throw invalidCatalog("Duplicate province_code: " + provinceCode);
            }

            provinceList.add(province);

            List<WardSource> wardSources = source.wards();

            if (wardSources == null || wardSources.isEmpty()) {
                throw invalidCatalog("Province has no wards: " + provinceCode);
            }

            List<Ward> provinceWards = new ArrayList<>();

            for (WardSource wardSource : wardSources) {
                String wardCode = requiredCatalogText(
                        wardSource.wardCode(),
                        "ward_code"
                );

                String wardProvinceCode = requiredCatalogText(
                        wardSource.provinceCode(),
                        "ward.province_code"
                );

                if (!provinceCode.equals(wardProvinceCode)) {
                    throw invalidCatalog(
                            "Ward " + wardCode + " references another province"
                    );
                }

                Ward ward = new Ward(
                        wardCode,
                        requiredCatalogText(wardSource.name(), "ward.name"),
                        wardProvinceCode
                );

                if (wardsByCode.putIfAbsent(wardCode, ward) != null) {
                    throw invalidCatalog("Duplicate ward_code: " + wardCode);
                }

                provinceWards.add(ward);
            }

            wardsByProvince.put(
                    provinceCode,
                    List.copyOf(provinceWards)
            );
        }

        return new CatalogData(
                List.copyOf(provinceList),
                immutableMap(provincesByCode),
                immutableMap(wardsByCode),
                immutableMap(wardsByProvince)
        );
    }

    private String normalizeCode(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.strip();

        return normalized.isEmpty() ? null : normalized;
    }

    private String requiredCatalogText(String value, String field) {
        String normalized = normalizeCode(value);

        if (normalized == null) {
            throw invalidCatalog("Missing " + field);
        }

        return normalized;
    }

    private IllegalStateException invalidCatalog(String message) {
        return new IllegalStateException(
                "Invalid Vietnam location catalog: " + message
        );
    }

    private <K, V> Map<K, V> immutableMap(Map<K, V> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    public record Province(
            String code,
            String name,
            String shortName,
            String placeType
    ) {
    }

    public record Ward(
            String code,
            String name,
            String provinceCode
    ) {
    }

    public record ResolvedLocation(
            String countryCode,
            String provinceCode,
            String provinceName,
            String wardCode,
            String wardName
    ) {
    }

    private record ProvinceSource(
            @JsonProperty("province_code")
            String provinceCode,

            String name,

            @JsonProperty("short_name")
            String shortName,

            String code,

            @JsonProperty("place_type")
            String placeType,

            List<WardSource> wards
    ) {
    }

    private record WardSource(
            @JsonProperty("ward_code")
            String wardCode,

            String name,

            @JsonProperty("province_code")
            String provinceCode
    ) {
    }

    private record CatalogData(
            List<Province> provinces,
            Map<String, Province> provinceByCode,
            Map<String, Ward> wardByCode,
            Map<String, List<Ward>> wardsByProvinceCode
    ) {
    }
}
