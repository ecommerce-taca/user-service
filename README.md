# Auth User Service

## Local Kafka Smoke Test

Kafka local được chạy bằng Docker Compose và tắt auto-create topic. Vì vậy các topic bắt buộc sẽ được tạo bởi `kafka-init`.

### Start Kafka

```bash
docker compose up -d kafka kafka-init
```

Kiểm tra topic đã được tạo:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list
```

Kết quả cần có:

```text
auth-user.events.dlq.v1
notification.commands.v1
shop.events.v1
user.events.v1
```

### Run Smoke Test

PowerShell:

```powershell
docker compose run --rm -e KAFKA_BOOTSTRAP_SERVER=kafka:9092 -v ./docker/kafka/smoke-test.sh:/opt/taca/kafka/smoke-test.sh:ro kafka /bin/sh /opt/taca/kafka/smoke-test.sh
```

Bash:

```bash
docker compose run --rm \
  -e KAFKA_BOOTSTRAP_SERVER=kafka:9092 \
  -v ./docker/kafka/smoke-test.sh:/opt/taca/kafka/smoke-test.sh:ro \
  kafka \
  /bin/sh /opt/taca/kafka/smoke-test.sh
```

Kết quả thành công:

```text
Kafka smoke test completed successfully.
```

### Notes

- `docker/kafka/create-topics.sh` dùng để bootstrap topic local.
- `docker/kafka/smoke-test.sh` dùng để kiểm tra produce/consume Kafka local.
- Hai file `.sh` cần dùng line ending `LF`, không dùng `CRLF`.
- Nếu topic guard đang bật, `user-service` chỉ nên start sau khi `kafka-init` chạy thành công.