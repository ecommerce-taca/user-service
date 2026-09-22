package com.ecommerce.authuser.location.application;

import com.ecommerce.authuser.location.exception.InvalidAdministrativeLocationException;
import com.ecommerce.authuser.location.exception.ProvinceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VietnamLocationCatalogTest {

    private VietnamLocationCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new VietnamLocationCatalog(new ObjectMapper());
    }

    @Test
    void shouldLoadAllProvincesAndWardsFromResource() {
        assertThat(catalog.findAllProvinces())
                .hasSize(34);

        int wardCount = catalog
                .findAllProvinces()
                .stream()
                .mapToInt(province ->
                        catalog
                                .findWardsByProvinceCode(province.code())
                                .size()
                )
                .sum();

        assertThat(wardCount)
                .isEqualTo(3321);
    }

    @Test
    void shouldResolveCanonicalNamesForMatchingCodes() {
        VietnamLocationCatalog.ResolvedLocation location =
                catalog.resolve("VN", "79", "26740");

        assertThat(location.countryCode())
                .isEqualTo("VN");

        assertThat(location.provinceName())
                .isEqualTo("Thành phố Hồ Chí Minh");

        assertThat(location.wardName())
                .isEqualTo("Phường Sài Gòn");
    }

    @Test
    void shouldRejectWardFromAnotherProvince() {
        assertThatThrownBy(() ->
                catalog.resolve("VN", "79", "00070")
        ).isInstanceOf(InvalidAdministrativeLocationException.class);
    }

    @Test
    void shouldRejectUnsupportedCountry() {
        assertThatThrownBy(() ->
                catalog.resolve("US", "79", "26740")
        ).isInstanceOf(InvalidAdministrativeLocationException.class);
    }

    @Test
    void shouldReportUnknownProvinceForWardLookup() {
        assertThatThrownBy(() ->
                catalog.findWardsByProvinceCode("00")
        ).isInstanceOf(ProvinceNotFoundException.class);
    }
}
