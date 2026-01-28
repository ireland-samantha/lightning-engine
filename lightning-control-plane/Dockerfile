# Lightning Control Plane Dockerfile
# Multi-stage build for optimal image size

# Build stage
FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /build

# Copy pom files first for better caching
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn package -DskipTests -Dquarkus.package.jar.type=uber-jar

# Runtime stage
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S lightning && adduser -S lightning -G lightning

# Copy the built JAR
COPY --from=builder /build/target/*-runner.jar app.jar

# Set ownership
RUN chown -R lightning:lightning /app

USER lightning

# Expose control plane port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/q/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
