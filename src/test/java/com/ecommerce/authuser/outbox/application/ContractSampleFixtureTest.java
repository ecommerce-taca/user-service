package com.ecommerce.authuser.outbox.application;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ContractSampleFixtureTest {

    private static final Path SAMPLE_DIR = Path.of(
            "docs/contracts/samples"
    );

    private static final Set<String> SECRET_FIELD_NAMES = Set.of(
            "original_payload",
            "verification_token",
            "reset_token",
            "otp",
            "raw_otp",
            "password",
            "password_hash"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sampleFixtures_shouldBeValidJsonFiles() throws Exception {
        List<Path> files = jsonSampleFiles();

        assertThat(files)
                .isNotEmpty();

        for (Path file : files) {
            JsonNode root = readJson(file);

            assertThat(root.isObject())
                    .as(file.toString())
                    .isTrue();

            assertThat(root.has("event_id"))
                    .as(file.toString())
                    .isTrue();

            assertThat(root.has("schema_version"))
                    .as(file.toString())
                    .isTrue();

            assertThat(root.has("occurred_at"))
                    .as(file.toString())
                    .isTrue();
        }
    }

    @Test
    void notificationSamples_shouldMatchNotificationCommandShape()
            throws Exception {
        for (Path file : jsonSampleFiles("notification-")) {
            JsonNode root = readJson(file);

            assertThat(root.has("command_type"))
                    .as(file.toString())
                    .isTrue();
            assertThat(root.has("dedupe_key"))
                    .as(file.toString())
                    .isTrue();
            assertThat(root.has("user_id"))
                    .as(file.toString())
                    .isTrue();
            assertThat(root.has("channel"))
                    .as(file.toString())
                    .isTrue();
            assertThat(root.has("recipient"))
                    .as(file.toString())
                    .isTrue();
            assertThat(root.has("template"))
                    .as(file.toString())
                    .isTrue();
            assertThat(root.has("data"))
                    .as(file.toString())
                    .isTrue();

            assertThat(root.has("event_type"))
                    .as(file.toString())
                    .isFalse();
            assertThat(root.has("aggregate_type"))
                    .as(file.toString())
                    .isFalse();
            assertThat(root.has("aggregate_id"))
                    .as(file.toString())
                    .isFalse();
            assertThat(root.has("payload"))
                    .as(file.toString())
                    .isFalse();
            assertThat(root.has("partition_key"))
                    .as(file.toString())
                    .isFalse();
        }
    }

    @Test
    void userEventSamples_shouldMatchDomainEventShape()
            throws Exception {
        for (Path file : jsonSampleFiles("user-")) {
            JsonNode root = readJson(file);

            assertDomainEventShape(file, root);

            assertThat(root.get("aggregate_type").asText())
                    .as(file.toString())
                    .isEqualTo("USER");

            assertThat(root.get("event_type").asText())
                    .as(file.toString())
                    .startsWith("user.");
        }
    }

    @Test
    void shopEventSamples_shouldMatchDomainEventShape()
            throws Exception {
        for (Path file : jsonSampleFiles("shop-")) {
            JsonNode root = readJson(file);

            assertDomainEventShape(file, root);

            assertThat(root.get("aggregate_type").asText())
                    .as(file.toString())
                    .isEqualTo("SHOP");

            assertThat(root.get("event_type").asText())
                    .as(file.toString())
                    .startsWith("shop.");
        }
    }

    @Test
    void sampleFixtures_shouldNotContainForbiddenSecretFields()
            throws Exception {
        for (Path file : jsonSampleFiles()) {
            JsonNode root = readJson(file);

            assertNoSecretFields(file, root);
        }
    }

    @Test
    void dlqSample_shouldMatchDlqEventShape()
            throws Exception {
        Path file = SAMPLE_DIR.resolve("auth-user-dlq-v1.json");

        JsonNode root = readJson(file);

        assertDomainEventShape(file, root);

        assertThat(root.has("payload"))
                .as(file.toString())
                .isTrue();

        JsonNode payload = root.get("payload");

        assertThat(payload.has("original_event_type"))
                .as(file.toString())
                .isTrue();
        assertThat(payload.has("original_aggregate_type"))
                .as(file.toString())
                .isTrue();
        assertThat(payload.has("original_aggregate_id"))
                .as(file.toString())
                .isTrue();
        assertThat(payload.has("original_partition_key"))
                .as(file.toString())
                .isTrue();
        assertThat(payload.has("original_payload_redacted"))
                .as(file.toString())
                .isTrue();
        assertThat(payload.get("original_payload_redacted").asBoolean())
                .as(file.toString())
                .isTrue();
        assertThat(payload.has("failure_code"))
                .as(file.toString())
                .isTrue();
        assertThat(payload.has("failure_message"))
                .as(file.toString())
                .isTrue();
        assertThat(payload.has("attempt_count"))
                .as(file.toString())
                .isTrue();

        assertThat(payload.has("original_payload"))
                .as(file.toString())
                .isFalse();
    }

    private void assertDomainEventShape(
            Path file,
            JsonNode root
    ) {
        assertThat(root.has("event_type"))
                .as(file.toString())
                .isTrue();
        assertThat(root.has("aggregate_type"))
                .as(file.toString())
                .isTrue();
        assertThat(root.has("aggregate_id"))
                .as(file.toString())
                .isTrue();
        assertThat(root.has("actor_user_id"))
                .as(file.toString())
                .isTrue();
        assertThat(root.has("payload"))
                .as(file.toString())
                .isTrue();

        assertThat(root.has("command_type"))
                .as(file.toString())
                .isFalse();
        assertThat(root.has("dedupe_key"))
                .as(file.toString())
                .isFalse();
        assertThat(root.has("template"))
                .as(file.toString())
                .isFalse();
        assertThat(root.has("partition_key"))
                .as(file.toString())
                .isFalse();
    }

    private void assertNoSecretFields(
            Path file,
            JsonNode node
    ) {
        if (node.isObject()) {
            node.properties().forEach(entry -> {
                assertThat(SECRET_FIELD_NAMES)
                        .as(file + " contains forbidden field " + entry.getKey())
                        .doesNotContain(entry.getKey());

                assertNoSecretFields(file, entry.getValue());
            });
        }

        if (node.isArray()) {
            node.forEach(child -> assertNoSecretFields(file, child));
        }
    }

    private JsonNode readJson(Path file) throws Exception {
        return objectMapper.readTree(
                Files.readString(file)
        );
    }

    private List<Path> jsonSampleFiles() throws Exception {
        try (var stream = Files.list(SAMPLE_DIR)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
    }

    private List<Path> jsonSampleFiles(String prefix) throws Exception {
        return jsonSampleFiles()
                .stream()
                .filter(path -> path.getFileName().toString().startsWith(prefix))
                .toList();
    }
}