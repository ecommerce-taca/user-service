# Auth User DLQ Contract v1

## Topic

```text
auth-user.events.dlq.v1
```

## Purpose

`auth-user-service` publishes messages to this topic when an outbox event cannot be published to its target Kafka topic after the configured max retry count.

This topic is intended for operators and diagnostics.

Consumers should not use this topic for normal business workflows.

## Kafka Key

The Kafka record key is the original outbox event partition key.

## Headers

DLQ messages use the same domain event envelope headers as regular domain events.

| Header | Required | Description |
|---|---:|---|
| `event_id` | Yes | Original outbox event id |
| `event_type` | Yes | Original event type |
| `schema_version` | Yes | Original schema version |
| `aggregate_type` | Yes | Original aggregate type |
| `aggregate_id` | Yes | Original aggregate id |

## Message Body

DLQ messages use the generic domain event envelope shape:

```json
{
  "event_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0589018",
  "event_type": "user.created",
  "schema_version": 1,
  "occurred_at": "2026-09-21T10:00:00Z",
  "aggregate_type": "USER",
  "aggregate_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0589018",
  "actor_user_id": null,
  "payload": {
    "original_event_type": "user.created",
    "original_aggregate_type": "USER",
    "original_aggregate_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0589018",
    "original_partition_key": "01a0c3a5-e3e0-70ce-b5a1-ded9f0589018",
    "original_payload_redacted": true,
    "failure_code": "KAFKA_PUBLISH_FAILED",
    "failure_message": "IllegalStateException",
    "attempt_count": 3
  }
}
```

## Payload Fields

| Field | Type | Required | Description |
|---|---|---:|---|
| `original_event_type` | string | Yes | Original event type |
| `original_aggregate_type` | string | Yes | Original aggregate type |
| `original_aggregate_id` | string UUID | Yes | Original aggregate id |
| `original_partition_key` | string | Yes | Original Kafka key |
| `original_payload_redacted` | boolean | Yes | Always `true`; original payload is not copied to DLQ |
| `failure_code` | string | Yes | Failure code |
| `failure_message` | string | Yes | Sanitized exception class name |
| `attempt_count` | number | Yes | Number of publish attempts |

## Failure Codes

| Code | Description |
|---|---|
| `KAFKA_PUBLISH_FAILED` | Outbox publisher could not publish event to Kafka |

## Security Rules

DLQ messages must not include original raw payloads.

DLQ messages must not contain raw secret fields.

Forbidden field names:

```text
original_payload
verification_token
reset_token
otp
raw_otp
password
password_hash
```

`failure_message` must be sanitized. It should contain an exception class name, not raw provider response, token, OTP, or payload text.

## Operator Workflow

Recommended manual workflow:

1. Consume DLQ message.
2. Inspect `original_event_type`, `original_aggregate_type`, and `original_aggregate_id`.
3. Inspect app logs around the event id.
4. Fix root cause.
5. Decide whether the source event should be replayed manually.
6. Do not replay directly from DLQ without validating payload source and idempotency.

## Compatibility Rules

Producers may add optional fields to DLQ payload.

Consumers must ignore unknown fields.

Required payload fields must not be removed without a new schema version or topic.