#!/bin/sh
set -eu

BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-kafka:9092}"

echo "Waiting for Kafka at ${BOOTSTRAP_SERVER}..."

until /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --list >/dev/null 2>&1
do
  echo "Kafka is not ready yet..."
  sleep 2
done

create_topic() {
  topic_name="$1"
  partitions="$2"

  echo "Creating Kafka topic ${topic_name} with ${partitions} partition(s)..."

  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "${BOOTSTRAP_SERVER}" \
    --create \
    --if-not-exists \
    --topic "${topic_name}" \
    --partitions "${partitions}" \
    --replication-factor 1
}

create_topic "notification.commands.v1" 3
create_topic "user.events.v1" 3
create_topic "shop.events.v1" 3
create_topic "auth-user.events.dlq.v1" 1

echo "Kafka topic bootstrap completed."

/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --list