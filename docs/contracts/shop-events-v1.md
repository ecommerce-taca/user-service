# Shop Events Contract v1

## Topic

```text
shop.events.v1
```

## Purpose

`auth-user-service` publishes shop domain events to this topic through the transactional outbox publisher.

Other services may consume this topic to update seller profiles, shop read models, search indexes, catalog ownership, compliance projections, or downstream workflows.

## Kafka Key

The Kafka record key is the shop id string for shop events. The key is not duplicated in the message body.

## Headers

| Header | Required | Description |
|---|---:|---|
| `event_id` | Yes | Outbox event id |
| `event_type` | Yes | Domain event type |
| `schema_version` | Yes | Message schema version |
| `aggregate_type` | Yes | Aggregate type, usually `SHOP` |
| `aggregate_id` | Yes | Shop id |

## Message Body

All shop events use this top-level shape:

```json
{
  "event_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0589018",
  "event_type": "shop.created",
  "schema_version": 1,
  "aggregate_type": "SHOP",
  "aggregate_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0589018",
  "actor_user_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0589000",
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
| `shop.created` | Shop was created |
| `shop.updated` | Shop profile was updated |
| `shop.status_changed` | Shop status changed |
| `shop.kyc.submitted` | Shop KYC was submitted |
| `shop.kyc.approved` | Shop KYC was approved |
| `shop.kyc.rejected` | Shop KYC was rejected |
| `shop.kyc.needs_info` | Shop KYC requires more information |
| `shop.kyc.expired` | Shop KYC expired |

## Idempotency

Consumers must use `event_id` for idempotency.

Consumers should tolerate duplicate deliveries.

## Compatibility Rules

Producers may add optional fields to `payload`.

Consumers must ignore unknown payload fields.

Required top-level fields must not be removed without a new topic or schema version.

## `shop.created`

### Payload

| Field | Type | Required |
|---|---|---:|
| `shop_id` | string UUID | Yes |
| `owner_user_id` | string UUID | Yes |
| `name` | string | Yes |
| `slug` | string | Yes |
| `status` | string | Yes |
| `created_at` | string ISO-8601 | Yes |

## `shop.updated`

### Payload

| Field | Type | Required |
|---|---|---:|
| `shop_id` | string UUID | Yes |
| `changed_fields` | array string | Yes |
| `snapshot` | object | Yes |
| `updated_at` | string ISO-8601 | Yes |
| `version` | number | Yes |

### Example

```json
{
  "event_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0589019",
  "event_type": "shop.updated",
  "schema_version": 1,
  "aggregate_type": "SHOP",
  "aggregate_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0589018",
  "actor_user_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0589000",
  "occurred_at": "2026-09-21T10:10:00Z",
  "payload": {
    "shop_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0589018",
    "changed_fields": [
      "name",
      "description"
    ],
    "snapshot": {
      "name": "Updated Shop",
      "description": "Updated shop description"
    },
    "updated_at": "2026-09-21T10:10:00Z",
    "version": 2
  }
}
```

## `shop.status_changed`

### Payload

| Field | Type | Required |
|---|---|---:|
| `shop_id` | string UUID | Yes |
| `old_status` | string | Yes |
| `new_status` | string | Yes |
| `reason` | string | No |
| `changed_at` | string ISO-8601 | Yes |

### Example

```json
{
  "event_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0589020",
  "event_type": "shop.status_changed",
  "schema_version": 1,
  "aggregate_type": "SHOP",
  "aggregate_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0589018",
  "actor_user_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0589001",
  "occurred_at": "2026-09-21T10:15:00Z",
  "payload": {
    "shop_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0589018",
    "old_status": "DRAFT",
    "new_status": "ACTIVE",
    "reason": "KYC_APPROVED",
    "changed_at": "2026-09-21T10:15:00Z"
  }
}
```

## `shop.kyc.submitted`

### Payload

| Field | Type | Required |
|---|---|---:|
| `shop_id` | string UUID | Yes |
| `kyc_case_id` | string UUID | Yes |
| `document_types` | array string | Yes |
| `submitted_at` | string ISO-8601 | Yes |
| `expires_at` | string ISO-8601 | Yes |

## `shop.kyc.approved`

### Payload

| Field | Type | Required |
|---|---|---:|
| `shop_id` | string UUID | Yes |
| `kyc_case_id` | string UUID | Yes |
| `reviewed_by` | string UUID | Yes |
| `reviewed_at` | string ISO-8601 | Yes |

## `shop.kyc.rejected`

### Payload

| Field | Type | Required |
|---|---|---:|
| `shop_id` | string UUID | Yes |
| `kyc_case_id` | string UUID | Yes |
| `reviewed_by` | string UUID | Yes |
| `reviewed_at` | string ISO-8601 | Yes |
| `reason` | string | Yes |

## `shop.kyc.needs_info`

### Payload

| Field | Type | Required |
|---|---|---:|
| `shop_id` | string UUID | Yes |
| `kyc_case_id` | string UUID | Yes |
| `reviewed_by` | string UUID | Yes |
| `reviewed_at` | string ISO-8601 | Yes |
| `reason` | string | Yes |
| `required_actions` | array string | No |

## `shop.kyc.expired`

### Payload

| Field | Type | Required |
|---|---|---:|
| `shop_id` | string UUID | Yes |
| `kyc_case_id` | string UUID | Yes |
| `expired_at` | string ISO-8601 | Yes |
| `expired_documents` | array string | Yes |

### Example

```json
{
  "event_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0589021",
  "event_type": "shop.kyc.expired",
  "schema_version": 1,
  "aggregate_type": "SHOP",
  "aggregate_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0589018",
  "actor_user_id": null,
  "occurred_at": "2026-09-21T10:20:00Z",
  "payload": {
    "shop_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0589018",
    "kyc_case_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0589022",
    "expired_at": "2026-09-21T10:20:00Z",
    "expired_documents": [
      "BUSINESS_LICENSE"
    ]
  }
}
```

## Sample Fixtures

Sample Kafka message bodies are available under:

```text
docs/contracts/samples/
```

Shop event samples:

| File | Event Type |
|---|---|
| `shop-updated-v1.json` | `shop.updated` |
| `shop-status-changed-v1.json` | `shop.status_changed` |
| `shop-kyc-expired-v1.json` | `shop.kyc.expired` |

These files represent Kafka record values only. Kafka headers and record keys are documented in this contract.