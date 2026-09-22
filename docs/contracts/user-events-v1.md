# User Events Contract v1

## Topic

```text
user.events.v1
```

## Purpose

`auth-user-service` publishes user domain events to this topic through the transactional outbox publisher.

Other services may consume this topic to update read models, profiles, search indexes, audit projections, or downstream domain state.

## Kafka Key

The Kafka record key is the user id string for user events. The key is not duplicated in the message body.

## Headers

| Header | Required | Description |
|---|---:|---|
| `event_id` | Yes | Outbox event id |
| `event_type` | Yes | Domain event type |
| `schema_version` | Yes | Message schema version |
| `aggregate_type` | Yes | Aggregate type, usually `USER` |
| `aggregate_id` | Yes | User id |

## Message Body

All user events use this top-level shape:

```json
{
  "event_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0588018",
  "event_type": "user.created",
  "schema_version": 1,
  "aggregate_type": "USER",
  "aggregate_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0588018",
  "actor_user_id": null,
  "occurred_at": "2026-09-21T10:00:00Z",
  "payload": {}
}
```

## Required Top-Level Fields

| Field | Type | Required | Description |
|---|---|---:|---|
| `event_id` | string UUID | Yes | Unique event id |
| `event_type` | string | Yes | Domain event type |
| `schema_version` | number | Yes | Contract version |
| `aggregate_type` | string | Yes | Aggregate type |
| `aggregate_id` | string UUID | Yes | Aggregate id |
| `actor_user_id` | string UUID or null | Yes | User id that caused the event, or null for system events |
| `occurred_at` | string ISO-8601 | Yes | Event timestamp |
| `payload` | object | Yes | Event-specific payload |

## Supported Event Types

| Event Type | Description |
|---|---|
| `user.created` | User account was created |
| `user.email_verified` | User email was verified |
| `user.password_changed` | User password was changed |
| `user.role_changed` | User role assignment changed |
| `user.status_changed` | User status changed |
| `user.updated` | User profile was updated |

## Idempotency

Consumers must use `event_id` for idempotency.

Recommended behavior:

1. Receive event.
2. Check if `event_id` was already processed.
3. If processed, commit Kafka offset and skip.
4. If not processed, handle event.
5. Store `event_id` as processed.
6. Commit Kafka offset.

Consumers should tolerate duplicate deliveries.

## Compatibility Rules

Producers may add optional fields to `payload`.

Consumers must ignore unknown payload fields.

Required top-level fields must not be removed without a new topic or schema version.

## `user.created`

### Payload

| Field | Type | Required |
|---|---|---:|
| `user_id` | string UUID | Yes |
| `email` | string | Yes |
| `full_name` | string | Yes |
| `status` | string | Yes |
| `created_at` | string ISO-8601 | Yes |

### Example

```json
{
  "event_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0588018",
  "event_type": "user.created",
  "schema_version": 1,
  "aggregate_type": "USER",
  "aggregate_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0588018",
  "actor_user_id": null,
  "occurred_at": "2026-09-21T10:00:00Z",
  "payload": {
    "user_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0588018",
    "email": "user@example.com",
    "full_name": "Smoke Test User",
    "status": "PENDING_VERIFICATION",
    "created_at": "2026-09-21T10:00:00Z"
  }
}
```

## `user.email_verified`

### Payload

| Field | Type | Required |
|---|---|---:|
| `user_id` | string UUID | Yes |
| `verified_at` | string ISO-8601 | Yes |

### Example

```json
{
  "event_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0588019",
  "event_type": "user.email_verified",
  "schema_version": 1,
  "aggregate_type": "USER",
  "aggregate_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0588018",
  "actor_user_id": null,
  "occurred_at": "2026-09-21T10:05:00Z",
  "payload": {
    "user_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0588018",
    "verified_at": "2026-09-21T10:05:00Z"
  }
}
```

## `user.password_changed`

### Payload

| Field | Type | Required |
|---|---|---:|
| `user_id` | string UUID | Yes |
| `changed_at` | string ISO-8601 | Yes |

### Security

The payload must not include password, password hash, reset token, or raw secret.

## `user.role_changed`

### Payload

| Field | Type | Required |
|---|---|---:|
| `user_id` | string UUID | Yes |
| `role_code` | string | Yes |
| `action` | string | Yes |
| `changed_at` | string ISO-8601 | Yes |

## `user.status_changed`

### Payload

| Field | Type | Required |
|---|---|---:|
| `user_id` | string UUID | Yes |
| `old_status` | string | Yes |
| `new_status` | string | Yes |
| `reason` | string | No |
| `changed_at` | string ISO-8601 | Yes |

## `user.updated`

### Payload

| Field | Type | Required |
|---|---|---:|
| `user_id` | string UUID | Yes |
| `changed_fields` | array string | Yes |
| `snapshot` | object | Yes |
| `updated_at` | string ISO-8601 | Yes |

### Example

```json
{
  "event_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0588020",
  "event_type": "user.updated",
  "schema_version": 1,
  "aggregate_type": "USER",
  "aggregate_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0588018",
  "actor_user_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0588018",
  "occurred_at": "2026-09-21T10:10:00Z",
  "payload": {
    "user_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0588018",
    "changed_fields": [
      "full_name"
    ],
    "snapshot": {
      "full_name": "Updated Name"
    },
    "updated_at": "2026-09-21T10:10:00Z"
  }
}
```

## Sample Fixtures

Sample Kafka message bodies are available under:

```text
docs/contracts/samples/
```

User event samples:

| File | Event Type |
|---|---|
| `user-created-v1.json` | `user.created` |
| `user-email-verified-v1.json` | `user.email_verified` |
| `user-updated-v1.json` | `user.updated` |

These files represent Kafka record values only. Kafka headers and record keys are documented in this contract.