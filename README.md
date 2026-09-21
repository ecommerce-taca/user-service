# Auth User Service

## Local Kafka Setup

Kafka local được chạy bằng Docker Compose và tắt auto-create topic. Các topic bắt buộc được tạo bởi `kafka-init`.

### Start Local Stack

```bash
docker compose up -d
```

Kiểm tra services:

```bash
docker compose ps
```

Các service chính cần ở trạng thái:

```text
kafka        healthy
mysql        healthy
redis        healthy
user-service Up
```

`kafka-init` chạy xong sẽ thoát. Kiểm tra bằng:

```bash
docker compose ps -a
docker compose logs kafka-init
```

Log thành công cần có:

```text
Kafka topic bootstrap completed.
```

### Verify Kafka Topics

```bash
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list
```

Cần có đủ các topic:

```text
auth-user.events.dlq.v1
notification.commands.v1
shop.events.v1
user.events.v1
```

## Local Kafka Smoke Test

Smoke test này kiểm tra Kafka local có thể produce và consume message.

PowerShell:

```powershell
docker compose run --rm -e KAFKA_BOOTSTRAP_SERVER=kafka:9092 -v ./docker/kafka/smoke-test.sh:/opt/taca/kafka/smoke-test.sh:ro kafka /bin/sh /opt/taca/kafka/smoke-test.sh
```

Kết quả thành công:

```text
Kafka smoke test completed successfully.
```

## Local Outbox E2E Smoke Test

Smoke test này kiểm tra pipeline thật:

```text
signup API -> outbox_events -> outbox scheduler -> Kafka -> consumer verify
```

Test sẽ xác nhận:

- `AUTH_VERIFICATION_REQUESTED` được publish vào `notification.commands.v1`
- `user.created` được publish vào `user.events.v1`

### 1. Start Local Stack

```bash
docker compose up -d
```

Đảm bảo app đã start:

```powershell
docker compose logs user-service --tail=1000 | Select-String -Pattern "Started AuthUser|Started .*Application|Tomcat started|APPLICATION FAILED|ERROR|Exception"
```

Cần thấy:

```text
Started AuthUserServiceApplication
```

### 2. Call Signup API

PowerShell:

```powershell
$email = "smoke-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())@example.com"; $body = @{ email = $email; password = "Password@123456"; full_name = "Smoke Test User" } | ConvertTo-Json; try { $response = Invoke-WebRequest -Method Post -Uri "http://localhost:8081/api/v1/auth/signup" -Headers @{ "X-Request-Id" = "smoke-test-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())" } -ContentType "application/json" -Body $body; Write-Host "STATUS=$($response.StatusCode)"; Write-Host "EMAIL=$email" } catch { $_.Exception.Response.StatusCode.value__; $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream()); $reader.ReadToEnd(); Write-Host "EMAIL=$email" }
```

Kết quả thành công cần có:

```text
STATUS=200
```

hoặc:

```text
STATUS=201
```

Copy email được in ra, ví dụ:

```text
EMAIL=smoke-1789989045@example.com
```

### 3. Verify Kafka Messages

Thay `EXPECTED_EMAIL` bằng email vừa tạo:

```powershell
docker compose run --rm -e KAFKA_BOOTSTRAP_SERVER=kafka:9092 -e EXPECTED_EMAIL=smoke-1789989045@example.com -v ./docker/kafka/outbox-e2e-smoke-test.sh:/opt/taca/kafka/outbox-e2e-smoke-test.sh:ro kafka /bin/sh /opt/taca/kafka/outbox-e2e-smoke-test.sh
```

Kết quả thành công:

```text
Notification command found.
User event found.
Outbox E2E smoke test completed successfully.
```

## Troubleshooting

### User service không start

Kiểm tra log:

```powershell
docker compose logs user-service --tail=300 | Select-String -Pattern "Started AuthUser|ERROR|Exception|APPLICATION FAILED"
```

### Signup trả 400

In response body:

```powershell
$email = "smoke-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())@example.com"; $body = @{ email = $email; password = "Password@123456"; full_name = "Smoke Test User" } | ConvertTo-Json; try { $response = Invoke-WebRequest -Method Post -Uri "http://localhost:8081/api/v1/auth/signup" -Headers @{ "X-Request-Id" = "smoke-test-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())" } -ContentType "application/json" -Body $body; $response.StatusCode; $response.Content; $email } catch { $_.Exception.Response.StatusCode.value__; $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream()); $reader.ReadToEnd(); $email }
```

### Không thấy Kafka message

Kiểm tra outbox DB:

```bash
docker compose exec mysql mysql -uecommerce -p userdb
```

SQL:

```sql
select event_type, attempt_count, published_at, failed_at, last_error_code, created_at
from outbox_events
order by created_at desc
limit 10;
```

Kiểm tra publisher log:

```powershell
docker compose logs user-service --tail=300 | Select-String -Pattern "Processed|Published outbox|Kafka publish failed|Outbox"
```

### Kiểm tra DLQ

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic auth-user.events.dlq.v1 --from-beginning --property print.key=true --property key.separator=:
```

Dừng bằng `Ctrl+C`.

## Notes

- `docker/kafka/create-topics.sh` bootstrap Kafka topics local.
- `docker/kafka/smoke-test.sh` kiểm tra Kafka produce/consume.
- `docker/kafka/outbox-e2e-smoke-test.sh` kiểm tra Kafka messages sau khi signup API tạo outbox.
- Các file `.sh` cần dùng line ending `LF`, không dùng `CRLF`.