package com.ecommerce.authuser.integration.user;

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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RefreshIntegrationTest {
private static final String SIGNUP_URL =
        "/api/v1/auth/signup";

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

    return "refresh-"
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


private String signupAndGetRefreshToken()
        throws Exception {

    String request = """
        {
          "full_name": "Refresh Test User",
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

    return root
            .path("data")
            .path("tokens")
            .path("refresh_token")
            .asText();
}


private String refresh(
        String refreshToken
) throws Exception {

    String request = """
        {
          "refresh_token": "%s"
        }
        """.formatted(refreshToken);

    String response =
            mockMvc.perform(
                    post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request)
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode root =
            objectMapper.readTree(response);

    return root
            .path("data")
            .path("tokens")
            .path("refresh_token")
            .asText();
}


// ============================================================
// A-22 — Active refresh token
// ============================================================

@Test
@DisplayName(
        "A-22 - Active refresh token phải tạo token pair mới"
)
void refresh_activeToken_shouldReturnNewTokenPair()
        throws Exception {

    String oldRefreshToken =
            signupAndGetRefreshToken();

    String request = """
        {
          "refresh_token": "%s"
        }
        """.formatted(oldRefreshToken);

    mockMvc.perform(
            post(REFRESH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
    )
    .andExpect(status().isOk())
    .andExpect(
            jsonPath(
                    "$.data.tokens.token_type"
            ).value("Bearer")
    )
    .andExpect(
            jsonPath(
                    "$.data.tokens.access_token",
                    not(emptyOrNullString())
            )
    )
    .andExpect(
            jsonPath(
                    "$.data.tokens.refresh_token",
                    not(emptyOrNullString())
            )
    )
    .andExpect(
            jsonPath(
                    "$.data.tokens.expires_in"
            ).value(900)
    )
    .andExpect(
            jsonPath(
                    "$.data.tokens.refresh_expires_in"
            ).value(2592000)
    )
    .andExpect(
            jsonPath(
                    "$.meta.request_id",
                    not(emptyOrNullString())
            )
    );
}


// ============================================================
// A-23 — Rotation
// ============================================================

@Test
@DisplayName(
        "A-23 - Refresh rotation phải tạo refresh token mới khác token cũ"
)
void refresh_shouldRotateRefreshToken()
        throws Exception {

    String oldRefreshToken =
            signupAndGetRefreshToken();

    String newRefreshToken =
            refresh(oldRefreshToken);

    assertNotNull(newRefreshToken);
    assertFalse(newRefreshToken.isBlank());
    assertNotEquals(
            oldRefreshToken,
            newRefreshToken
    );
}


// ============================================================
// A-24 — Reuse old refresh token
// ============================================================

@Test
@DisplayName(
        "A-24 - Refresh token cũ sau rotation phải bị từ chối"
)
void refresh_reusedOldToken_shouldBeRejected()
        throws Exception {

    String oldRefreshToken =
            signupAndGetRefreshToken();

    refresh(oldRefreshToken);

    String request = """
        {
          "refresh_token": "%s"
        }
        """.formatted(oldRefreshToken);

    mockMvc.perform(
            post(REFRESH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
    )
    .andExpect(status().isUnauthorized())
    .andExpect(
            jsonPath(
                    "$.error.code"
            ).value("AUTH_REFRESH_REUSED")
    );
}


// ============================================================
// A-25 — Random token
// ============================================================

@Test
@DisplayName(
        "A-25 - Refresh token random phải bị từ chối"
)
void refresh_randomToken_shouldReturnInvalidToken()
        throws Exception {

    String randomToken =
            "random-refresh-token-"
                    + java.util.UUID.randomUUID()
                    + "-padding-padding-padding-padding";

    String request = """
        {
          "refresh_token": "%s"
        }
        """.formatted(randomToken);

    mockMvc.perform(
            post(REFRESH_URL)
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
// A-26 — Missing token
// ============================================================

@Test
@DisplayName(
        "A-26 - Refresh thiếu refresh_token phải bị từ chối"
)
void refresh_missingToken_shouldReturnInvalidInput()
        throws Exception {

    String request = """
        {
        }
        """;

    mockMvc.perform(
            post(REFRESH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
    )
    .andExpect(status().isBadRequest())
    .andExpect(
            jsonPath(
                    "$.error.code"
            ).value("AUTH_INVALID_INPUT")
    );
}


// ============================================================
// A-27 — Blank token
// ============================================================

@Test
@DisplayName(
        "A-27 - Refresh token blank phải bị từ chối"
)
void refresh_blankToken_shouldReturnInvalidInput()
        throws Exception {

    String request = """
        {
          "refresh_token": ""
        }
        """;

    mockMvc.perform(
            post(REFRESH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
    )
    .andExpect(status().isBadRequest())
    .andExpect(
            jsonPath(
                    "$.error.code"
            ).value("AUTH_INVALID_INPUT")
    );
}


// ============================================================
// A-28 — New refresh token remains usable
// ============================================================

@Test
@DisplayName(
        "A-28 - Refresh token mới sau rotation phải tiếp tục sử dụng được"
)
void refresh_newToken_shouldRemainUsable()
        throws Exception {

    String oldRefreshToken =
            signupAndGetRefreshToken();

    String newRefreshToken =
            refresh(oldRefreshToken);

    assertNotEquals(
            oldRefreshToken,
            newRefreshToken
    );

    String secondRequest = """
        {
          "refresh_token": "%s"
        }
        """.formatted(newRefreshToken);

    mockMvc.perform(
            post(REFRESH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(secondRequest)
    )
    .andExpect(status().isOk())
    .andExpect(
            jsonPath(
                    "$.data.tokens.access_token",
                    not(emptyOrNullString())
            )
    )
    .andExpect(
            jsonPath(
                    "$.data.tokens.refresh_token",
                    not(emptyOrNullString())
            )
    );
}
}


