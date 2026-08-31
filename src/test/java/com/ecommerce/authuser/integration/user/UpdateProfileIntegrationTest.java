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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UpdateProfileIntegrationTest {

    private static final String SIGNUP_URL = "/api/v1/auth/signup";
    private static final String CURRENT_USER_URL = "/api/v1/users/me";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private TestUser createUser() throws Exception {

        String email =
                "update-user-" + java.util.UUID.randomUUID() + "@example.com";

        String request = """
            {
              "full_name": "Original Name",
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

        return new TestUser(
                root.path("data").path("user").path("id").asText(),
                root.path("data").path("tokens").path("access_token").asText(),
                email
        );
    }

    // ============================================================
    // A-13 — Update profile hợp lệ
    // ============================================================

    @Test
    @DisplayName("A-13 - User có thể cập nhật profile hợp lệ")
    void updateProfile_valid_shouldUpdateProfile() throws Exception {

        TestUser user = createUser();

        String request = """
            {
              "full_name": "Updated Name",
              "date_of_birth": "1995-06-30"
            }
            """;

        mockMvc.perform(
                put(CURRENT_USER_URL)
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.full_name").value("Updated Name"))
        .andExpect(jsonPath("$.data.email").value(user.email()))
        .andExpect(jsonPath("$.data.date_of_birth").value("1995-06-30"));
    }

    // ============================================================
    // A-14 — Update profile invalid
    // ============================================================

    @Test
    @DisplayName("A-14 - Full name rỗng phải bị từ chối")
    void updateProfile_invalidFullName_shouldBeRejected() throws Exception {

        TestUser user = createUser();

        String request = """
            {
              "full_name": ""
            }
            """;

        mockMvc.perform(
                put(CURRENT_USER_URL)
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("PROFILE_INVALID"));
    }

    @Test
    @DisplayName("A-14 - Full name vượt quá 120 Unicode characters phải bị từ chối")
    void updateProfile_fullNameOver120_shouldBeRejected() throws Exception {

        TestUser user = createUser();

        String fullName = "あ".repeat(121);

        String request = """
            {
              "full_name": "%s"
            }
            """.formatted(fullName);

        mockMvc.perform(
                put(CURRENT_USER_URL)
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("PROFILE_INVALID"));
    }

    @Test
    @DisplayName("A-14 - Date of birth trong tương lai phải bị từ chối")
    void updateProfile_futureDateOfBirth_shouldBeRejected() throws Exception {

        TestUser user = createUser();

        String request = """
            {
              "full_name": "Updated Name",
              "date_of_birth": "2099-01-01"
            }
            """;

        mockMvc.perform(
                put(CURRENT_USER_URL)
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("PROFILE_INVALID"));
    }

    // ============================================================
    // A-15 — Protected fields
    // ============================================================

    @Test
    @DisplayName("A-15 - Không cho user tự đổi email")
    void updateProfile_shouldNotAllowEmailChange() throws Exception {

        TestUser user = createUser();

        String request = """
            {
              "full_name": "Updated Name",
              "email": "attacker@example.com"
            }
            """;

        mockMvc.perform(
                put(CURRENT_USER_URL)
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("PROFILE_INVALID"));
    }

    @Test
    @DisplayName("A-15 - Không cho user tự đổi role")
    void updateProfile_shouldNotAllowRoleChange() throws Exception {

        TestUser user = createUser();

        String request = """
            {
              "full_name": "Updated Name",
              "role": "SELLER"
            }
            """;

        mockMvc.perform(
                put(CURRENT_USER_URL)
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("PROFILE_INVALID"));
    }

    @Test
    @DisplayName("A-15 - Không cho user tự đổi status")
    void updateProfile_shouldNotAllowStatusChange() throws Exception {

        TestUser user = createUser();

        String request = """
            {
              "full_name": "Updated Name",
              "status": "SUSPENDED"
            }
            """;

        mockMvc.perform(
                put(CURRENT_USER_URL)
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("PROFILE_INVALID"));
    }

    @Test
    @DisplayName("A-15 - Không cho user tự đổi user_id")
    void updateProfile_shouldNotAllowUserIdChange() throws Exception {

        TestUser user = createUser();

        String request = """
            {
              "full_name": "Updated Name",
              "user_id": "01912f31-7a1b-7c12-9c55-8b1c34a6d921"
            }
            """;

        mockMvc.perform(
                put(CURRENT_USER_URL)
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("PROFILE_INVALID"));
    }

    // ============================================================
    // A-16 — Update phone
    // ============================================================

    @Test
    @DisplayName("A-16 - User có thể cập nhật phone hợp lệ")
    void updateProfile_phone_shouldUpdatePhone() throws Exception {

        TestUser user = createUser();

        String newPhone = "+84909999999";

        String request = """
            {
              "full_name": "Updated Name",
              "phone": "%s"
            }
            """.formatted(newPhone);

        mockMvc.perform(
                put(CURRENT_USER_URL)
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.phone").value(newPhone))
        .andExpect(jsonPath("$.data.phone_verification_required").value(true));

        // Verify persistence through business API, not repository.
        mockMvc.perform(
                get(CURRENT_USER_URL)
                        .header("Authorization", "Bearer " + user.accessToken())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.phone").value(newPhone))
        .andExpect(jsonPath("$.data.phone_verified").value(false));
    }

    private record TestUser(
            String userId,
            String accessToken,
            String email
    ) {
    }
}


