# Kafka Contracts

This directory documents Kafka contracts published by `auth-user-service`.

## Topics

| Topic | Contract | Producer | Consumers |
|---|---|---|---|
| `notification.commands.v1` | [Notification Command v1](notification-command-v1.md) | `auth-user-service` | notification service |
| `user.events.v1` | [User Events v1](user-events-v1.md) | `auth-user-service` | downstream services |
| `shop.events.v1` | [Shop Events v1](shop-events-v1.md) | `auth-user-service` | downstream services |
| `auth-user.events.dlq.v1` | DLQ payload emitted by outbox publisher | `auth-user-service` | operators / diagnostics |

## Contract Files

| File | Purpose |
|---|---|
| [notification-command-v1.md](notification-command-v1.md) | Notification command contract |
| [user-events-v1.md](user-events-v1.md) | User domain event contract |
| [shop-events-v1.md](shop-events-v1.md) | Shop domain event contract |
| [schema/notification-command-v1.schema.json](schema/notification-command-v1.schema.json) | JSON Schema for notification command messages |
| [samples/README.md](samples/README.md) | Sample fixture index |

## Sample Fixtures

Sample Kafka message bodies are available under:

```text
docs/contracts/samples/
```

These samples represent Kafka record values only.

Kafka record keys and headers are documented in each contract file.

## Producer Rules

All Kafka messages are published through the transactional outbox publisher.

Producer guarantees:

- outbox events are persisted before publish
- Kafka publish happens asynchronously from business transaction
- event id is stable and can be used for idempotency
- notification command messages do not contain raw secrets
- domain event messages include aggregate metadata and `actor_user_id`

## Consumer Rules

Consumers must be idempotent.

Recommended idempotency keys:

| Topic | Idempotency Key |
|---|---|
| `notification.commands.v1` | `dedupe_key` |
| `user.events.v1` | `event_id` |
| `shop.events.v1` | `event_id` |
| `auth-user.events.dlq.v1` | `event_id` |

Consumers must ignore unknown optional fields.

Consumers must not depend on object field order.

## Security Rules

Kafka message bodies must not contain raw secret fields.

Forbidden field names:

```text
verification_token
reset_token
otp
raw_otp
password
password_hash
```

Notification messages may contain safe URLs such as:

```text
verification_url
reset_url
```

## Contract Tests

Contract coverage is protected by tests:

| Test | Purpose |
|---|---|
| `NotificationCommandEnvelopeSchemaContractTest` | Verifies notification command body shape |
| `OutboxMessageEnvelopeSchemaContractTest` | Verifies user/shop domain event body shape |
| `OutboxTopicResolverContractCoverageTest` | Verifies documented event types route to expected topics |
| `ContractSampleFixtureTest` | Verifies sample fixture JSON shape and no forbidden secret fields |

Run contract tests:

```bash
mvn -Dtest=NotificationCommandEnvelopeSchemaContractTest,OutboxMessageEnvelopeSchemaContractTest,OutboxTopicResolverContractCoverageTest,ContractSampleFixtureTest test
```

## Local Verification

Local Kafka and outbox smoke tests are documented in the project root README.

Useful scripts:

| Script | Purpose |
|---|---|
| `docker/kafka/create-topics.sh` | Create local Kafka topics |
| `docker/kafka/smoke-test.sh` | Verify Kafka produce/consume |
| `docker/kafka/outbox-e2e-smoke-test.sh` | Verify signup outbox publish to Kafka |