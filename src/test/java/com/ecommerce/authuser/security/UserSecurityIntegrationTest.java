package com.ecommerce.authuser.security;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserSecurityIntegrationTest {

    private static final String SIGNUP_URL = "/api/v1/auth/signup";
    private static final String CURRENT_USER_URL = "/api/v1/users/me";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ============================================================
    // A-12 — Unauthenticated GET current user
    // ============================================================

    @Test
    @DisplayName("A-12 - GET /users/me không có authentication phải trả 401")
    void getCurrentUser_withoutAuthentication_shouldReturn401() throws Exception {

        mockMvc.perform(
                get(CURRENT_USER_URL)
        )
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("AUTH_TOKEN_INVALID"))
        .andExpect(jsonPath("$.error.message").isNotEmpty())
        .andExpect(jsonPath("$.error.trace_id").isNotEmpty());
    }

    // ============================================================
    // A-17 — Signup response không leak secret
    // ============================================================

    @Test
    @DisplayName("A-17 - Signup response không được leak secret")
    void signup_responseShouldNotLeakSecrets() throws Exception {

        String email =
                "secret-check-" + java.util.UUID.randomUUID() + "@example.com";

        String request = """
            {
              "full_name": "Secret Check User",
              "email": "%s",
              "password": "StrongPassword#2026",
              "phone": "+84901234567"
            }
            """.formatted(email);

        String response = mockMvc.perform(
                post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

        JsonNode root = objectMapper.readTree(response);

        JsonNode user = root.path("data").path("user");

        // User representation must never contain password/hash
        org.junit.jupiter.api.Assertions.assertFalse(
                user.has("password"),
                "Response must not contain password"
        );

        org.junit.jupiter.api.Assertions.assertFalse(
                user.has("password_hash"),
                "Response must not contain password_hash"
        );

        org.junit.jupiter.api.Assertions.assertFalse(
                user.has("refresh_token_hash"),
                "Response must not contain refresh_token_hash"
        );

        org.junit.jupiter.api.Assertions.assertFalse(
                user.has("totp_secret"),
                "Response must not contain TOTP secret"
        );

        org.junit.jupiter.api.Assertions.assertFalse(
                user.has("otp"),
                "Response must not contain raw OTP"
        );
    }

    // ============================================================
    // A-17 — Current user response không leak secret
    // ============================================================

    @Test
    @DisplayName("A-17 - GET /users/me không được leak secret")
    void currentUser_responseShouldNotLeakSecrets() throws Exception {

        String email =
                "secret-current-" + java.util.UUID.randomUUID() + "@example.com";

        String request = """
            {
              "full_name": "Secret Current User",
              "email": "%s",
              "password": "StrongPassword#2026",
              "phone": "+84901234567"
            }
            """.formatted(email);

        String signupResponse = mockMvc.perform(
                post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

        JsonNode signupRoot = objectMapper.readTree(signupResponse);

        String accessToken = signupRoot
                .path("data")
                .path("tokens")
                .path("access_token")
                .asText();

        String response = mockMvc.perform(
                get(CURRENT_USER_URL)
                        .header("Authorization", "Bearer " + accessToken)
        )
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        JsonNode user = root.path("data");

        org.junit.jupiter.api.Assertions.assertFalse(user.has("password"));
        org.junit.jupiter.api.Assertions.assertFalse(user.has("password_hash"));
        org.junit.jupiter.api.Assertions.assertFalse(user.has("refresh_token"));
        org.junit.jupiter.api.Assertions.assertFalse(user.has("refresh_token_hash"));
        org.junit.jupiter.api.Assertions.assertFalse(user.has("otp"));
        org.junit.jupiter.api.Assertions.assertFalse(user.has("totp_secret"));
        org.junit.jupiter.api.Assertions.assertFalse(user.has("bank_account_number"));
    }
}


