# ==============================================================================
# Multi-Stage Production Dockerfile for Spring Boot 3.3 (Java 21)
# ==============================================================================

# Stage 1: Build & Package
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Cache Maven dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy application sources and build thin executable jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal Distroless / Hardened Alpine Runtime
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Run as non-privileged security user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder --chown=appuser:appgroup /build/target/es-job-search-service-1.0.0-SNAPSHOT.jar app.jar

USER appuser

EXPOSE 8090

# Production JVM ergonomics: ZGC with Generational Mode, fast startup, OOM heap dump
ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -Xms256m -Xmx512m -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=20s --timeout=4s --retries=5 --start-period=25s \
  CMD wget -q -O - http://localhost:8090/v3/api-docs > /dev/null || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
