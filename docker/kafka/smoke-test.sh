#!/bin/sh
set -eu

BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-kafka:9092}"
TOPIC="${KAFKA_SMOKE_TEST_TOPIC:-notification.commands.v1}"
GROUP_ID="auth-user-smoke-test-$(date +%s)"
MESSAGE_KEY="smoke-test-key"
MESSAGE_VALUE='{"event_id":"00000000-0000-0000-0000-000000000000","schema_version":1,"command_type":"SMOKE_TEST","occurred_at":"2026-01-01T00:00:00Z","dedupe_key":"smoke-test","user_id":"00000000-0000-0000-0000-000000000000","channel":"EMAIL","recipient":"smoke@example.com","template":"smoke-test","data":{"source":"docker-smoke-test"}}'

required_topics="notification.commands.v1 user.events.v1 shop.events.v1 auth-user.events.dlq.v1"

echo "Checking Kafka at ${BOOTSTRAP_SERVER}..."

topics="$(
  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "${BOOTSTRAP_SERVER}" \
    --list
)"

for topic in ${required_topics}
do
  echo "${topics}" | grep -x "${topic}" >/dev/null 2>&1 || {
    echo "Missing required topic: ${topic}"
    exit 1
  }
done

echo "All required topics exist."

echo "Producing smoke test message to ${TOPIC}..."

printf '%s:%s\n' "${MESSAGE_KEY}" "${MESSAGE_VALUE}" | \
  /opt/kafka/bin/kafka-console-producer.sh \
    --bootstrap-server "${BOOTSTRAP_SERVER}" \
    --topic "${TOPIC}" \
    --property parse.key=true \
    --property key.separator=:

echo "Consuming smoke test message from ${TOPIC}..."

consumed="$(
  timeout 15 /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server "${BOOTSTRAP_SERVER}" \
    --topic "${TOPIC}" \
    --group "${GROUP_ID}" \
    --from-beginning \
    --timeout-ms 10000 \
    --property print.key=true \
    --property key.separator=: \
    2>/dev/null || true
)"

echo "${consumed}" | grep "${MESSAGE_KEY}" >/dev/null 2>&1 || {
  echo "Smoke test message was not consumed."
  echo "Consumed output:"
  echo "${consumed}"
  exit 1
}

echo "Kafka smoke test completed successfully."