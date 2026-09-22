package com.ecommerce.authuser.outbox.application;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationCommandEnvelopeSchemaContractTest {

    private static final Set<String> REQUIRED_FIELDS = Set.of(
            "event_id",
            "schema_version",
            "command_type",
            "occurred_at",
            "dedupe_key",
            "user_id",
            "channel",
            "recipient",
            "template",
            "data"
    );

    private static final Set<String> ALLOWED_FIELDS = REQUIRED_FIELDS;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void schemaFile_shouldContainRequiredNotificationCommandContract()
            throws Exception {
        String schema = Files.readString(
                Path.of("docs/contracts/schema/notification-command-v1.schema.json")
        );

        assertThat(schema)
                .contains("\"additionalProperties\": false")
                .contains("\"AUTH_VERIFICATION_REQUESTED\"")
                .contains("\"PASSWORD_RESET_REQUESTED\"")
                .contains("\"PHONE_OTP_REQUESTED\"")
                .contains("\"EMAIL\"")
                .contains("\"SMS\"");

        for (String field : REQUIRED_FIELDS) {
            assertThat(schema).contains("\"" + field + "\"");
        }
    }

    @Test
    void authVerificationEnvelope_shouldMatchNotificationCommandSchema()
            throws Exception {
        UUID userId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.create(
                OutboxAggregateType.USER,
                userId,
                "AUTH_VERIFICATION_REQUESTED",
                (short) 1,
                userId.toString(),
                Map.of("protected", true)
        );

        Map<String, Object> payload = Map.of(
                "command_type", "AUTH_VERIFICATION_REQUESTED",
                "user_id", userId.toString(),
                "channel", "EMAIL",
                "recipient", "smoke@example.com",
                "template", "auth-email-verification-v1",
                "dedupe_key", "email-verification:" + userId + ":token-id",
                "data", Map.of(
                        "display_name", "Smoke Test User",
                        "verification_url", "https://taca.vn/verify?t=opaque-token",
                        "expires_in_minutes", 30
                )
        );

        Map<String, Object> body = NotificationCommandEnvelope
                .from(event, payload)
                .toMessageBody();

        assertNotificationCommandSchema(body);

        assertThat(body)
                .containsEntry("command_type", "AUTH_VERIFICATION_REQUESTED")
                .containsEntry("channel", "EMAIL")
                .containsEntry("recipient", "smoke@example.com")
                .containsEntry("template", "auth-email-verification-v1");

        assertThat(body.toString())
                .doesNotContain("verification_token")
                .doesNotContain("reset_token")
                .doesNotContain("\"otp\"");
    }

    @Test
    void passwordResetEnvelope_shouldMatchNotificationCommandSchema()
            throws Exception {
        UUID userId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.create(
                OutboxAggregateType.USER,
                userId,
                "PASSWORD_RESET_REQUESTED",
                (short) 1,
                userId.toString(),
                Map.of("protected", true)
        );

        Map<String, Object> payload = Map.of(
                "command_type", "PASSWORD_RESET_REQUESTED",
                "user_id", userId.toString(),
                "channel", "EMAIL",
                "recipient", "smoke@example.com",
                "template", "auth-password-reset-v1",
                "dedupe_key", "password-reset:" + userId + ":token-id",
                "data", Map.of(
                        "display_name", "Smoke Test User",
                        "reset_url", "https://taca.vn/reset-password?token=opaque-token",
                        "expires_in_minutes", 30
                )
        );

        Map<String, Object> body = NotificationCommandEnvelope
                .from(event, payload)
                .toMessageBody();

        assertNotificationCommandSchema(body);

        assertThat(body)
                .containsEntry("command_type", "PASSWORD_RESET_REQUESTED")
                .containsEntry("channel", "EMAIL")
                .containsEntry("template", "auth-password-reset-v1");

        assertThat(body.toString())
                .doesNotContain("verification_token")
                .doesNotContain("reset_token")
                .doesNotContain("\"otp\"");
    }

    @Test
    void phoneOtpEnvelope_shouldMatchNotificationCommandSchema()
            throws Exception {
        UUID userId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.create(
                OutboxAggregateType.USER,
                userId,
                "PHONE_OTP_REQUESTED",
                (short) 1,
                userId.toString(),
                Map.of("protected", true)
        );

        Map<String, Object> payload = Map.of(
                "command_type", "PHONE_OTP_REQUESTED",
                "user_id", userId.toString(),
                "channel", "SMS",
                "recipient", "+84901234567",
                "template", "auth-phone-otp-v1",
                "dedupe_key", "phone-otp:challenge-id",
                "data", Map.of(
                        "challenge_id", "challenge-id",
                        "expires_in_minutes", 5
                )
        );

        Map<String, Object> body = NotificationCommandEnvelope
                .from(event, payload)
                .toMessageBody();

        assertNotificationCommandSchema(body);

        assertThat(body)
                .containsEntry("command_type", "PHONE_OTP_REQUESTED")
                .containsEntry("channel", "SMS")
                .containsEntry("template", "auth-phone-otp-v1");

        assertThat(body.toString())
                .doesNotContain("verification_token")
                .doesNotContain("reset_token")
                .doesNotContain("\"otp\"")
                .doesNotContain("raw_otp");
    }

    private void assertNotificationCommandSchema(
            Map<String, Object> body
    ) throws Exception {
        assertThat(body.keySet())
                .containsExactlyInAnyOrderElementsOf(ALLOWED_FIELDS);

        assertThat(body.keySet())
                .containsAll(REQUIRED_FIELDS);

        assertThat(body.get("event_id"))
                .isInstanceOf(String.class);
        UUID.fromString((String) body.get("event_id"));

        assertThat(body.get("schema_version"))
                .isEqualTo((short) 1);

        assertThat(body.get("command_type"))
                .isInstanceOf(String.class)
                .isIn(
                        "AUTH_VERIFICATION_REQUESTED",
                        "PASSWORD_RESET_REQUESTED",
                        "PHONE_OTP_REQUESTED"
                );

        assertThat(body.get("occurred_at"))
                .isInstanceOf(String.class);
        Instant.parse((String) body.get("occurred_at"));

        assertThat(body.get("dedupe_key"))
                .isInstanceOf(String.class)
                .asString()
                .isNotBlank();

        assertThat(body.get("user_id"))
                .isInstanceOf(String.class);
        UUID.fromString((String) body.get("user_id"));

        assertThat(body.get("channel"))
                .isInstanceOf(String.class)
                .isIn("EMAIL", "SMS");

        assertThat(body.get("recipient"))
                .isInstanceOf(String.class)
                .asString()
                .isNotBlank();

        assertThat(body.get("template"))
                .isInstanceOf(String.class)
                .asString()
                .isNotBlank();

        assertThat(body.get("data"))
                .isInstanceOf(Map.class);

        String json = objectMapper.writeValueAsString(body);

        assertThat(json)
                .contains("\"event_id\"")
                .contains("\"schema_version\"")
                .contains("\"command_type\"")
                .contains("\"dedupe_key\"")
                .contains("\"data\"")
                .doesNotContain("\"payload\"")
                .doesNotContain("\"aggregate_type\"")
                .doesNotContain("\"aggregate_id\"")
                .doesNotContain("\"partition_key\"");
    }
}
