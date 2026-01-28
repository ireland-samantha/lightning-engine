# ======================
# Stage 1: Build Frontend
# ======================
FROM node:22-alpine AS frontend-build

WORKDIR /app/frontend
COPY lightning-engine/webservice/quarkus-web-api/src/main/frontend/package*.json ./
RUN npm ci
COPY lightning-engine/webservice/quarkus-web-api/src/main/frontend/ ./
# Vite outputs to ../resources/META-INF/resources/admin/dashboard relative to frontend dir
RUN npm run build

# ======================
# Stage 2: Build Backend
# ======================
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app
COPY . .

# Copy built frontend to where Quarkus expects it (vite outputs to /app/resources/... in frontend stage)
COPY --from=frontend-build /app/resources/META-INF/resources/admin/dashboard/ lightning-engine/webservice/quarkus-web-api/src/main/resources/META-INF/resources/admin/dashboard/

# Generate JWT RSA key pairs (same as gen_secrets.py but without Python dependency)
# These are required for SmallRye JWT signing/verification
RUN mkdir -p lightning-engine/webservice/quarkus-web-api/src/main/resources \
             lightning-engine/webservice/quarkus-web-api/src/test/resources && \
    openssl genpkey -algorithm RSA -out lightning-engine/webservice/quarkus-web-api/src/main/resources/privateKey.pem -pkeyopt rsa_keygen_bits:2048 && \
    openssl rsa -pubout -in lightning-engine/webservice/quarkus-web-api/src/main/resources/privateKey.pem -out lightning-engine/webservice/quarkus-web-api/src/main/resources/publicKey.pem && \
    cp lightning-engine/webservice/quarkus-web-api/src/main/resources/privateKey.pem lightning-engine/webservice/quarkus-web-api/src/test/resources/ && \
    cp lightning-engine/webservice/quarkus-web-api/src/main/resources/publicKey.pem lightning-engine/webservice/quarkus-web-api/src/test/resources/

RUN mvn -B clean install

# ======================
# Stage 2: Runtime
# ======================
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Install wget for healthcheck
RUN apk add --no-cache wget

# Create non-root user
RUN addgroup -S quarkus && adduser -S quarkus -G quarkus

# Copy Quarkus fast-jar
COPY --from=build /app/lightning-engine/webservice/quarkus-web-api/target/quarkus-app/lib/ ./lib/
COPY --from=build /app/lightning-engine/webservice/quarkus-web-api/target/quarkus-app/*.jar ./
COPY --from=build /app/lightning-engine/webservice/quarkus-web-api/target/quarkus-app/app/ ./app/
COPY --from=build /app/lightning-engine/webservice/quarkus-web-api/target/quarkus-app/quarkus/ ./quarkus/

# Copy extension modules
RUN mkdir -p /app/modules
COPY --from=build /app/lightning-engine-extensions/modules/entity-module/target/*.jar ./modules/
COPY --from=build /app/lightning-engine-extensions/modules/rigid-body-module/target/*.jar ./modules/
COPY --from=build /app/lightning-engine-extensions/modules/grid-map-module/target/*.jar ./modules/
COPY --from=build /app/lightning-engine-extensions/modules/rendering-module/target/*.jar ./modules/

RUN chown -R quarkus:quarkus /app
USER quarkus

EXPOSE 8080
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0"

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/containers || exit 1

ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
