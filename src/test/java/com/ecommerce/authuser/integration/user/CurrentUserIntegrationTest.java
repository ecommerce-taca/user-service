package com.ecommerce.authuser.integration.user;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CurrentUserIntegrationTest {

    private static final String SIGNUP_URL = "/api/v1/auth/signup";
    private static final String CURRENT_USER_URL = "/api/v1/users/me";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("A-11 - User đã signup có thể đọc profile của chính mình")
    void getCurrentUser_shouldReturnAuthenticatedUser() throws Exception {

        String email =
                "current-user-" + java.util.UUID.randomUUID() + "@example.com";

        String signupRequest = """
            {
              "full_name": "Current User",
              "email": "%s",
              "password": "StrongPassword#2026",
              "phone": "+84901234567"
            }
            """.formatted(email);

        String signupResponse = mockMvc.perform(
                post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupRequest)
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

        String userId = signupRoot
                .path("data")
                .path("user")
                .path("id")
                .asText();

        assertNotNull(accessToken);
        assertNotNull(userId);

        mockMvc.perform(
                get(CURRENT_USER_URL)
                        .header("Authorization", "Bearer " + accessToken)
        )
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.data.id").value(userId))
        .andExpect(jsonPath("$.data.full_name").value("Current User"))
        .andExpect(jsonPath("$.data.email").value(email))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
        .andExpect(jsonPath("$.data.roles", hasItem("BUYER")))
        .andExpect(jsonPath("$.meta.request_id", not(emptyOrNullString())));
    }
}


