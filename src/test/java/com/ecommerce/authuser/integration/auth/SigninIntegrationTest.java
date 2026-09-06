package com.ecommerce.authuser.integration.auth;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerce.authuser.support.base.BaseIntegrationTest;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SigninIntegrationTest extends BaseIntegrationTest {
private static final String SIGNUP_URL =
        "/api/v1/auth/signup";

private static final String SIGNIN_URL =
        "/api/v1/auth/signin";

private static final String PASSWORD =
        "StrongPassword#2026";

@Autowired
private MockMvc mockMvc;

@Autowired
private ObjectMapper objectMapper;


// ============================================================
// Helper
// ============================================================

private String signupUser(
        String email,
        String phone
) throws Exception {

    String request = """
        {
          "full_name": "Signin Test User",
          "email": "%s",
          "password": "%s",
          "phone": "%s"
        }
        """.formatted(
            email,
            PASSWORD,
            phone
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


private String uniqueEmail(String prefix) {

    return prefix
            + "-"
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


// ============================================================
// A-11 — Signin bằng email/password đúng
// ============================================================

@Test
@DisplayName(
        "A-11 - Signin bằng email và password đúng phải trả token pair"
)
void signin_validEmailAndPassword_shouldReturnTokenPair()
        throws Exception {

    String email =
            uniqueEmail("signin-email");

    signupUser(
            email,
            uniquePhone()
    );

    String request = """
        {
          "identifier": "%s",
          "password": "%s"
        }
        """.formatted(
            email,
            PASSWORD
    );

    mockMvc.perform(
            post(SIGNIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
    )
    .andExpect(status().isOk())
    .andExpect(
            content()
                    .contentTypeCompatibleWith(
                            MediaType.APPLICATION_JSON
                    )
    )
    .andExpect(
            jsonPath(
                    "$.data.user.email"
            ).value(email)
    )
    .andExpect(
            jsonPath(
                    "$.data.user.status"
            ).value("ACTIVE")
    )
    .andExpect(
            jsonPath(
                    "$.data.user.roles",
                    hasItem("BUYER")
            )
    )
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
// A-12 — Signin bằng phone
// ============================================================

@Test
@DisplayName(
        "A-12 - Signin bằng phone và password đúng phải thành công"
)
void signin_validPhoneAndPassword_shouldReturnTokenPair()
        throws Exception {

    String email =
            uniqueEmail("signin-phone");

    String phone =
            uniquePhone();

    signupUser(
            email,
            phone
    );

    String request = """
        {
          "identifier": "%s",
          "password": "%s"
        }
        """.formatted(
            phone,
            PASSWORD
    );

    mockMvc.perform(
            post(SIGNIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
    )
    .andExpect(status().isOk())
    .andExpect(
            jsonPath(
                    "$.data.user.email"
            ).value(email)
    )
    .andExpect(
            jsonPath(
                    "$.data.user.phone"
            ).value(phone)
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
    );
}


// ============================================================
// A-13 — User không tồn tại
// ============================================================

@Test
@DisplayName(
        "A-13 - Signin với identifier không tồn tại phải trả invalid credentials"
)
void signin_unknownIdentifier_shouldReturnInvalidCredentials()
        throws Exception {

    String request = """
        {
          "identifier": "not-found-%s@example.com",
          "password": "%s"
        }
        """.formatted(
            java.util.UUID.randomUUID(),
            PASSWORD
    );

    mockMvc.perform(
            post(SIGNIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
    )
    .andExpect(status().isUnauthorized())
    .andExpect(
            jsonPath(
                    "$.error.code"
            ).value("AUTH_INVALID_CREDENTIALS")
    )
    .andExpect(
            jsonPath(
                    "$.error.message"
            ).isNotEmpty()
    )
    .andExpect(
            jsonPath(
                    "$.error.trace_id"
            ).isNotEmpty()
    );
}


// ============================================================
// A-14 — Password sai
// ============================================================

@Test
@DisplayName(
        "A-14 - Signin với password sai phải trả invalid credentials"
)
void signin_wrongPassword_shouldReturnInvalidCredentials()
        throws Exception {

    String email =
            uniqueEmail("signin-wrong-password");

    signupUser(
            email,
            uniquePhone()
    );

    String request = """
        {
          "identifier": "%s",
          "password": "WrongPassword#2026"
        }
        """.formatted(email);

    mockMvc.perform(
            post(SIGNIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
    )
    .andExpect(status().isUnauthorized())
    .andExpect(
            jsonPath(
                    "$.error.code"
            ).value("AUTH_INVALID_CREDENTIALS")
    );
}


// ============================================================
// A-15 — Không enumerate account
// ============================================================

@Test
@DisplayName(
        "A-15 - Signin user không tồn tại và password sai không được tiết lộ account"
)
void signin_unknownUser_shouldNotRevealAccountExistence()
        throws Exception {

    String unknownRequest = """
        {
          "identifier": "unknown-%s@example.com",
          "password": "WrongPassword#2026"
        }
        """.formatted(
            java.util.UUID.randomUUID()
    );

    mockMvc.perform(
            post(SIGNIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(unknownRequest)
    )
    .andExpect(status().isUnauthorized())
    .andExpect(
            jsonPath(
                    "$.error.code"
            ).value("AUTH_INVALID_CREDENTIALS")
    );
}


// ============================================================
// A-16 — remember_me mặc định true
// ============================================================

@Test
@DisplayName(
        "A-16 - remember_me bị bỏ trống vẫn signin thành công"
)
void signin_withoutRememberMe_shouldUseDefault()
        throws Exception {

    String email =
            uniqueEmail("signin-remember-default");

    signupUser(
            email,
            uniquePhone()
    );

    String request = """
        {
          "identifier": "%s",
          "password": "%s"
        }
        """.formatted(
            email,
            PASSWORD
    );

    mockMvc.perform(
            post(SIGNIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
    )
    .andExpect(status().isOk())
    .andExpect(
            jsonPath(
                    "$.data.tokens.access_token",
                    not(emptyOrNullString())
            )
    );
}


// ============================================================
// A-17 — remember_me=false
// ============================================================

@Test
@DisplayName(
        "A-17 - remember_me=false vẫn cho signin thành công"
)
void signin_rememberMeFalse_shouldStillSucceed()
        throws Exception {

    String email =
            uniqueEmail("signin-remember-false");

    signupUser(
            email,
            uniquePhone()
    );

    String request = """
        {
          "identifier": "%s",
          "password": "%s",
          "remember_me": false
        }
        """.formatted(
            email,
            PASSWORD
    );

    mockMvc.perform(
            post(SIGNIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
    )
    .andExpect(status().isOk())
    .andExpect(
            jsonPath(
                    "$.data.tokens.access_token",
                    not(emptyOrNullString())
            )
    );
}


// ============================================================
// A-18 — Password sai nhiều lần → account locked
// ============================================================

@Test
@DisplayName(
        "A-18 - Sai password liên tiếp đủ threshold phải khóa account"
)
void signin_repeatedWrongPassword_shouldLockAccount()
        throws Exception {

    String email =
            uniqueEmail("signin-lock");

    signupUser(
            email,
            uniquePhone()
    );

    String request = """
        {
          "identifier": "%s",
          "password": "WrongPassword#2026"
        }
        """.formatted(email);

    for (int i = 1; i <= 5; i++) {

        mockMvc.perform(
                post(SIGNIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(
                status().is(
                        i == 5
                                ? 423
                                : 401
                )
        );
    }
}


// ============================================================
// A-19 — Account đã locked không được signin
// ============================================================

@Test
@DisplayName(
        "A-19 - Account đã bị khóa phải trả AUTH_ACCOUNT_LOCKED"
)
void signin_lockedAccount_shouldReturnAccountLocked()
        throws Exception {

    String email =
            uniqueEmail("signin-locked");

    signupUser(
            email,
            uniquePhone()
    );

    String wrongRequest = """
        {
          "identifier": "%s",
          "password": "WrongPassword#2026"
        }
        """.formatted(email);

    for (int i = 1; i <= 5; i++) {

        mockMvc.perform(
                post(SIGNIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongRequest)
        );
    }

    String correctRequest = """
        {
          "identifier": "%s",
          "password": "%s"
        }
        """.formatted(
            email,
            PASSWORD
    );

    mockMvc.perform(
            post(SIGNIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(correctRequest)
    )
    .andExpect(status().isLocked())
    .andExpect(
            jsonPath(
                    "$.error.code"
            ).value("AUTH_ACCOUNT_LOCKED")
    );
}


// ============================================================
// A-20 — Signin body validation
// ============================================================

@Test
@DisplayName(
        "A-20 - Signin thiếu identifier phải bị từ chối"
)
void signin_missingIdentifier_shouldReturnInvalidInput()
        throws Exception {

    String request = """
        {
          "password": "%s"
        }
        """.formatted(PASSWORD);

    mockMvc.perform(
            post(SIGNIN_URL)
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


@Test
@DisplayName(
        "A-21 - Signin thiếu password phải bị từ chối"
)
void signin_missingPassword_shouldReturnInvalidInput()
        throws Exception {

    String email =
            uniqueEmail("signin-validation");

    String request = """
        {
          "identifier": "%s"
        }
        """.formatted(email);

    mockMvc.perform(
            post(SIGNIN_URL)
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
}


