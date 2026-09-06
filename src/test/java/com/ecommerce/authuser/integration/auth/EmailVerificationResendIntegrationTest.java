package com.ecommerce.authuser.integration.auth;

import com.ecommerce.authuser.auth.web.signup.SignupRequest;
import com.ecommerce.authuser.support.base.BaseIntegrationTest;
import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;
import com.ecommerce.authuser.outbox.repository.OutboxEventRepository;
import com.ecommerce.authuser.token.domain.VerificationChannel;
import com.ecommerce.authuser.token.domain.VerificationPurpose;
import com.ecommerce.authuser.token.domain.VerificationToken;
import com.ecommerce.authuser.token.repository.VerificationTokenRepository;
import com.ecommerce.authuser.user.domain.User;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class EmailVerificationResendIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void resendEmail_shouldCreateNewTokenRevokeOldTokenAndCreateOutboxEvent()
            throws Exception {

        SignupContext context = signupUser(
                "email-resend-success@test.com");

        List<VerificationToken> beforeTokens = verificationTokenRepository
                .findAllByUser_IdAndPurposeAndChannelAndUsedAtIsNullAndRevokedAtIsNull(
                        context.userId(),
                        VerificationPurpose.EMAIL_VERIFY,
                        VerificationChannel.EMAIL);

        assertThat(beforeTokens)
                .hasSize(1);

        VerificationToken oldToken = beforeTokens.get(0);

        String responseBody = mockMvc.perform(
                post("/api/v1/auth/email/resend")
                        .header(
                                "Authorization",
                                "Bearer " + context.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);

        assertThat(response.at("/data/accepted").asBoolean())
                .isTrue();

        assertThat(response.at("/data/expires_at").asText())
                .isNotBlank();

        VerificationToken persistedOldToken = verificationTokenRepository
                .findById(oldToken.getId())
                .orElseThrow();

        assertThat(persistedOldToken.getRevokedAt())
                .isNotNull();

        List<VerificationToken> activeTokens = verificationTokenRepository
                .findAllByUser_IdAndPurposeAndChannelAndUsedAtIsNullAndRevokedAtIsNull(
                        context.userId(),
                        VerificationPurpose.EMAIL_VERIFY,
                        VerificationChannel.EMAIL);

        assertThat(activeTokens)
                .hasSize(1);

        VerificationToken newToken = activeTokens.get(0);

        assertThat(newToken.getId())
                .isNotEqualTo(oldToken.getId());

        assertThat(newToken.getChannel())
                .isEqualTo(VerificationChannel.EMAIL);

        assertThat(newToken.getPurpose())
                .isEqualTo(VerificationPurpose.EMAIL_VERIFY);

        assertThat(newToken.getExpiresAt())
                .isAfter(oldToken.getExpiresAt());

        List<OutboxEvent> events = outboxEventRepository
                .findAllByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                        OutboxAggregateType.USER,
                        context.userId());

        List<OutboxEvent> verificationEvents = events.stream()
                .filter(event -> "AUTH_VERIFICATION_REQUESTED"
                        .equals(event.getEventType()))
                .toList();

        assertThat(verificationEvents)
                .hasSize(2);

        OutboxEvent resendEvent = verificationEvents.get(1);

        assertThat(resendEvent.getAggregateType())
                .isEqualTo(OutboxAggregateType.USER);

        assertThat(resendEvent.getAggregateId())
                .isEqualTo(context.userId());

        assertThat(resendEvent.getSchemaVersion())
                .isEqualTo((short) 1);

        assertThat(resendEvent.getPartitionKey())
                .isEqualTo(context.userId().toString());

        Map<String, Object> payload = resendEvent.getPayloadView();

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
    void resendEmail_shouldRejectUnauthenticatedRequest()
            throws Exception {

        mockMvc.perform(
                post("/api/v1/auth/email/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resendEmail_shouldRejectAlreadyVerifiedUser()
            throws Exception {

        SignupContext context = signupUser(
                "email-resend-verified@test.com");

        User user = userRepository
                .findById(context.userId())
                .orElseThrow();

        user.verifyEmail(java.time.Instant.now());

        userRepository.saveAndFlush(user);

        String responseBody = mockMvc.perform(
                post("/api/v1/auth/email/resend")
                        .header(
                                "Authorization",
                                "Bearer " + context.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(
                objectMapper
                        .readTree(responseBody)
                        .at("/error/code")
                        .asText())
                .isEqualTo("AUTH_VERIFICATION_ALREADY_COMPLETE");
    }

    @Test
    void resendEmail_shouldEnforceThreeResendsPerHour()
            throws Exception {

        SignupContext context = signupUser(
                "email-resend-limit@test.com");

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(
                    post("/api/v1/auth/email/resend")
                            .header(
                                    "Authorization",
                                    "Bearer " + context.accessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isAccepted());
        }

        String responseBody = mockMvc.perform(
                post("/api/v1/auth/email/resend")
                        .header(
                                "Authorization",
                                "Bearer " + context.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(
                objectMapper
                        .readTree(responseBody)
                        .at("/error/code")
                        .asText())
                .isEqualTo("AUTH_RESEND_LIMIT_EXCEEDED");
    }

    @Test
    void resendEmail_shouldPreserveRequestId()
            throws Exception {

        SignupContext context = signupUser(
                "email-resend-request-id@test.com");

        String requestId = "test-email-resend-request-id";

        String responseBody = mockMvc.perform(
                post("/api/v1/auth/email/resend")
                        .header(
                                "Authorization",
                                "Bearer " + context.accessToken())
                        .header(
                                "X-Request-ID",
                                requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);

        assertThat(response.at("/meta/request_id").asText())
                .isEqualTo(requestId);
    }

    private SignupContext signupUser(String email)
            throws Exception {

        SignupRequest request = new SignupRequest(
                "Email Test User",
                email,
                "Password123456!",
                null);

        String requestBody = objectMapper.writeValueAsString(request);

        String responseBody = mockMvc.perform(
                post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);

        String userId = response
                .at("/data/user/id")
                .asText();

        String accessToken = response
                .at("/data/tokens/access_token")
                .asText();

        return new SignupContext(
                java.util.UUID.fromString(userId),
                email,
                accessToken);
    }

    private record SignupContext(
            java.util.UUID userId,
            String email,
            String accessToken) {
    }
}
