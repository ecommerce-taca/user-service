package com.ecommerce.authuser.outbox.application;

import com.ecommerce.authuser.outbox.domain.OutboxAggregateType;
import com.ecommerce.authuser.outbox.domain.OutboxEvent;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxMessageEnvelopeSchemaContractTest {

    private static final Set<String> REQUIRED_FIELDS = Set.of(
            "event_id",
            "event_type",
            "schema_version",
            "aggregate_type",
            "aggregate_id",
            "actor_user_id",
            "occurred_at",
            "payload"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void userCreatedEnvelope_shouldMatchUserEventContract()
            throws Exception {
        UUID userId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.createWithActor(
                OutboxAggregateType.USER,
                userId,
                null,
                "user.created",
                (short) 1,
                userId.toString(),
                Map.of("protected", true)
        );

        Map<String, Object> payload = Map.of(
                "user_id", userId.toString(),
                "email", "smoke@example.com",
                "full_name", "Smoke Test User",
                "status", "PENDING_VERIFICATION",
                "created_at", "2026-09-21T10:00:00Z"
        );

        Map<String, Object> body = OutboxMessageEnvelope
                .from(event, payload)
                .toMessageBody();

        assertDomainEnvelopeSchema(body);

        assertThat(body)
                .containsEntry("event_type", "user.created")
                .containsEntry("aggregate_type", "USER")
                .containsEntry("aggregate_id", userId.toString())
                .containsEntry("actor_user_id", null);

        @SuppressWarnings("unchecked")
        Map<String, Object> bodyPayload =
                (Map<String, Object>) body.get("payload");

        assertThat(bodyPayload)
                .containsEntry("user_id", userId.toString())
                .containsEntry("email", "smoke@example.com")
                .containsEntry("full_name", "Smoke Test User")
                .containsEntry("status", "PENDING_VERIFICATION")
                .containsKey("created_at");
    }

    @Test
    void userUpdatedEnvelope_shouldMatchUserEventContract()
            throws Exception {
        UUID userId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.createWithActor(
                OutboxAggregateType.USER,
                userId,
                userId,
                "user.updated",
                (short) 1,
                userId.toString(),
                Map.of("protected", true)
        );

        Map<String, Object> payload = Map.of(
                "user_id", userId.toString(),
                "changed_fields", java.util.List.of("full_name"),
                "snapshot", Map.of("full_name", "Updated Name"),
                "updated_at", "2026-09-21T10:10:00Z"
        );

        Map<String, Object> body = OutboxMessageEnvelope
                .from(event, payload)
                .toMessageBody();

        assertDomainEnvelopeSchema(body);

        assertThat(body)
                .containsEntry("event_type", "user.updated")
                .containsEntry("aggregate_type", "USER")
                .containsEntry("actor_user_id", userId.toString());

        @SuppressWarnings("unchecked")
        Map<String, Object> bodyPayload =
                (Map<String, Object>) body.get("payload");

        assertThat(bodyPayload)
                .containsEntry("user_id", userId.toString())
                .containsKey("changed_fields")
                .containsKey("snapshot")
                .containsKey("updated_at");
    }

    @Test
    void shopUpdatedEnvelope_shouldMatchShopEventContract()
            throws Exception {
        UUID shopId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.createWithActor(
                OutboxAggregateType.SHOP,
                shopId,
                actorUserId,
                "shop.updated",
                (short) 1,
                shopId.toString(),
                Map.of("protected", true)
        );

        Map<String, Object> payload = Map.of(
                "shop_id", shopId.toString(),
                "changed_fields", java.util.List.of("name", "description"),
                "snapshot", Map.of(
                        "name", "Updated Shop",
                        "description", "Updated shop description"
                ),
                "updated_at", "2026-09-21T10:10:00Z",
                "version", 2
        );

        Map<String, Object> body = OutboxMessageEnvelope
                .from(event, payload)
                .toMessageBody();

        assertDomainEnvelopeSchema(body);

        assertThat(body)
                .containsEntry("event_type", "shop.updated")
                .containsEntry("aggregate_type", "SHOP")
                .containsEntry("aggregate_id", shopId.toString())
                .containsEntry("actor_user_id", actorUserId.toString());

        @SuppressWarnings("unchecked")
        Map<String, Object> bodyPayload =
                (Map<String, Object>) body.get("payload");

        assertThat(bodyPayload)
                .containsEntry("shop_id", shopId.toString())
                .containsKey("changed_fields")
                .containsKey("snapshot")
                .containsEntry("updated_at", "2026-09-21T10:10:00Z")
                .containsEntry("version", 2);
    }

    @Test
    void shopKycExpiredEnvelope_shouldMatchShopEventContract()
            throws Exception {
        UUID shopId = UUID.randomUUID();
        UUID kycCaseId = UUID.randomUUID();

        OutboxEvent event = OutboxEvent.createWithActor(
                OutboxAggregateType.SHOP,
                shopId,
                null,
                "shop.kyc.expired",
                (short) 1,
                shopId.toString(),
                Map.of("protected", true)
        );

        Map<String, Object> payload = Map.of(
                "shop_id", shopId.toString(),
                "kyc_case_id", kycCaseId.toString(),
                "expired_at", "2026-09-21T10:20:00Z",
                "expired_documents", java.util.List.of("BUSINESS_LICENSE")
        );

        Map<String, Object> body = OutboxMessageEnvelope
                .from(event, payload)
                .toMessageBody();

        assertDomainEnvelopeSchema(body);

        assertThat(body)
                .containsEntry("event_type", "shop.kyc.expired")
                .containsEntry("aggregate_type", "SHOP")
                .containsEntry("actor_user_id", null);

        @SuppressWarnings("unchecked")
        Map<String, Object> bodyPayload =
                (Map<String, Object>) body.get("payload");

        assertThat(bodyPayload)
                .containsEntry("shop_id", shopId.toString())
                .containsEntry("kyc_case_id", kycCaseId.toString())
                .containsEntry("expired_at", "2026-09-21T10:20:00Z")
                .containsKey("expired_documents");
    }

    private void assertDomainEnvelopeSchema(
            Map<String, Object> body
    ) throws Exception {
        assertThat(body.keySet())
                .containsExactlyInAnyOrderElementsOf(REQUIRED_FIELDS);

        assertThat(body.get("event_id"))
                .isInstanceOf(String.class);
        UUID.fromString((String) body.get("event_id"));

        assertThat(body.get("event_type"))
                .isInstanceOf(String.class)
                .asString()
                .isNotBlank();

        assertThat(((Number) body.get("schema_version")).intValue())
                .isEqualTo(1);

        assertThat(body.get("aggregate_type"))
                .isInstanceOf(String.class)
                .asString()
                .isNotBlank();

        assertThat(body.get("aggregate_id"))
                .isInstanceOf(String.class);
        UUID.fromString((String) body.get("aggregate_id"));

        Object actorUserId = body.get("actor_user_id");
        if (actorUserId != null) {
            assertThat(actorUserId).isInstanceOf(String.class);
            UUID.fromString((String) actorUserId);
        }

        assertThat(body.get("occurred_at"))
                .isInstanceOf(String.class);
        Instant.parse((String) body.get("occurred_at"));

        assertThat(body.get("payload"))
                .isInstanceOf(Map.class);

        String json = objectMapper.writeValueAsString(body);

        assertThat(json)
                .contains("\"event_id\"")
                .contains("\"event_type\"")
                .contains("\"schema_version\"")
                .contains("\"aggregate_type\"")
                .contains("\"aggregate_id\"")
                .contains("\"actor_user_id\"")
                .doesNotContain("\"partition_key\"")
                .contains("\"occurred_at\"")
                .contains("\"payload\"")
                .doesNotContain("\"command_type\"")
                .doesNotContain("\"dedupe_key\"")
                .doesNotContain("\"template\"");
    }
}