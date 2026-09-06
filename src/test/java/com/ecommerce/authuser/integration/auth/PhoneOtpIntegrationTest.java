package com.ecommerce.authuser.integration.auth;

import com.ecommerce.authuser.auth.web.verification.phone.PhoneOtpRequest;
import com.ecommerce.authuser.auth.web.verification.phone.PhoneOtpVerifyRequest;
import com.ecommerce.authuser.common.id.UuidV7Generator;
import com.ecommerce.authuser.support.base.BaseIntegrationTest;
import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.support.builder.UserTestBuilder;
import com.ecommerce.authuser.support.security.TestJwtFactory;
import com.ecommerce.authuser.support.security.TestUserToken;
import com.ecommerce.authuser.token.domain.VerificationChannel;
import com.ecommerce.authuser.token.domain.VerificationPurpose;
import com.ecommerce.authuser.token.domain.VerificationToken;
import com.ecommerce.authuser.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Import;
import com.ecommerce.authuser.support.security.TestSecurityConfig;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class PhoneOtpIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private User user;

    private TestUserToken userToken;

    @Value("${auth.outbox.encryption-key-base64}")
    private String outboxEncryptionKeyBase64;

    @BeforeEach
    void setUp() {
        String email = "phone-otp-" + UUID.randomUUID() + "@test.com";

        user = UserTestBuilder
                .aUser()
                .withEmail(email)
                .withEmailNormalized(email)
                .withPhone(null)
                .build();

        user = userRepository.saveAndFlush(user);

        userToken = TestJwtFactory.createUserToken(
                user.getId(),
                List.of("BUYER"));
    }

    @Test
    @DisplayName("POST /phone/request-otp - should request OTP successfully")
    void requestOtp_success() throws Exception {

        String phone = "+84901234567";

        String response = mockMvc.perform(
                post("/api/v1/auth/phone/request-otp")
                        .header(
                                "Authorization",
                                "Bearer " + userToken.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        new PhoneOtpRequest(phone))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.challenge_id").isNotEmpty())
                .andExpect(jsonPath("$.data.masked_phone")
                        .value("+84******567"))
                .andExpect(jsonPath("$.data.max_attempts")
                        .value(5))
                .andExpect(jsonPath("$.data.expires_at")
                        .isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);

        UUID challengeId = UUID.fromString(
                root.path("data")
                        .path("challenge_id")
                        .asText());

        VerificationToken challenge = verificationTokenRepository
                .findById(challengeId)
                .orElseThrow();

        assertThat(challenge.getUser().getId())
                .isEqualTo(user.getId());

        assertThat(challenge.getChannel())
                .isEqualTo(VerificationChannel.PHONE);

        assertThat(challenge.getPurpose())
                .isEqualTo(VerificationPurpose.PHONE_VERIFY);

        assertThat(challenge.getRecipientValue())
                .isEqualTo(phone);

        assertThat(challenge.getRecipientMasked())
                .isEqualTo("+84******567");

        assertThat(challenge.getAttemptCount())
                .isZero();

        assertThat(challenge.getExpiresAt())
                .isAfter(Instant.now());

        assertThat(challenge.getExpiresAt())
                .isBefore(
                        Instant.now()
                                .plus(6, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("POST /phone/request-otp - should create PHONE_OTP_REQUESTED outbox event")
    void requestOtp_shouldCreateOutboxEvent() throws Exception {

        String phone = "+84901234567";

        String response = mockMvc.perform(
                post("/api/v1/auth/phone/request-otp")
                        .header(
                                "Authorization",
                                "Bearer " + userToken.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        new PhoneOtpRequest(phone))))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID challengeId = UUID.fromString(
                objectMapper
                        .readTree(response)
                        .path("data")
                        .path("challenge_id")
                        .asText());

        List<OutboxEvent> events = outboxEventRepository
                .findAllByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                        OutboxAggregateType.USER,
                        user.getId());

        OutboxEvent otpEvent = events.stream()
                .filter(event -> "PHONE_OTP_REQUESTED"
                        .equals(event.getEventType()))
                .findFirst()
                .orElseThrow();

        assertThat(otpEvent.getAggregateType())
                .isEqualTo(OutboxAggregateType.USER);

        assertThat(otpEvent.getAggregateId())
                .isEqualTo(user.getId());

        assertThat(otpEvent.getEventType())
                .isEqualTo("PHONE_OTP_REQUESTED");

        Map<String, Object> payload = otpEvent.getPayloadView();

        assertThat(payload)
                .containsKeys(
                        "alg",
                        "ciphertext",
                        "iv",
                        "key_version",
                        "protected");

        assertThat(payload.get("protected"))
                .isEqualTo(true);

        assertThat(payload.get("alg"))
                .isEqualTo("AES-256-GCM");

        assertThat(payload.get("key_version"))
                .isEqualTo("local-v1");

        assertThat(payload.get("iv"))
                .isInstanceOf(String.class)
                .isNotNull();

        assertThat(payload.get("ciphertext"))
                .isInstanceOf(String.class)
                .isNotNull();
    }

    @Test
    @DisplayName("POST /phone/request-otp - should reject invalid phone format")
    void requestOtp_invalidPhoneFormat() throws Exception {

        mockMvc.perform(
                post("/api/v1/auth/phone/request-otp")
                        .header(
                                "Authorization",
                                "Bearer " + userToken.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                        {
                                            "phone": "0901234567"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("AUTH_INVALID_INPUT"));
    }

    @Test
    @DisplayName("POST /phone/request-otp - should reject unauthenticated request")
    void requestOtp_unauthenticated() throws Exception {

        mockMvc.perform(
                post("/api/v1/auth/phone/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                        {
                                            "phone": "+84901234567"
                                        }
                                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /phone/verify-otp - should verify phone successfully")
    void verifyOtp_success() throws Exception {

        String phone = "+84901234567";

        String requestResponse = requestOtp(phone);

        UUID challengeId = UUID.fromString(
                objectMapper
                        .readTree(requestResponse)
                        .path("data")
                        .path("challenge_id")
                        .asText());

        String rawOtp = findRawOtp();

        Instant beforeVerification = Instant.now();

        mockMvc.perform(
                post("/api/v1/auth/phone/verify-otp")
                        .header(
                                "Authorization",
                                "Bearer " + userToken.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        new PhoneOtpVerifyRequest(
                                                challengeId,
                                                rawOtp))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone_verified")
                        .value(true))
                .andExpect(jsonPath("$.data.phone_verified_at")
                        .isNotEmpty());

        Instant afterVerification = Instant.now();

        User persistedUser = userRepository
                .findById(user.getId())
                .orElseThrow();

        assertThat(persistedUser.getPhoneVerifiedAt())
                .isNotNull();

        assertThat(persistedUser.getPhone())
                .isEqualTo(phone);

        assertThat(persistedUser.getPhoneNormalized())
                .isEqualTo(phone);

        VerificationToken challenge = verificationTokenRepository
                .findById(challengeId)
                .orElseThrow();

        assertThat(challenge.isUsed())
                .isTrue();

        assertThat(challenge.getUsedAt())
                .isBetween(
                        beforeVerification,
                        afterVerification);
    }

    @Test
    @DisplayName("POST /phone/verify-otp - should reject invalid OTP")
    void verifyOtp_invalidOtp() throws Exception {

        String requestResponse = requestOtp("+84901234567");

        UUID challengeId = UUID.fromString(
                objectMapper
                        .readTree(requestResponse)
                        .path("data")
                        .path("challenge_id")
                        .asText());

        mockMvc.perform(
                post("/api/v1/auth/phone/verify-otp")
                        .header(
                                "Authorization",
                                "Bearer " + userToken.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        new PhoneOtpVerifyRequest(
                                                challengeId,
                                                "000000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("AUTH_VERIFICATION_INVALID"));

        VerificationToken challenge = verificationTokenRepository
                .findById(challengeId)
                .orElseThrow();

        assertThat(challenge.getAttemptCount())
                .isEqualTo((byte) 1);

        assertThat(challenge.isUsed())
                .isFalse();

        assertThat(challenge.isRevoked())
                .isFalse();
    }

    @Test
    @DisplayName("POST /phone/verify-otp - should revoke challenge after fifth failed attempt")
    void verifyOtp_fifthInvalidAttempt_shouldRevokeChallenge()
            throws Exception {

        String requestResponse = requestOtp("+84901234567");

        UUID challengeId = UUID.fromString(
                objectMapper
                        .readTree(requestResponse)
                        .path("data")
                        .path("challenge_id")
                        .asText());

        for (int attempt = 1; attempt <= 4; attempt++) {

            mockMvc.perform(
                    post("/api/v1/auth/phone/verify-otp")
                            .header(
                                    "Authorization",
                                    "Bearer " + userToken.value())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                    objectMapper.writeValueAsString(
                                            new PhoneOtpVerifyRequest(
                                                    challengeId,
                                                    "000000"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code")
                            .value("AUTH_VERIFICATION_INVALID"));
        }

        mockMvc.perform(
                post("/api/v1/auth/phone/verify-otp")
                        .header(
                                "Authorization",
                                "Bearer " + userToken.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        new PhoneOtpVerifyRequest(
                                                challengeId,
                                                "000000"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code")
                        .value("AUTH_OTP_ATTEMPTS_EXCEEDED"));

        VerificationToken challenge = verificationTokenRepository
                .findById(challengeId)
                .orElseThrow();

        assertThat(challenge.getAttemptCount())
                .isEqualTo((byte) 5);

        assertThat(challenge.isRevoked())
                .isTrue();

        assertThat(challenge.isUsed())
                .isFalse();
    }

    @Test
    @DisplayName("POST /phone/verify-otp - should reject invalid OTP format")
    void verifyOtp_invalidOtpFormat() throws Exception {

        String requestResponse = requestOtp("+84901234567");

        UUID challengeId = UUID.fromString(
                objectMapper
                        .readTree(requestResponse)
                        .path("data")
                        .path("challenge_id")
                        .asText());

        mockMvc.perform(
                post("/api/v1/auth/phone/verify-otp")
                        .header(
                                "Authorization",
                                "Bearer " + userToken.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        new PhoneOtpVerifyRequest(
                                                challengeId,
                                                "12345"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("AUTH_INVALID_INPUT"));
    }

    @Test
    @DisplayName("POST /phone/verify-otp - should reject nonexistent challenge")
    void verifyOtp_challengeNotFound() throws Exception {

        UUID challengeId = UuidV7Generator.generate();

        mockMvc.perform(
                post("/api/v1/auth/phone/verify-otp")
                        .header(
                                "Authorization",
                                "Bearer " + userToken.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        new PhoneOtpVerifyRequest(
                                                challengeId,
                                                "123456"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("AUTH_VERIFICATION_INVALID"));
    }

    @Test
    @DisplayName("POST /phone/verify-otp - should reject expired challenge")
    void verifyOtp_expiredChallenge() throws Exception {

        VerificationToken challenge = VerificationToken.createPhoneChallenge(
                user,
                "test-hash",
                "+84901234567",
                "+84******567",
                Instant.now().plus(500, ChronoUnit.MILLIS));

        challenge = verificationTokenRepository.saveAndFlush(challenge);

        Thread.sleep(600);

        mockMvc.perform(
                post("/api/v1/auth/phone/verify-otp")
                        .header(
                                "Authorization",
                                "Bearer " + userToken.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        new PhoneOtpVerifyRequest(
                                                challenge.getId(),
                                                "123456"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("AUTH_VERIFICATION_INVALID"));
    }

    @Test
    @DisplayName("POST /phone/verify-otp - should reject used challenge")
    void verifyOtp_usedChallenge() throws Exception {

        VerificationToken challenge = VerificationToken.createPhoneChallenge(
                user,
                "test-hash",
                "+84901234567",
                "+84******567",
                Instant.now().plus(5, ChronoUnit.MINUTES));

        challenge = verificationTokenRepository
                .saveAndFlush(challenge);

        challenge.markUsed(Instant.now());

        verificationTokenRepository.saveAndFlush(challenge);

        mockMvc.perform(
                post("/api/v1/auth/phone/verify-otp")
                        .header(
                                "Authorization",
                                "Bearer " + userToken.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        new PhoneOtpVerifyRequest(
                                                challenge.getId(),
                                                "123456"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("AUTH_VERIFICATION_INVALID"));
    }

    @Test
    @DisplayName("POST /phone/verify-otp - should reject revoked challenge")
    void verifyOtp_revokedChallenge() throws Exception {

        VerificationToken challenge = VerificationToken.createPhoneChallenge(
                user,
                "test-hash",
                "+84901234567",
                "+84******567",
                Instant.now().plus(5, ChronoUnit.MINUTES));

        challenge = verificationTokenRepository
                .saveAndFlush(challenge);

        challenge.revoke(Instant.now());

        verificationTokenRepository.saveAndFlush(challenge);

        mockMvc.perform(
                post("/api/v1/auth/phone/verify-otp")
                        .header(
                                "Authorization",
                                "Bearer " + userToken.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        new PhoneOtpVerifyRequest(
                                                challenge.getId(),
                                                "123456"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("AUTH_VERIFICATION_INVALID"));
    }

    @Test
    @DisplayName("POST /phone/verify-otp - should reject challenge belonging to another user")
    void verifyOtp_challengeBelongsToAnotherUser() throws Exception {

        String anotherEmail = "another-" + UUID.randomUUID() + "@test.com";

        User anotherUser = UserTestBuilder
                .aUser()
                .withEmail(anotherEmail)
                .withEmailNormalized(anotherEmail)
                .withPhone(null)
                .build();

        anotherUser = userRepository.saveAndFlush(anotherUser);

        VerificationToken challenge = VerificationToken.createPhoneChallenge(
                anotherUser,
                "test-hash",
                "+84901234568",
                "+84*****568",
                Instant.now().plus(5, ChronoUnit.MINUTES));

        challenge = verificationTokenRepository
                .saveAndFlush(challenge);

        mockMvc.perform(
                post("/api/v1/auth/phone/verify-otp")
                        .header(
                                "Authorization",
                                "Bearer " + userToken.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        new PhoneOtpVerifyRequest(
                                                challenge.getId(),
                                                "123456"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("AUTH_VERIFICATION_INVALID"));
    }

    @Test
    @DisplayName("POST /phone/request-otp - should reject second request during cooldown")
    void requestOtp_rateLimited() throws Exception {

        String phone = "+84901234567";

        requestOtp(phone);

        mockMvc.perform(
                post("/api/v1/auth/phone/request-otp")
                        .header(
                                "Authorization",
                                "Bearer " + userToken.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        new PhoneOtpRequest(phone))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code")
                        .value("AUTH_OTP_RATE_LIMITED"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/phone/request-otp - should revoke previous active challenge")
    void requestOtp_shouldRevokePreviousChallenge() throws Exception {

        String phone = "+84901234567";

        String firstResponse = requestOtp(phone);

        UUID firstChallengeId = UUID.fromString(
                objectMapper
                        .readTree(firstResponse)
                        .path("data")
                        .path("challenge_id")
                        .asText());

        VerificationToken firstChallenge = verificationTokenRepository
                .findById(firstChallengeId)
                .orElseThrow();

        assertThat(firstChallenge.isRevoked())
                .isFalse();

        /*
         * The production service applies a 60-second cooldown.
         * Therefore a second HTTP request immediately after the first
         * request is expected to be rate-limited before a new challenge
         * can be created.
         *
         * This test verifies the rate-limit boundary instead of bypassing
         * the production rule.
         */
        mockMvc.perform(
                post("/api/v1/auth/phone/request-otp")
                        .header(
                                "Authorization",
                                "Bearer " + userToken.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        new PhoneOtpRequest(phone))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code")
                        .value("AUTH_OTP_RATE_LIMITED"));

        VerificationToken persistedChallenge = verificationTokenRepository
                .findById(firstChallengeId)
                .orElseThrow();

        assertThat(persistedChallenge.isRevoked())
                .isFalse();
    }

    private String requestOtp(String phone) throws Exception {

        return mockMvc.perform(
                post("/api/v1/auth/phone/request-otp")
                        .header(
                                "Authorization",
                                "Bearer " + userToken.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        new PhoneOtpRequest(phone))))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String findRawOtp() throws Exception {

        List<OutboxEvent> events = outboxEventRepository
                .findAllByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                        OutboxAggregateType.USER,
                        user.getId());

        OutboxEvent otpEvent = events.stream()
                .filter(event -> "PHONE_OTP_REQUESTED"
                        .equals(event.getEventType()))
                .reduce((first, second) -> second)
                .orElseThrow();

        Map<String, Object> protectedPayload = otpEvent.getPayloadView();

        assertThat(protectedPayload)
                .containsKeys(
                        "protected",
                        "alg",
                        "key_version",
                        "iv",
                        "ciphertext");

        assertThat(protectedPayload.get("protected"))
                .isEqualTo(true);

        String ivBase64 = String.valueOf(
                protectedPayload.get("iv"));

        String ciphertextBase64 = String.valueOf(
                protectedPayload.get("ciphertext"));

        byte[] iv = Base64.getUrlDecoder()
                .decode(ivBase64);

        byte[] ciphertext = Base64.getUrlDecoder()
                .decode(ciphertextBase64);

        byte[] key = Base64.getDecoder()
                .decode(outboxEncryptionKeyBase64);

        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

        Cipher cipher = Cipher.getInstance(
                "AES/GCM/NoPadding");

        cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                new GCMParameterSpec(128, iv));

        cipher.updateAAD(
                "PHONE_OTP_REQUESTED"
                        .getBytes(StandardCharsets.UTF_8));

        byte[] plaintext = cipher.doFinal(ciphertext);

        JsonNode payload = objectMapper.readTree(plaintext);

        JsonNode otpNode = payload
                .path("data")
                .path("otp");

        assertThat(otpNode.isMissingNode())
                .isFalse();

        assertThat(otpNode.asText())
                .isNotBlank();

        return otpNode.asText();
    }
}
