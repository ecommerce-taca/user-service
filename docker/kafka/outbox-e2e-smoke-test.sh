#!/bin/sh
set -eu

BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-kafka:9092}"
EMAIL="${EXPECTED_EMAIL:?Missing EXPECTED_EMAIL}"

NOTIFICATION_GROUP_ID="auth-user-outbox-smoke-notification-$(date +%s)"
USER_GROUP_ID="auth-user-outbox-smoke-user-$(date +%s)"

echo "Running outbox E2E Kafka verification..."
echo "Kafka bootstrap server: ${BOOTSTRAP_SERVER}"
echo "Expected email: ${EMAIL}"

echo "Waiting for outbox scheduler to publish..."
sleep 8

echo "Consuming notification command..."

notification_output="$(
  timeout 25 /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server "${BOOTSTRAP_SERVER}" \
    --topic notification.commands.v1 \
    --group "${NOTIFICATION_GROUP_ID}" \
    --from-beginning \
    --timeout-ms 20000 \
    --property print.key=true \
    --property key.separator=: \
    2>/dev/null || true
)"

echo "${notification_output}" | grep "${EMAIL}" >/dev/null 2>&1 || {
  echo "Did not find signup notification command for ${EMAIL}."
  echo "Consumed notification output:"
  echo "${notification_output}"
  exit 1
}

echo "${notification_output}" | grep "AUTH_VERIFICATION_REQUESTED" >/dev/null 2>&1 || {
  echo "Did not find AUTH_VERIFICATION_REQUESTED command."
  echo "Consumed notification output:"
  echo "${notification_output}"
  exit 1
}

echo "Notification command found."

echo "Consuming user event..."

user_output="$(
  timeout 25 /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server "${BOOTSTRAP_SERVER}" \
    --topic user.events.v1 \
    --group "${USER_GROUP_ID}" \
    --from-beginning \
    --timeout-ms 20000 \
    --property print.key=true \
    --property key.separator=: \
    2>/dev/null || true
)"

echo "${user_output}" | grep "${EMAIL}" >/dev/null 2>&1 || {
  echo "Did not find user event for ${EMAIL}."
  echo "Consumed user output:"
  echo "${user_output}"
  exit 1
}

echo "${user_output}" | grep "user.created" >/dev/null 2>&1 || {
  echo "Did not find user.created event."
  echo "Consumed user output:"
  echo "${user_output}"
  exit 1
}

echo "User event found."

echo "Outbox E2E smoke test completed successfully."