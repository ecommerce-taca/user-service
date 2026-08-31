package com.ecommerce.authuser.integration.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SignupIntegrationTest {

    private static final String SIGNUP_URL = "/api/v1/auth/signup";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String validSignupJson() {
        return """
            {
              "full_name": "Nguyễn Minh Anh",
              "email": "minhanh-%s@example.com",
              "password": "StrongPassword#2026",
              "phone": "+84901234567"
            }
            """.formatted(java.util.UUID.randomUUID());
    }

    // ============================================================
    // A-01 — Signup hợp lệ
    // ============================================================

    @Test
    @DisplayName("A-01 - Signup hợp lệ tạo buyer account và trả token")
    void signup_valid_shouldCreateBuyerAccount() throws Exception {

        String request = validSignupJson();

        mockMvc.perform(
                post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isCreated())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.data.user.id", not(emptyOrNullString())))
        .andExpect(jsonPath("$.data.user.full_name").value("Nguyễn Minh Anh"))
        .andExpect(jsonPath("$.data.user.email", containsString("@example.com")))
        .andExpect(jsonPath("$.data.user.roles", hasItem("BUYER")))
        .andExpect(jsonPath("$.data.user.status").value("ACTIVE"))
        .andExpect(jsonPath("$.data.tokens.token_type").value("Bearer"))
        .andExpect(jsonPath("$.data.tokens.access_token", not(emptyOrNullString())))
        .andExpect(jsonPath("$.data.tokens.refresh_token", not(emptyOrNullString())))
        .andExpect(jsonPath("$.data.tokens.expires_in").value(900))
        .andExpect(jsonPath("$.data.tokens.refresh_expires_in").value(2592000))
        .andExpect(jsonPath("$.data.verification.email_sent").value(true))
        .andExpect(jsonPath("$.meta.request_id", not(emptyOrNullString())));
    }

    // ============================================================
    // A-02 — Signup thiếu body
    // ============================================================

    @Test
    @DisplayName("A-02 - Signup không có request body phải bị từ chối")
    void signup_withoutBody_shouldReturnInvalidInput() throws Exception {

        mockMvc.perform(
                post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_INPUT"))
        .andExpect(jsonPath("$.error.message").isNotEmpty())
        .andExpect(jsonPath("$.error.trace_id").isNotEmpty());
    }

    // ============================================================
    // A-03 — Signup thiếu field bắt buộc
    // ============================================================

    @Test
    @DisplayName("A-03 - Signup thiếu field bắt buộc phải bị từ chối")
    void signup_missingRequiredField_shouldReturnInvalidInput() throws Exception {

        String request = """
            {
              "email": "missing-field-%s@example.com",
              "password": "StrongPassword#2026",
              "phone": "+84901234567"
            }
            """.formatted(java.util.UUID.randomUUID());

        mockMvc.perform(
                post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_INPUT"));
    }

    // ============================================================
    // A-04 — Full name boundary
    // ============================================================

    @Test
    @DisplayName("A-04a - Full name dài đúng 1 Unicode character phải hợp lệ")
    void signup_fullNameLength1_shouldBeAccepted() throws Exception {

        String request = """
            {
              "full_name": "A",
              "email": "fullname-1-%s@example.com",
              "password": "StrongPassword#2026",
              "phone": "+84901234567"
            }
            """.formatted(java.util.UUID.randomUUID());

        mockMvc.perform(
                post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("A-04b - Full name dài đúng 120 Unicode characters phải hợp lệ")
    void signup_fullNameLength120_shouldBeAccepted() throws Exception {

        String fullName = "あ".repeat(120);

        String request = """
            {
              "full_name": "%s",
              "email": "fullname-120-%s@example.com",
              "password": "StrongPassword#2026",
              "phone": "+84901234567"
            }
            """.formatted(fullName, java.util.UUID.randomUUID());

        mockMvc.perform(
                post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("A-04c - Full name dài 121 Unicode characters phải bị từ chối")
    void signup_fullNameLength121_shouldBeRejected() throws Exception {

        String fullName = "あ".repeat(121);

        String request = """
            {
              "full_name": "%s",
              "email": "fullname-121-%s@example.com",
              "password": "StrongPassword#2026",
              "phone": "+84901234567"
            }
            """.formatted(fullName, java.util.UUID.randomUUID());

        mockMvc.perform(
                post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_INPUT"));
    }

    // ============================================================
    // A-05 — Email invalid
    // ============================================================

    @ParameterizedTest(name = "invalid email = {0}")
    @ValueSource(strings = {
            "abc",
            "abc@",
            "@example.com",
            "abc@example",
            "abc example@example.com"
    })
    @DisplayName("A-05 - Email sai format phải bị từ chối")
    void signup_invalidEmail_shouldReturnInvalidInput(String email) throws Exception {

        String request = """
            {
              "full_name": "Nguyễn Minh Anh",
              "email": "%s",
              "password": "StrongPassword#2026",
              "phone": "+84901234567"
            }
            """.formatted(email);

        mockMvc.perform(
                post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_INPUT"));
    }

    // ============================================================
    // A-06 — Password boundary
    // ============================================================

    @Test
    @DisplayName("A-06a - Password 11 characters phải bị từ chối")
    void signup_passwordLength11_shouldBeRejected() throws Exception {

        String request = """
            {
              "full_name": "Nguyễn Minh Anh",
              "email": "password-11-%s@example.com",
              "password": "12345678901",
              "phone": "+84901234567"
            }
            """.formatted(java.util.UUID.randomUUID());

        mockMvc.perform(
                post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_INPUT"));
    }

    @Test
    @DisplayName("A-06b - Password đúng 12 characters phải hợp lệ")
    void signup_passwordLength12_shouldBeAccepted() throws Exception {

        String request = """
            {
              "full_name": "Nguyễn Minh Anh",
              "email": "password-12-%s@example.com",
              "password": "123456789012",
              "phone": "+84901234567"
            }
            """.formatted(java.util.UUID.randomUUID());

        mockMvc.perform(
                post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("A-06c - Password đúng 72 characters phải hợp lệ")
    void signup_passwordLength72_shouldBeAccepted() throws Exception {

        String password = "a".repeat(72);

        String request = """
            {
              "full_name": "Nguyễn Minh Anh",
              "email": "password-72-%s@example.com",
              "password": "%s",
              "phone": "+84901234567"
            }
            """.formatted(
                java.util.UUID.randomUUID(),
                password
        );

        mockMvc.perform(
                post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("A-06d - Password 73 characters phải bị từ chối")
    void signup_passwordLength73_shouldBeRejected() throws Exception {

        String password = "a".repeat(73);

        String request = """
            {
              "full_name": "Nguyễn Minh Anh",
              "email": "password-73-%s@example.com",
              "password": "%s",
              "phone": "+84901234567"
            }
            """.formatted(
                java.util.UUID.randomUUID(),
                password
        );

        mockMvc.perform(
                post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_INPUT"));
    }

    // ============================================================
    // A-07 — Duplicate email
    // ============================================================

    @Test
    @DisplayName("A-07 - Không cho đăng ký email đã tồn tại")
    void signup_duplicateEmail_shouldReturnConflict() throws Exception {

        String email = "duplicate-email-" + java.util.UUID.randomUUID() + "@example.com";

        String firstRequest = """
            {
              "full_name": "First User",
              "email": "%s",
              "password": "StrongPassword#2026",
              "phone": "+84901111111"
            }
            """.formatted(email);

        mockMvc.perform(
                post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequest)
        )
        .andExpect(status().isCreated());

        String secondRequest = """
            {
              "full_name": "Second User",
              "email": "%s",
              "password": "StrongPassword#2026",
              "phone": "+84902222222"
            }
            """.formatted(email);

        mockMvc.perform(
                post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondRequest)
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("AUTH_EMAIL_EXISTS"));
    }

    // ============================================================
    // A-08 — Duplicate phone
    // ============================================================

    @Test
    @DisplayName("A-08 - Không cho đăng ký phone đã tồn tại")
    void signup_duplicatePhone_shouldReturnConflict() throws Exception {

        String phone = "+8490" + String.format("%07d",
                Math.abs(java.util.UUID.randomUUID().hashCode()) % 10_000_000);

        String firstRequest = """
            {
              "full_name": "First User",
              "email": "phone-first-%s@example.com",
              "password": "StrongPassword#2026",
              "phone": "%s"
            }
            """.formatted(
                java.util.UUID.randomUUID(),
                phone
        );

        mockMvc.perform(
                post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequest)
        )
        .andExpect(status().isCreated());

        String secondRequest = """
            {
              "full_name": "Second User",
              "email": "phone-second-%s@example.com",
              "password": "StrongPassword#2026",
              "phone": "%s"
            }
            """.formatted(
                java.util.UUID.randomUUID(),
                phone
        );

        mockMvc.perform(
                post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondRequest)
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("AUTH_PHONE_EXISTS"));
    }

    // ============================================================
    // A-09 — Phone null
    // ============================================================

    @Test
    @DisplayName("A-09 - Signup không có phone vẫn hợp lệ")
    void signup_nullPhone_shouldBeAccepted() throws Exception {

        String request = """
            {
              "full_name": "Nguyễn Minh Anh",
              "email": "null-phone-%s@example.com",
              "password": "StrongPassword#2026",
              "phone": null
            }
            """.formatted(java.util.UUID.randomUUID());

        mockMvc.perform(
                post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.user.phone").doesNotExist())
        .andExpect(jsonPath("$.data.user.phone_verified").value(false));
    }

    // ============================================================
    // A-10 — User mặc định sau signup
    // ============================================================

    @Test
    @DisplayName("A-10 - User sau signup có trạng thái và verification mặc định đúng")
    void signup_shouldCreateUserWithCorrectDefaultState() throws Exception {

        String request = validSignupJson();

        String response = mockMvc.perform(
                post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.user.roles", hasItem("BUYER")))
        .andExpect(jsonPath("$.data.user.status").value("ACTIVE"))
        .andExpect(jsonPath("$.data.user.email_verified").value(false))
        .andExpect(jsonPath("$.data.user.phone_verified").value(false))
        .andReturn()
        .getResponse()
        .getContentAsString();

        JsonNode root = objectMapper.readTree(response);

        org.junit.jupiter.api.Assertions.assertNotNull(
                root.path("data").path("user").path("id").textValue()
        );
    }
}


