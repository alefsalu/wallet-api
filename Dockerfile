# Stage 1: Build application JAR
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal hardened runtime image
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

RUN addgroup -g 10001 appgroup && \
    adduser -u 10001 -G appgroup -s /bin/sh -D appuser && \
    mkdir -p /app/keys && \
    chown -R appuser:appgroup /app

COPY --from=builder --chown=appuser:appgroup /workspace/target/wallet-api-*.jar app.jar

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --retries=3 \
    CMD wget -q -O - http://localhost:8080/health/live || exit 1

ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:+ZGenerational", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
