# Notification Command Contract v1

## Topic

```text
notification.commands.v1
```

## JSON Schema

Canonical schema:

```text
docs/contracts/schema/notification-command-v1.schema.json
```

## Purpose

`auth-user-service` publishes notification commands to this topic through the transactional outbox publisher.

The notification service consumes this topic and sends user-facing notifications such as:

- email verification
- password reset
- phone OTP

## Kafka Key

The Kafka record key must be:

```text
user_id
```

or the same partition key used by the source outbox event.

For auth notification commands, the key is the user id string.

## Headers

| Header | Required | Description |
|---|---:|---|
| `event_id` | Yes | Outbox event id |
| `command_type` | Yes | Notification command type |
| `schema_version` | Yes | Message schema version |

Example:

```text
event_id=01a0c3a5-e3e0-70ce-b5a1-ded9f0588018
command_type=AUTH_VERIFICATION_REQUESTED
schema_version=1
```

## Message Body

All notification command messages use this top-level shape:

```json
{
  "event_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0588018",
  "schema_version": 1,
  "command_type": "AUTH_VERIFICATION_REQUESTED",
  "occurred_at": "2026-09-21T10:00:00Z",
  "dedupe_key": "email-verification:01a0c3a5-e3e0-70ce-b5a1-ded9f0588018:token-id",
  "user_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0588018",
  "channel": "EMAIL",
  "recipient": "user@example.com",
  "template": "auth-email-verification-v1",
  "data": {}
}
```

## Required Fields

| Field | Type | Required | Description |
|---|---|---:|---|
| `event_id` | string UUID | Yes | Unique command event id |
| `schema_version` | number | Yes | Contract version |
| `command_type` | string | Yes | Command type |
| `occurred_at` | string ISO-8601 | Yes | Source event timestamp |
| `dedupe_key` | string | Yes | Idempotency key |
| `user_id` | string UUID | Yes | Target user id |
| `channel` | string | Yes | Delivery channel |
| `recipient` | string | Yes | Email address or phone number |
| `template` | string | Yes | Notification template key |
| `data` | object | Yes | Template variables |

## Supported Command Types

| Command Type | Channel | Template |
|---|---|---|
| `AUTH_VERIFICATION_REQUESTED` | `EMAIL` | `auth-email-verification-v1` |
| `PASSWORD_RESET_REQUESTED` | `EMAIL` | `auth-password-reset-v1` |
| `PHONE_OTP_REQUESTED` | `SMS` | `auth-phone-otp-v1` |

## Idempotency

Consumers must use `dedupe_key` to avoid duplicate sends.

Recommended behavior:

1. Receive Kafka message.
2. Check if `dedupe_key` has already been processed.
3. If processed, commit Kafka offset and skip send.
4. If not processed, send notification.
5. Store `dedupe_key` as processed.
6. Commit Kafka offset.

The consumer must tolerate duplicate Kafka deliveries.

## Security Rules

Notification command payloads must not contain raw secrets.

Forbidden fields:

```text
verification_token
reset_token
otp
raw_otp
password
```

Allowed examples:

```text
verification_url
reset_url
expires_in_minutes
challenge_id
```

## AUTH_VERIFICATION_REQUESTED Example

```json
{
  "event_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0588018",
  "schema_version": 1,
  "command_type": "AUTH_VERIFICATION_REQUESTED",
  "occurred_at": "2026-09-21T10:00:00Z",
  "dedupe_key": "email-verification:01a0c3a5-e3e0-70ce-b5a1-ded9f0588018:token-id",
  "user_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0588018",
  "channel": "EMAIL",
  "recipient": "user@example.com",
  "template": "auth-email-verification-v1",
  "data": {
    "display_name": "Smoke Test User",
    "verification_url": "https://taca.vn/verify?t=opaque-token",
    "expires_in_minutes": 30
  }
}
```

## PASSWORD_RESET_REQUESTED Example

```json
{
  "event_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0588019",
  "schema_version": 1,
  "command_type": "PASSWORD_RESET_REQUESTED",
  "occurred_at": "2026-09-21T10:05:00Z",
  "dedupe_key": "password-reset:01a0c3a5-e3e0-70ce-b5a1-ded9f0588018:token-id",
  "user_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0588018",
  "channel": "EMAIL",
  "recipient": "user@example.com",
  "template": "auth-password-reset-v1",
  "data": {
    "display_name": "Smoke Test User",
    "reset_url": "https://taca.vn/reset-password?token=opaque-token",
    "expires_in_minutes": 30
  }
}
```

## PHONE_OTP_REQUESTED Example

```json
{
  "event_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0588020",
  "schema_version": 1,
  "command_type": "PHONE_OTP_REQUESTED",
  "occurred_at": "2026-09-21T10:10:00Z",
  "dedupe_key": "phone-otp:challenge-id",
  "user_id": "01a0c3a5-e3e0-70ce-b5a1-ded9f0588018",
  "channel": "SMS",
  "recipient": "+84901234567",
  "template": "auth-phone-otp-v1",
  "data": {
    "challenge_id": "challenge-id",
    "expires_in_minutes": 5
  }
}
```
## Sample Fixtures

Sample Kafka message bodies are available under:

```text
docs/contracts/samples/
```

Notification command samples:

| File | Command Type |
|---|---|
| `notification-auth-verification-requested-v1.json` | `AUTH_VERIFICATION_REQUESTED` |
| `notification-password-reset-requested-v1.json` | `PASSWORD_RESET_REQUESTED` |
| `notification-phone-otp-requested-v1.json` | `PHONE_OTP_REQUESTED` |

These files represent Kafka record values only. Kafka headers and record keys are documented in this contract.

## Consumer Error Handling

The notification consumer should retry transient errors.

Examples of transient errors:

- SMTP provider timeout
- SMS provider timeout
- HTTP 429 from provider
- HTTP 5xx from provider

Examples of permanent errors:

- invalid recipient format
- unsupported template
- unsupported command type
- missing required field

Permanent failures should be sent to the notification service DLQ.

## Compatibility Rules

Producers may add optional fields to `data`.

Consumers must ignore unknown fields.

Producers must not remove or rename required top-level fields without creating a new schema version.
