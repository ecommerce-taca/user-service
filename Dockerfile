# syntax=docker/dockerfile:1.7

# =========================
# Build stage
# =========================
# Keep Java 25 intentionally: the project is compiled with <java.version>25</java.version>.
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /workspace

# Copy Maven metadata first so dependency downloads are cached across source changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp dependency:go-offline

COPY src/ src/

# Tests should run in CI before image build. The image build only packages the app.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp clean package -DskipTests


# =========================
# Runtime stage
# =========================
# Java 25 is kept here as well so runtime and build JVMs match.
FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

# Create an unprivileged runtime user and a writable tmp directory for the JVM/Spring.
RUN addgroup -S spring \
    && adduser -S spring -G spring \
    && mkdir -p /tmp \
    && chown spring:spring /tmp

COPY --from=builder --chown=spring:spring \
    /workspace/target/auth-user-service-0.0.1-SNAPSHOT.jar \
    /app/app.jar

USER spring:spring

EXPOSE 8081

# Prefer JAVA_TOOL_OPTIONS for JVM flags; it is honored by java without a shell wrapper.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Check readiness instead of aggregate health because aggregate health also includes
# operational indicators such as outbox lag/failed events.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -q -O /dev/null "http://127.0.0.1:${SERVER_PORT:-8081}/actuator/health/readiness" || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
