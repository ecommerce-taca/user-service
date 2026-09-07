package com.ecommerce.authuser.integration.auth;

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

import com.ecommerce.authuser.support.base.BaseIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SignoutIntegrationTest extends BaseIntegrationTest {
private static final String SIGNUP_URL =
        "/api/v1/auth/signup";

private static final String SIGNOUT_URL =
        "/api/v1/auth/signout";

private static final String REFRESH_URL =
        "/api/v1/auth/refresh";

private static final String PASSWORD =
        "StrongPassword#2026";

@Autowired
private MockMvc mockMvc;

@Autowired
private ObjectMapper objectMapper;


// ============================================================
// Helpers
// ============================================================

private String uniqueEmail() {

    return "signout-"
            + java.util.UUID.randomUUID()
            + "@example.com";
}


private String uniquePhone() {

    long number =
            Math.abs(
                    java.util.UUID.randomUUID()
                            .getMostSignificantBits()
            ) % 100000000;

    return "+849"
            + String.format(
                    "%08d",
                    number
            );
}


private SignupTokens signup()
        throws Exception {

    String request = """
        {
          "full_name": "Signout Test User",
          "email": "%s",
          "password": "%s",
          "phone": "%s"
        }
        """.formatted(
            uniqueEmail(),
            PASSWORD,
            uniquePhone()
    );

    String response =
            mockMvc.perform(
                    post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request)
            )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode root =
            objectMapper.readTree(response);

    String accessToken =
            root.path("data")
                    .path("tokens")
                    .path("access_token")
                    .asText();

    String refreshToken =
            root.path("data")
                    .path("tokens")
                    .path("refresh_token")
                    .asText();

    return new SignupTokens(
            accessToken,
            refreshToken
    );
}


// ============================================================
// A-29 — Signout current session
// ============================================================

@Test
@DisplayName(
        "A-29 - Signout current session phải trả 204"
)
void signout_currentSession_shouldReturnNoContent()
        throws Exception {

    SignupTokens tokens =
            signup();

    mockMvc.perform(
            post(SIGNOUT_URL)
                    .header(
                            "Authorization",
                            "Bearer " + tokens.accessToken()
                    )
    )
    .andExpect(status().isNoContent())
    .andExpect(content().string(""));
}


// ============================================================
// A-30 — Signout phải revoke refresh token
// ============================================================

@Test
@DisplayName(
        "A-30 - Signout phải revoke refresh token của session"
)
void signout_shouldRevokeRefreshToken()
        throws Exception {

    SignupTokens tokens =
            signup();

    mockMvc.perform(
            post(SIGNOUT_URL)
                    .header(
                            "Authorization",
                            "Bearer " + tokens.accessToken()
                    )
    )
    .andExpect(status().isNoContent());


    String refreshRequest = """
        {
          "refresh_token": "%s"
        }
        """.formatted(
            tokens.refreshToken()
    );

    mockMvc.perform(
            post(REFRESH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(refreshRequest)
    )
    .andExpect(status().isUnauthorized());
}


// ============================================================
// A-31 — Signout with refresh token
// ============================================================

@Test
@DisplayName(
        "A-31 - Signout kèm refresh token hợp lệ phải thành công"
)
void signout_withRefreshToken_shouldSucceed()
        throws Exception {

    SignupTokens tokens =
            signup();

    String request = """
        {
          "refresh_token": "%s"
        }
        """.formatted(
            tokens.refreshToken()
    );

    mockMvc.perform(
            post(SIGNOUT_URL)
                    .header(
                            "Authorization",
                            "Bearer " + tokens.accessToken()
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
    )
    .andExpect(status().isNoContent());
}


// ============================================================
// A-32 — Invalid refresh token
// ============================================================

@Test
@DisplayName(
        "A-32 - Signout với refresh token không hợp lệ phải bị từ chối"
)
void signout_invalidRefreshToken_shouldReturnUnauthorized()
        throws Exception {

    SignupTokens tokens =
            signup();

    String request = """
        {
          "refresh_token": "invalid-refresh-token-%s"
        }
        """.formatted(
            java.util.UUID.randomUUID()
    );

    mockMvc.perform(
            post(SIGNOUT_URL)
                    .header(
                            "Authorization",
                            "Bearer " + tokens.accessToken()
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
    )
    .andExpect(status().isUnauthorized())
    .andExpect(
            jsonPath(
                    "$.error.code"
            ).value("AUTH_TOKEN_INVALID")
    );
}


// ============================================================
// A-33 — all_sessions requires MFA step-up
// ============================================================

@Test
@DisplayName(
        "A-33 - all_sessions=true phải yêu cầu MFA step-up"
)
void signout_allSessionsWithoutStepUp_shouldRequireMfa()
        throws Exception {

    SignupTokens tokens =
            signup();

    String request = """
        {
          "all_sessions": true
        }
        """;

    mockMvc.perform(
            post(SIGNOUT_URL)
                    .header(
                            "Authorization",
                            "Bearer " + tokens.accessToken()
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
    )
    .andExpect(status().isPreconditionRequired())
    .andExpect(
            jsonPath(
                    "$.error.code"
            ).value("RBAC_MFA_REQUIRED")
    );
}


// ============================================================
// A-34 — all_sessions failure must not revoke session
// ============================================================

@Test
@DisplayName(
        "A-34 - all_sessions thiếu MFA không được revoke session"
)
void signout_allSessionsWithoutStepUp_shouldNotRevokeSession()
        throws Exception {

    SignupTokens tokens =
            signup();

    String request = """
        {
          "all_sessions": true
        }
        """;

    mockMvc.perform(
            post(SIGNOUT_URL)
                    .header(
                            "Authorization",
                            "Bearer " + tokens.accessToken()
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
    )
    .andExpect(status().isPreconditionRequired());


    String refreshRequest = """
        {
          "refresh_token": "%s"
        }
        """.formatted(
            tokens.refreshToken()
    );

    mockMvc.perform(
            post(REFRESH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(refreshRequest)
    )
    .andExpect(status().isOk());
}


// ============================================================
// A-35 — Unauthenticated
// ============================================================

@Test
@DisplayName(
        "A-35 - Signout không có Authorization phải bị từ chối"
)
void signout_withoutAuthentication_shouldReturnUnauthorized()
        throws Exception {

    mockMvc.perform(
            post(SIGNOUT_URL)
    )
    .andExpect(status().isUnauthorized());
}


// ============================================================
// Test data holder
// ============================================================

private record SignupTokens(
        String accessToken,
        String refreshToken
) {
}
}


