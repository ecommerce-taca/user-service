package com.ecommerce.authuser.integration.auth;

import com.ecommerce.authuser.auth.web.signup.SignupRequest;
import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.rbac.domain.UserRole;
import com.ecommerce.authuser.support.base.BaseIntegrationTest;
import com.ecommerce.authuser.support.testdata.SignupTestData;
import com.ecommerce.authuser.token.domain.RefreshToken;
import com.ecommerce.authuser.token.domain.VerificationChannel;
import com.ecommerce.authuser.token.domain.VerificationPurpose;
import com.ecommerce.authuser.token.domain.VerificationToken;
import com.ecommerce.authuser.user.domain.User;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SignupIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/auth/signup - should create buyer and all authentication side effects")
    void signup_shouldCreateUserRoleTokensAndOutboxEvents()
            throws Exception {

        SignupRequest request = SignupTestData.defaultRequest();

        String responseBody = mockMvc.perform(
                post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        request)))
                .andExpect(status().isCreated())

                .andExpect(
                        jsonPath("$.data.user.id")
                                .isNotEmpty())

                .andExpect(
                        jsonPath("$.data.user.full_name")
                                .value(
                                        SignupTestData.DEFAULT_FULL_NAME))

                .andExpect(
                        jsonPath("$.data.user.email")
                                .value(
                                        SignupTestData.DEFAULT_EMAIL))

                .andExpect(
                        jsonPath("$.data.user.email_verified")
                                .value(false))

                .andExpect(
                        jsonPath("$.data.user.phone")
                                .value(
                                        SignupTestData.DEFAULT_PHONE))

                .andExpect(
                        jsonPath("$.data.user.phone_verified")
                                .value(false))

                .andExpect(
                        jsonPath("$.data.user.roles[0]")
                                .value("BUYER"))

                .andExpect(
                        jsonPath("$.data.user.status")
                                .value("ACTIVE"))

                .andExpect(
                        jsonPath("$.data.tokens.token_type")
                                .value("Bearer"))

                .andExpect(
                        jsonPath("$.data.tokens.access_token")
                                .isNotEmpty())

                .andExpect(
                        jsonPath("$.data.tokens.refresh_token")
                                .isNotEmpty())

                .andExpect(
                        jsonPath("$.data.tokens.expires_in")
                                .value(900))

                .andExpect(
                        jsonPath("$.data.tokens.refresh_expires_in")
                                .value(2592000))

                .andExpect(
                        jsonPath("$.data.verification.email_sent")
                                .value(true))

                .andExpect(
                        jsonPath("$.data.verification.expires_at")
                                .isNotEmpty())

                .andExpect(
                        jsonPath("$.meta.request_id")
                                .isNotEmpty())

                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);

        UUID userId = UUID.fromString(
                response
                        .path("data")
                        .path("user")
                        .path("id")
                        .asText());

        assertThat(userId)
                .isNotNull();

        /*
         * ============================================================
         * 1. USER
         * ============================================================
         */

        User user = userRepository
                .findById(userId)
                .orElseThrow();

        assertThat(user.getId())
                .isEqualTo(userId);

        assertThat(user.getEmail())
                .isEqualTo(
                        SignupTestData.DEFAULT_EMAIL);

        assertThat(user.getEmailNormalized())
                .isEqualTo(
                        SignupTestData.DEFAULT_EMAIL);

        assertThat(user.getFullName())
                .isEqualTo(
                        SignupTestData.DEFAULT_FULL_NAME);

        assertThat(user.getPhone())
                .isEqualTo(
                        SignupTestData.DEFAULT_PHONE);

        assertThat(user.getPhoneNormalized())
                .isEqualTo(
                        SignupTestData.DEFAULT_PHONE);

        assertThat(user.getEmailVerifiedAt())
                .isNull();

        assertThat(user.getPhoneVerifiedAt())
                .isNull();

        assertThat(user.getStatus().name())
                .isEqualTo("ACTIVE");

        assertThat(user.getFailedLoginCount())
                .isZero();

        assertThat(user.getPasswordHash())
                .isNotBlank()
                .isNotEqualTo(
                        SignupTestData.DEFAULT_PASSWORD);

        assertThat(user.getCreatedAt())
                .isNotNull();

        assertThat(user.getUpdatedAt())
                .isNotNull();

        assertThat(user.getDeletedAt())
                .isNull();

        /*
         * ============================================================
         * 2. BUYER ROLE
         * ============================================================
         */

        List<UserRole> userRoles = userRoleRepository
                .findAllByUser_IdAndRevokedAtIsNull(
                        userId);

        assertThat(userRoles)
                .hasSize(1);

        UserRole buyerAssignment = userRoles.get(0);

        assertThat(buyerAssignment.getUser().getId())
                .isEqualTo(userId);

        assertThat(buyerAssignment.getRole())
                .isNotNull();

        assertThat(buyerAssignment.getRole().getRoleKey())
                .isEqualTo("BUYER");

        assertThat(buyerAssignment.getShop())
                .isNull();

        assertThat(buyerAssignment.getGrantedBy())
                .isNull();

        assertThat(buyerAssignment.getRevokedAt())
                .isNull();

        assertThat(buyerAssignment.isActive())
                .isTrue();

        assertThat(buyerAssignment.getGrantedAt())
                .isNotNull();

        assertThat(buyerAssignment.getUpdatedAt())
                .isNotNull();

        assertThat(
                userRoleRepository
                        .existsByUser_IdAndRole_RoleKeyAndRevokedAtIsNull(
                                userId,
                                "BUYER"))
                .isTrue();

        /*
         * ============================================================
         * 3. EMAIL VERIFICATION TOKEN
         * ============================================================
         */

        List<VerificationToken> verificationTokens = verificationTokenRepository
                .findAllByUser_IdAndPurposeAndChannelAndUsedAtIsNullAndRevokedAtIsNull(
                        userId,
                        VerificationPurpose.EMAIL_VERIFY,
                        VerificationChannel.EMAIL);

        assertThat(verificationTokens)
                .hasSize(1);

        VerificationToken verificationToken = verificationTokens.get(0);

        assertThat(verificationToken.getId())
                .isNotNull();

        assertThat(verificationToken.getUser().getId())
                .isEqualTo(userId);

        assertThat(verificationToken.getChannel())
                .isEqualTo(
                        VerificationChannel.EMAIL);

        assertThat(verificationToken.getPurpose())
                .isEqualTo(
                        VerificationPurpose.EMAIL_VERIFY);

        assertThat(verificationToken.getRecipientMasked())
                .isEqualTo("s***@test.com");

        assertThat(verificationToken.getExpiresAt())
                .isAfter(Instant.now());

        assertThat(verificationToken.getUsedAt())
                .isNull();

        assertThat(verificationToken.getRevokedAt())
                .isNull();

        assertThat(verificationToken.getAttemptCount())
                .isZero();

        assertThat(verificationToken.getCreatedAt())
                .isNotNull();

        assertThat(
                verificationToken.isUsable(
                        Instant.now()))
                .isTrue();

        /*
         * ============================================================
         * 4. REFRESH TOKEN
         * ============================================================
         */

        List<RefreshToken> refreshTokens = refreshTokenRepository
                .findAllByUser_IdAndRevokedAtIsNullAndExpiresAtAfter(
                        userId,
                        Instant.now());

        assertThat(refreshTokens)
                .hasSize(1);

        RefreshToken refreshToken = refreshTokens.get(0);

        assertThat(refreshToken.getId())
                .isNotNull();

        assertThat(refreshToken.getUser().getId())
                .isEqualTo(userId);

        assertThat(refreshToken.getFamilyId())
                .isNotNull();

        assertThat(refreshToken.getIssuedAt())
                .isNotNull();

        assertThat(refreshToken.getExpiresAt())
                .isAfter(
                        refreshToken.getIssuedAt());

        assertThat(refreshToken.getExpiresAt())
                .isAfter(Instant.now());

        assertThat(refreshToken.getRevokedAt())
                .isNull();

        assertThat(refreshToken.getRevokeReason())
                .isNull();

        assertThat(refreshToken.getReplacedByTokenId())
                .isNull();

        assertThat(refreshToken.getLastSeenAt())
                .isNull();

        assertThat(refreshToken.getCreatedAt())
                .isNotNull();

        assertThat(
                refreshToken.isRevoked())
                .isFalse();

        assertThat(
                refreshToken.isExpired(
                        Instant.now()))
                .isFalse();

        /*
         * ============================================================
         * 5. OUTBOX EVENTS
         * ============================================================
         */

        List<OutboxEvent> events = outboxEventRepository
                .findAllByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                        OutboxAggregateType.USER,
                        userId);

        assertThat(events)
                .hasSize(2);

        assertThat(
                events
                        .stream()
                        .map(OutboxEvent::getEventType))
                .containsExactlyInAnyOrder(
                        "user.created",
                        "AUTH_VERIFICATION_REQUESTED");

        assertThat(events)
                .allMatch(
                        event -> event.getAggregateType() == OutboxAggregateType.USER);

        assertThat(events)
                .allMatch(
                        event -> userId.equals(
                                event.getAggregateId()));

        assertThat(events)
                .allMatch(
                        event -> event.getSchemaVersion() == 1);

        assertThat(events)
                .allMatch(
                        event -> userId.toString().equals(
                                event.getPartitionKey()));

        assertThat(events)
                .allMatch(
                        event -> event.getCreatedAt() != null);

        assertThat(events)
                .allMatch(
                        event -> event.getPublishedAt() == null);

        assertThat(events)
                .allMatch(
                        event -> event.getFailedAt() == null);

        assertThat(events)
                .allMatch(
                        event -> event.getAttemptCount() == 0);

        /*
         * ============================================================
         * 6. USER.CREATED EVENT PROTECTED PAYLOAD
         * ============================================================
         */

        OutboxEvent userCreatedEvent = events.stream()
                .filter(
                        event -> "user.created"
                                .equals(
                                        event.getEventType()))
                .findFirst()
                .orElseThrow();

        Map<String, Object> protectedPayload = userCreatedEvent.getPayloadView();

        assertThat(protectedPayload)
                .containsEntry(
                        "alg",
                        "AES-256-GCM")
                .containsEntry(
                        "key_version",
                        "local-v1")
                .containsEntry(
                        "protected",
                        true);

        assertThat(
                protectedPayload.get("iv"))
                .isInstanceOf(String.class)
                .asString()
                .isNotBlank();

        assertThat(
                protectedPayload.get("ciphertext"))
                .isInstanceOf(String.class)
                .asString()
                .isNotBlank();

        /*
         * ============================================================
         * 7. VERIFICATION REQUEST EVENT PROTECTED PAYLOAD
         * ============================================================
         */

        OutboxEvent verificationEvent = events.stream()
                .filter(
                        event -> "AUTH_VERIFICATION_REQUESTED"
                                .equals(
                                        event.getEventType()))
                .findFirst()
                .orElseThrow();

        Map<String, Object> verificationPayload = verificationEvent.getPayloadView();

        assertThat(verificationPayload)
                .containsEntry(
                        "alg",
                        "AES-256-GCM")
                .containsEntry(
                        "key_version",
                        "local-v1")
                .containsEntry(
                        "protected",
                        true);

        assertThat(
                verificationPayload.get("iv"))
                .isInstanceOf(String.class)
                .asString()
                .isNotBlank();

        assertThat(
                verificationPayload.get("ciphertext"))
                .isInstanceOf(String.class)
                .asString()
                .isNotBlank();
    }

    @Test
    @DisplayName("POST /api/v1/auth/signup - should reject duplicate email")
    void signup_shouldRejectDuplicateEmail()
            throws Exception {

        SignupRequest firstRequest = SignupTestData.defaultRequest();

        mockMvc.perform(
                post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        firstRequest)))
                .andExpect(status().isCreated());

        long usersBefore = userRepository.count();

        long verificationTokensBefore = verificationTokenRepository.count();

        long refreshTokensBefore = refreshTokenRepository.count();

        long outboxEventsBefore = outboxEventRepository.count();

        SignupRequest duplicateRequest = SignupTestData.requestWithEmail(
                SignupTestData.DEFAULT_EMAIL);

        mockMvc.perform(
                post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        duplicateRequest)))
                .andExpect(status().isConflict());

        assertThat(userRepository.count())
                .isEqualTo(usersBefore);

        assertThat(verificationTokenRepository.count())
                .isEqualTo(verificationTokensBefore);

        assertThat(refreshTokenRepository.count())
                .isEqualTo(refreshTokensBefore);

        assertThat(outboxEventRepository.count())
                .isEqualTo(outboxEventsBefore);
    }

    @Test
    @DisplayName("POST /api/v1/auth/signup - should reject duplicate phone")
    void signup_shouldRejectDuplicatePhone()
            throws Exception {

        SignupRequest firstRequest = SignupTestData.defaultRequest();

        mockMvc.perform(
                post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        firstRequest)))
                .andExpect(status().isCreated());

        long usersBefore = userRepository.count();

        long verificationTokensBefore = verificationTokenRepository.count();

        long refreshTokensBefore = refreshTokenRepository.count();

        long outboxEventsBefore = outboxEventRepository.count();

        SignupRequest duplicatePhoneRequest = SignupTestData.request(
                "Another User",
                SignupTestData.SECOND_EMAIL,
                SignupTestData.DEFAULT_PASSWORD,
                SignupTestData.DEFAULT_PHONE);

        mockMvc.perform(
                post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        duplicatePhoneRequest)))
                .andExpect(status().isConflict());

        assertThat(userRepository.count())
                .isEqualTo(usersBefore);

        assertThat(verificationTokenRepository.count())
                .isEqualTo(verificationTokensBefore);

        assertThat(refreshTokenRepository.count())
                .isEqualTo(refreshTokensBefore);

        assertThat(outboxEventRepository.count())
                .isEqualTo(outboxEventsBefore);
    }

    @Test
    @DisplayName("POST /api/v1/auth/signup - should allow signup without phone")
    void signup_shouldAllowMissingPhone()
            throws Exception {

        SignupRequest request = SignupTestData.requestWithoutPhone();

        String responseBody = mockMvc.perform(
                post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        request)))
                .andExpect(status().isCreated())

                .andExpect(
                        jsonPath("$.data.user.phone")
                                .doesNotExist())

                .andExpect(
                        jsonPath("$.data.user.phone_verified")
                                .value(false))

                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);

        UUID userId = UUID.fromString(
                response
                        .path("data")
                        .path("user")
                        .path("id")
                        .asText());

        User user = userRepository
                .findById(userId)
                .orElseThrow();

        assertThat(user.getPhone())
                .isNull();

        assertThat(user.getPhoneNormalized())
                .isNull();

        assertThat(user.getPhoneVerifiedAt())
                .isNull();

        assertThat(
                userRoleRepository
                        .existsByUser_IdAndRole_RoleKeyAndRevokedAtIsNull(
                                userId,
                                "BUYER"))
                .isTrue();

        assertThat(
                verificationTokenRepository
                        .findAllByUser_IdAndPurposeAndChannelAndUsedAtIsNullAndRevokedAtIsNull(
                                userId,
                                VerificationPurpose.EMAIL_VERIFY,
                                VerificationChannel.EMAIL))
                .hasSize(1);

        assertThat(
                refreshTokenRepository
                        .findAllByUser_IdAndRevokedAtIsNullAndExpiresAtAfter(
                                userId,
                                Instant.now()))
                .hasSize(1);

        assertThat(
                outboxEventRepository
                        .findAllByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                                OutboxAggregateType.USER,
                                userId))
                .hasSize(2);
    }

    @Test
    @DisplayName("POST /api/v1/auth/signup - should reject invalid email")
    void signup_shouldRejectInvalidEmail()
            throws Exception {

        SignupRequest request = SignupTestData.request(
                SignupTestData.DEFAULT_FULL_NAME,
                "not-an-email",
                SignupTestData.DEFAULT_PASSWORD,
                SignupTestData.DEFAULT_PHONE);

        mockMvc.perform(
                post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        request)))
                .andExpect(status().isBadRequest());

        assertThat(
                userRepository
                        .existsByEmailNormalized(
                                "not-an-email"))
                .isFalse();
    }

    @Test
    @DisplayName("POST /api/v1/auth/signup - should reject password shorter than 12 characters")
    void signup_shouldRejectShortPassword()
            throws Exception {

        SignupRequest request = SignupTestData.request(
                SignupTestData.DEFAULT_FULL_NAME,
                SignupTestData.DEFAULT_EMAIL,
                "short",
                SignupTestData.DEFAULT_PHONE);

        mockMvc.perform(
                post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        request)))
                .andExpect(status().isBadRequest());

        assertThat(
                userRepository
                        .existsByEmailNormalized(
                                SignupTestData.DEFAULT_EMAIL))
                .isFalse();
    }

    @Test
    @DisplayName("POST /api/v1/auth/signup - should reject blank full name")
    void signup_shouldRejectBlankFullName()
            throws Exception {

        SignupRequest request = SignupTestData.request(
                "   ",
                SignupTestData.DEFAULT_EMAIL,
                SignupTestData.DEFAULT_PASSWORD,
                SignupTestData.DEFAULT_PHONE);

        mockMvc.perform(
                post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        request)))
                .andExpect(status().isBadRequest());

        assertThat(
                userRepository
                        .existsByEmailNormalized(
                                SignupTestData.DEFAULT_EMAIL))
                .isFalse();
    }

    @Test
    @DisplayName("POST /api/v1/auth/signup - should reject invalid E.164 phone")
    void signup_shouldRejectInvalidPhone()
            throws Exception {

        SignupRequest request = SignupTestData.request(
                SignupTestData.DEFAULT_FULL_NAME,
                SignupTestData.DEFAULT_EMAIL,
                SignupTestData.DEFAULT_PASSWORD,
                "0901234567");

        mockMvc.perform(
                post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        request)))
                .andExpect(status().isBadRequest());

        assertThat(
                userRepository
                        .existsByEmailNormalized(
                                SignupTestData.DEFAULT_EMAIL))
                .isFalse();
    }
}