# Lightning CLI Dockerfile
# Multi-stage build for minimal image size

# Build stage
FROM golang:1.22-alpine AS builder

WORKDIR /build

# Install git for go mod (some deps may need it)
RUN apk add --no-cache git

# Copy go mod files first for better caching
COPY go.mod go.sum ./
RUN go mod download

# Copy source code
COPY . .

# Build the CLI binary
# CGO_ENABLED=0 for static binary, ldflags for smaller binary
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-w -s" -o lightning ./cmd/lightning/

# Runtime stage - use scratch for minimal image
FROM alpine:3.19

# Install ca-certificates for HTTPS
RUN apk add --no-cache ca-certificates

WORKDIR /app

# Copy the binary from builder
COPY --from=builder /build/lightning /usr/local/bin/lightning

# Create non-root user
RUN addgroup -S lightning && adduser -S lightning -G lightning
USER lightning

# Set default environment variables
ENV LIGHTNING_CONTROL_PLANE_URL=http://host.docker.internal:8081
ENV LIGHTNING_OUTPUT_FORMAT=table

ENTRYPOINT ["lightning"]
CMD ["--help"]
