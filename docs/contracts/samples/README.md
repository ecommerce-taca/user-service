# Contract Sample Fixtures

This folder contains sample Kafka message bodies for contract consumers.

## Notification Commands

| File | Topic |
|---|---|
| `notification-auth-verification-requested-v1.json` | `notification.commands.v1` |
| `notification-password-reset-requested-v1.json` | `notification.commands.v1` |
| `notification-phone-otp-requested-v1.json` | `notification.commands.v1` |

## User Events

| File | Topic |
|---|---|
| `user-created-v1.json` | `user.events.v1` |
| `user-email-verified-v1.json` | `user.events.v1` |
| `user-updated-v1.json` | `user.events.v1` |

## Shop Events

| File | Topic |
|---|---|
| `shop-updated-v1.json` | `shop.events.v1` |
| `shop-status-changed-v1.json` | `shop.events.v1` |
| `shop-kyc-expired-v1.json` | `shop.events.v1` |

## DLQ

| File | Topic |
|---|---|
| `auth-user-dlq-v1.json` | `auth-user.events.dlq.v1` |

## Notes

These files represent Kafka record values only.

Kafka headers and record keys are documented in the contract markdown files.