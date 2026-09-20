package com.ecommerce.authuser.integration.address;

import com.ecommerce.authuser.address.domain.Address;
import com.ecommerce.authuser.address.repository.AddressRepository;
import com.ecommerce.authuser.support.base.BaseIntegrationTest;
import com.ecommerce.authuser.support.builder.UserTestBuilder;
import com.ecommerce.authuser.support.security.TestJwtFactory;
import com.ecommerce.authuser.support.security.TestUserToken;
import com.ecommerce.authuser.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AddressLocationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AddressRepository addressRepository;

    @Test
    void locationCatalogEndpointsShouldBePublicAndCacheable() throws Exception {
        mockMvc.perform(
                        get("/api/v1/locations/vn/provinces")
                )
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Cache-Control",
                        "max-age=86400, public"
                ))
                .andExpect(jsonPath("$.data.length()")
                        .value(34))
                .andExpect(jsonPath("$.data[0].code")
                        .value("01"))
                .andExpect(jsonPath("$.data[0].name")
                        .value("Thành phố Hà Nội"));

        mockMvc.perform(
                        get("/api/v1/locations/vn/provinces/79/wards")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == '26740')].name")
                        .value(hasItem("Phường Sài Gòn")));
    }

    @Test
    void createAddressShouldResolveAndPersistCanonicalLocationNames() throws Exception {
        User user = saveUser("address-create@test.com");
        TestUserToken token = TestJwtFactory.createUserToken(
                user.getId(),
                List.of("BUYER")
        );

        mockMvc.perform(
                        post("/api/v1/users/me/addresses")
                                .header(
                                        "Authorization",
                                        "Bearer " + token.value()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validAddressJson("79", "26740"))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.country_code")
                        .value("VN"))
                .andExpect(jsonPath("$.data.province_code")
                        .value("79"))
                .andExpect(jsonPath("$.data.province")
                        .value("Thành phố Hồ Chí Minh"))
                .andExpect(jsonPath("$.data.ward_code")
                        .value("26740"))
                .andExpect(jsonPath("$.data.ward")
                        .value("Phường Sài Gòn"))
                .andExpect(jsonPath("$.data.district")
                        .doesNotExist());

        Address address = addressRepository
                .findAll()
                .stream()
                .filter(item -> item.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(address.getCountryCode())
                .isEqualTo("VN");

        assertThat(address.getProvinceCode())
                .isEqualTo("79");

        assertThat(address.getProvince())
                .isEqualTo("Thành phố Hồ Chí Minh");

        assertThat(address.getWardCode())
                .isEqualTo("26740");

        assertThat(address.getWard())
                .isEqualTo("Phường Sài Gòn");

        assertThat(address.getDistrict())
                .isNull();
    }

    @Test
    void createAddressShouldRejectWardFromAnotherProvince() throws Exception {
        User user = saveUser("address-invalid@test.com");
        TestUserToken token = TestJwtFactory.createUserToken(
                user.getId(),
                List.of("BUYER")
        );

        mockMvc.perform(
                        post("/api/v1/users/me/addresses")
                                .header(
                                        "Authorization",
                                        "Bearer " + token.value()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validAddressJson("79", "00070"))
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code")
                        .value("ADDRESS_LOCATION_INVALID"));

        assertThat(addressRepository
                .countByUser_IdAndDeletedAtIsNull(user.getId()))
                .isZero();
    }

    @Test
    void updateAddressShouldReplaceCodesAndCanonicalNames() throws Exception {
        User user = saveUser("address-update@test.com");
        TestUserToken token = TestJwtFactory.createUserToken(
                user.getId(),
                List.of("BUYER")
        );

        String createResponse = mockMvc.perform(
                        post("/api/v1/users/me/addresses")
                                .header(
                                        "Authorization",
                                        "Bearer " + token.value()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validAddressJson("79", "26740"))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson = objectMapper.readTree(createResponse);
        String addressId = responseJson
                .path("data")
                .path("id")
                .asText();

        mockMvc.perform(
                        put("/api/v1/users/me/addresses/{addressId}", addressId)
                                .header(
                                        "Authorization",
                                        "Bearer " + token.value()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validAddressJson("01", "00070"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.province_code")
                        .value("01"))
                .andExpect(jsonPath("$.data.province")
                        .value("Thành phố Hà Nội"))
                .andExpect(jsonPath("$.data.ward_code")
                        .value("00070"))
                .andExpect(jsonPath("$.data.ward")
                        .value("Phường Hoàn Kiếm"));
    }

    private User saveUser(String email) {
        return userRepository.saveAndFlush(
                UserTestBuilder
                        .aUser()
                        .withEmail(email)
                        .withEmailNormalized(email)
                        .build()
        );
    }

    private String validAddressJson(
            String provinceCode,
            String wardCode
    ) {
        return """
                {
                  "recipient": "Nguyen Van A",
                  "phone": "+84901234567",
                  "line1": "123 Nguyen Hue",
                  "line2": null,
                  "country_code": "VN",
                  "province_code": "%s",
                  "ward_code": "%s",
                  "postal_code": null,
                  "is_default": true
                }
                """.formatted(provinceCode, wardCode);
    }
}
