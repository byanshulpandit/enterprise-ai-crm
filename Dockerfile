# ==============================================================================
# Enterprise AI-CRM Platform - Production Multi-Stage Dockerfile
# Base Image: Eclipse Temurin Java 21 LTS
# ==============================================================================

# Stage 1: Build & Package
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy Maven POM and pre-fetch dependencies for layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production jar (skipping tests for container image build)
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal Production Runtime
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="engineering@crm.internal"
LABEL description="Enterprise AI-CRM Platform Backend"

# Create dedicated non-root user and group
RUN addgroup -g 10001 crmgroup && \
    adduser -u 10001 -G crmgroup -s /bin/sh -D crmapp

WORKDIR /app

# Copy artifact from build stage
COPY --from=builder /build/target/crm-platform-*.jar app.jar

# Ensure appropriate ownership and permissions
RUN chown -R crmapp:crmgroup /app

# Switch to non-root user
USER crmapp:crmgroup

EXPOSE 8080

# Production JVM ergonomics optimized for container cgroups
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
