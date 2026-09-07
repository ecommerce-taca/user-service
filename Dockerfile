# =========================
# Build stage
# =========================
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline -B

COPY src/ src/

RUN ./mvnw clean package -DskipTests -B


# =========================
# Runtime stage
# =========================
FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S spring \
    && adduser -S spring -G spring

COPY --from=builder \
    /workspace/target/auth-user-service-0.0.1-SNAPSHOT.jar \
    /app/app.jar

RUN chown spring:spring /app/app.jar

USER spring:spring

EXPOSE 8081

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]