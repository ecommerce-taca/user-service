package com.ecommerce.authuser.integration.auth;

import com.ecommerce.authuser.auth.security.SecureTokenGenerator;
import com.ecommerce.authuser.auth.security.TokenHasher;
import com.ecommerce.authuser.support.base.BaseIntegrationTest;
import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;
import com.ecommerce.authuser.token.domain.VerificationChannel;
import com.ecommerce.authuser.token.domain.VerificationPurpose;
import com.ecommerce.authuser.token.domain.VerificationToken;
import com.ecommerce.authuser.token.repository.VerificationTokenRepository;
import com.ecommerce.authuser.user.domain.User;
import com.ecommerce.authuser.support.builder.UserTestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class EmailVerificationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SecureTokenGenerator tokenGenerator;

    @Autowired
    private TokenHasher tokenHasher;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void verifyEmail_shouldVerifyUserAndCreateOutboxEvent() throws Exception {
        User user = UserTestBuilder
                .aUser()
                .withEmail("email-verify-success@test.com")
                .withPhone(null)
                .build();

        userRepository.saveAndFlush(user);

        String rawToken = tokenGenerator.generate();

        VerificationToken verificationToken = VerificationToken.create(
                user,
                VerificationChannel.EMAIL,
                VerificationPurpose.EMAIL_VERIFY,
                tokenHasher.hash(rawToken),
                "e***@test.com",
                Instant.now().plusSeconds(3600));

        verificationTokenRepository.saveAndFlush(verificationToken);

        String requestBody = objectMapper.writeValueAsString(
                new TokenRequest(rawToken));

        String responseBody = mockMvc.perform(
                post("/api/v1/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);

        assertThat(response.at("/data/user_id").asText())
                .isEqualTo(user.getId().toString());

        assertThat(response.at("/data/email_verified").asBoolean())
                .isTrue();

        assertThat(response.at("/data/verified_at").asText())
                .isNotBlank();

        User verifiedUser = userRepository
                .findById(user.getId())
                .orElseThrow();

        assertThat(verifiedUser.getEmailVerifiedAt())
                .isNotNull();

        VerificationToken usedToken = verificationTokenRepository
                .findById(verificationToken.getId())
                .orElseThrow();

        assertThat(usedToken.getUsedAt())
                .isNotNull();

        List<OutboxEvent> events = outboxEventRepository
                .findAllByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                        OutboxAggregateType.USER,
                        user.getId());

        OutboxEvent verificationEvent = events.stream()
                .filter(event -> "user.email_verified".equals(event.getEventType()))
                .findFirst()
                .orElseThrow();

        assertThat(verificationEvent.getAggregateType())
                .isEqualTo(OutboxAggregateType.USER);

        assertThat(verificationEvent.getAggregateId())
                .isEqualTo(user.getId());

        assertThat(verificationEvent.getSchemaVersion())
                .isEqualTo((short) 1);

        assertThat(verificationEvent.getPartitionKey())
                .isEqualTo(user.getId().toString());

        Map<String, Object> payload = verificationEvent.getPayloadView();

        assertThat(payload)
                .containsKeys(
                        "protected",
                        "alg",
                        "key_version",
                        "iv",
                        "ciphertext");

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
    void verifyEmail_shouldRejectNonExistentToken() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new TokenRequest("non-existent-verification-token"));

        String responseBody = mockMvc.perform(
                post("/api/v1/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(
                objectMapper
                        .readTree(responseBody)
                        .at("/error/code")
                        .asText())
                .isEqualTo("AUTH_VERIFICATION_INVALID");
    }

    @Test
    void verifyEmail_shouldRejectWrongToken() throws Exception {
        User user = UserTestBuilder
                .aUser()
                .withEmail("email-verify-wrong@test.com")
                .withPhone(null)
                .build();

        userRepository.saveAndFlush(user);

        String validToken = tokenGenerator.generate();

        VerificationToken verificationToken = VerificationToken.create(
                user,
                VerificationChannel.EMAIL,
                VerificationPurpose.EMAIL_VERIFY,
                tokenHasher.hash(validToken),
                "e***@test.com",
                Instant.now().plusSeconds(3600));

        verificationTokenRepository.saveAndFlush(verificationToken);

        String requestBody = objectMapper.writeValueAsString(
                new TokenRequest("wrong-token"));

        String responseBody = mockMvc.perform(
                post("/api/v1/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(
                objectMapper
                        .readTree(responseBody)
                        .at("/error/code")
                        .asText())
                .isEqualTo("AUTH_VERIFICATION_INVALID");

        User unchangedUser = userRepository
                .findById(user.getId())
                .orElseThrow();

        assertThat(unchangedUser.getEmailVerifiedAt())
                .isNull();

        VerificationToken unchangedToken = verificationTokenRepository
                .findById(verificationToken.getId())
                .orElseThrow();

        assertThat(unchangedToken.getUsedAt())
                .isNull();
    }

    @Test
    void verifyEmail_shouldRejectUsedToken() throws Exception {
        User user = UserTestBuilder
                .aUser()
                .withEmail("email-verify-used@test.com")
                .withPhone(null)
                .build();

        userRepository.saveAndFlush(user);

        String rawToken = tokenGenerator.generate();

        VerificationToken verificationToken = VerificationToken.create(
                user,
                VerificationChannel.EMAIL,
                VerificationPurpose.EMAIL_VERIFY,
                tokenHasher.hash(rawToken),
                "e***@test.com",
                Instant.now().plusSeconds(3600));

        verificationTokenRepository.saveAndFlush(verificationToken);

        verificationToken.markUsed(Instant.now());
        verificationTokenRepository.saveAndFlush(verificationToken);

        String requestBody = objectMapper.writeValueAsString(
                new TokenRequest(rawToken));

        String responseBody = mockMvc.perform(
                post("/api/v1/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(
                objectMapper
                        .readTree(responseBody)
                        .at("/error/code")
                        .asText())
                .isEqualTo("AUTH_VERIFICATION_INVALID");

        User unchangedUser = userRepository
                .findById(user.getId())
                .orElseThrow();

        assertThat(unchangedUser.getEmailVerifiedAt())
                .isNull();
    }

    @Test
    void verifyEmail_shouldRejectRevokedToken() throws Exception {
        User user = UserTestBuilder
                .aUser()
                .withEmail("email-verify-revoked@test.com")
                .withPhone(null)
                .build();

        userRepository.saveAndFlush(user);

        String rawToken = tokenGenerator.generate();

        VerificationToken verificationToken = VerificationToken.create(
                user,
                VerificationChannel.EMAIL,
                VerificationPurpose.EMAIL_VERIFY,
                tokenHasher.hash(rawToken),
                "e***@test.com",
                Instant.now().plusSeconds(3600));

        verificationTokenRepository.saveAndFlush(verificationToken);

        verificationToken.revoke(Instant.now());
        verificationTokenRepository.saveAndFlush(verificationToken);

        String requestBody = objectMapper.writeValueAsString(
                new TokenRequest(rawToken));

        String responseBody = mockMvc.perform(
                post("/api/v1/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(
                objectMapper
                        .readTree(responseBody)
                        .at("/error/code")
                        .asText())
                .isEqualTo("AUTH_VERIFICATION_INVALID");

        User unchangedUser = userRepository
                .findById(user.getId())
                .orElseThrow();

        assertThat(unchangedUser.getEmailVerifiedAt())
                .isNull();
    }

    @Test
    void verifyEmail_shouldRejectAlreadyVerifiedUser() throws Exception {
        User user = UserTestBuilder
                .aUser()
                .withEmail("email-verify-already@test.com")
                .withPhone(null)
                .build();

        user.verifyEmail(Instant.now());

        userRepository.saveAndFlush(user);

        String rawToken = tokenGenerator.generate();

        VerificationToken verificationToken = VerificationToken.create(
                user,
                VerificationChannel.EMAIL,
                VerificationPurpose.EMAIL_VERIFY,
                tokenHasher.hash(rawToken),
                "e***@test.com",
                Instant.now().plusSeconds(3600));

        verificationTokenRepository.saveAndFlush(verificationToken);

        String requestBody = objectMapper.writeValueAsString(
                new TokenRequest(rawToken));

        String responseBody = mockMvc.perform(
                post("/api/v1/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(
                objectMapper
                        .readTree(responseBody)
                        .at("/error/code")
                        .asText())
                .isEqualTo("AUTH_VERIFICATION_INVALID");
    }

    @Test
    void verifyEmail_shouldRejectBlankToken() throws Exception {
        String requestBody = """
                {
                    "token": ""
                }
                """;

        String responseBody = mockMvc.perform(
                post("/api/v1/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(
                objectMapper
                        .readTree(responseBody)
                        .at("/error/code")
                        .asText())
                .isEqualTo("AUTH_INVALID_INPUT");
    }

    @Test
    void verifyEmail_shouldRejectNullToken() throws Exception {
        String requestBody = """
                {
                    "token": null
                }
                """;

        String responseBody = mockMvc.perform(
                post("/api/v1/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(
                objectMapper
                        .readTree(responseBody)
                        .at("/error/code")
                        .asText())
                .isEqualTo("AUTH_INVALID_INPUT");
    }

    @Test
    void verifyEmail_shouldRejectTokenExceedingMaxLength() throws Exception {
        String oversizedToken = "a".repeat(513);

        String requestBody = objectMapper.writeValueAsString(
                new TokenRequest(oversizedToken));

        String responseBody = mockMvc.perform(
                post("/api/v1/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(
                objectMapper
                        .readTree(responseBody)
                        .at("/error/code")
                        .asText())
                .isEqualTo("AUTH_INVALID_INPUT");
    }

    private record TokenRequest(
            String token) {
    }
}
