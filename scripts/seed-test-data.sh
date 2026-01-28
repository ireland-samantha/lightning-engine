#!/bin/bash
# Seed script for Lightning Engine local test instance
# Based on docs/api-reference.md and docs/docker.md
#
# Usage: ./seed-test-data.sh [config.json]
#        ./seed-test-data.sh seed-config-1000.json

set -e

# Configuration
API_URL="${API_URL:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin}"
CONFIG_FILE="${1:-$(dirname "$0")/seed-config.json}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log() { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# Check dependencies
for cmd in jq curl bc; do
    if ! command -v $cmd &> /dev/null; then
        error "$cmd is required but not installed. Install with: brew install $cmd"
    fi
done

# Check config file
if [ ! -f "$CONFIG_FILE" ]; then
    error "Config file not found: $CONFIG_FILE"
fi

log "Using config file: $CONFIG_FILE"

# Check if service is running
log "Checking if Lightning Engine is running at $API_URL..."
if ! curl -s --fail "$API_URL/api/health" > /dev/null 2>&1; then
    error "Lightning Engine is not running at $API_URL. Start with: docker compose up -d"
fi

# Step 1: Authenticate
log "Authenticating as $ADMIN_USER..."
LOGIN_RESPONSE=$(curl -s -X POST "$API_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"$ADMIN_USER\", \"password\": \"$ADMIN_PASS\"}")

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token')
if [ -z "$TOKEN" ] || [ "$TOKEN" == "null" ]; then
    error "Authentication failed: $(echo "$LOGIN_RESPONSE" | jq -r '.message // .error // "Unknown error"')"
fi
log "Authenticated successfully"

# Helper function for authenticated API calls
api() {
    local method=$1
    local endpoint=$2
    local data=$3

    if [ -n "$data" ]; then
        curl -s -X "$method" "$API_URL$endpoint" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "$data"
    else
        curl -s -X "$method" "$API_URL$endpoint" \
            -H "Authorization: Bearer $TOKEN"
    fi
}

# Helper to get random number in range
random_range() {
    local min=$1
    local max=$2
    echo "scale=2; $min + ($RANDOM / 32767) * ($max - $min)" | bc
}

# Helper to get value from position/velocity config
get_value() {
    local config=$1
    local index=$2

    local is_number=$(echo "$config" | jq 'type == "number"')
    if [ "$is_number" = "true" ]; then
        echo "$config" | jq -r '.'
        return
    fi

    local is_random=$(echo "$config" | jq -r '.random // false')
    if [ "$is_random" = "true" ]; then
        local min=$(echo "$config" | jq -r '.min // 0')
        local max=$(echo "$config" | jq -r '.max // 100')
        random_range "$min" "$max"
        return
    fi

    local start=$(echo "$config" | jq -r '.start // .value // 0')
    local increment=$(echo "$config" | jq -r '.increment // 0')
    echo "scale=2; $start + ($index * $increment)" | bc
}

# Read config
CONFIG=$(cat "$CONFIG_FILE")

# Get counts
NUM_CONTAINERS=$(echo "$CONFIG" | jq -r '.containers // 1')
NUM_MATCHES=$(echo "$CONFIG" | jq -r '.matchesPerContainer // 1')
CONTAINER_NAME_BASE=$(echo "$CONFIG" | jq -r '.container.name // "seed-container"')
MATCH_MODULES=$(echo "$CONFIG" | jq -c '.match.modules // ["EntityModule", "RigidBodyModule", "GridMapModule"]')
PLAYER_NAME=$(echo "$CONFIG" | jq -r '.player.name // "SeedPlayer"')

# Calculate totals
ENTITY_COUNT_PER_MATCH=$(echo "$CONFIG" | jq '[.entities[].count] | add // 0')
TOTAL_MATCHES=$((NUM_CONTAINERS * NUM_MATCHES))
TOTAL_ENTITIES=$((TOTAL_MATCHES * ENTITY_COUNT_PER_MATCH))

log "Plan: $NUM_CONTAINERS containers x $NUM_MATCHES matches x $ENTITY_COUNT_PER_MATCH entities = $TOTAL_ENTITIES total entities"

# Track timing
START_TIME=$(date +%s)

# Arrays to track created resources
declare -a CONTAINER_IDS

for ((c=0; c<NUM_CONTAINERS; c++)); do
    CONTAINER_NUM=$((c + 1))

    if [ "$NUM_CONTAINERS" -gt 1 ]; then
        CONTAINER_NAME="${CONTAINER_NAME_BASE}-${CONTAINER_NUM}-$(date +%s)"
    else
        CONTAINER_NAME="${CONTAINER_NAME_BASE}-$(date +%s)"
    fi

    # Create container
    log "[$CONTAINER_NUM/$NUM_CONTAINERS] Creating container: $CONTAINER_NAME"
    CONTAINER_RESPONSE=$(api POST "/api/containers" "{\"name\": \"$CONTAINER_NAME\"}")

    CONTAINER_ID=$(echo "$CONTAINER_RESPONSE" | jq -r '.id')
    if [ -z "$CONTAINER_ID" ] || [ "$CONTAINER_ID" == "null" ]; then
        error "Failed to create container: $(echo "$CONTAINER_RESPONSE" | jq '.')"
    fi
    CONTAINER_IDS+=("$CONTAINER_ID")

    # Start container and wait
    api POST "/api/containers/$CONTAINER_ID/start" > /dev/null
    for attempt in {1..30}; do
        STATUS=$(api GET "/api/containers/$CONTAINER_ID" | jq -r '.status')
        if [ "$STATUS" = "RUNNING" ]; then
            break
        fi
        if [ "$attempt" -eq 30 ]; then
            error "Container $CONTAINER_ID failed to start. Status: $STATUS"
        fi
        sleep 1
    done

    # Create player once per container
    PLAYER_RESPONSE=$(api POST "/api/containers/$CONTAINER_ID/players" "{\"name\": \"$PLAYER_NAME\"}")
    PLAYER_ID=$(echo "$PLAYER_RESPONSE" | jq -r '.id // 1')

    # Create matches
    for ((m=0; m<NUM_MATCHES; m++)); do
        MATCH_NUM=$((m + 1))

        # Create match
        MATCH_RESPONSE=$(api POST "/api/containers/$CONTAINER_ID/matches" \
            "{\"enabledModuleNames\": $MATCH_MODULES}")

        MATCH_ID=$(echo "$MATCH_RESPONSE" | jq -r '.id')
        if [ -z "$MATCH_ID" ] || [ "$MATCH_ID" == "null" ]; then
            warn "Failed to create match $MATCH_NUM in container $CONTAINER_ID, skipping"
            continue
        fi

        # Spawn entities
        ENTITY_GROUPS=$(echo "$CONFIG" | jq -c '.entities // []')
        CURRENT_ENTITY_ID=0

        echo "$ENTITY_GROUPS" | jq -c '.[]' | while read -r GROUP; do
            COUNT=$(echo "$GROUP" | jq -r '.count // 1')
            ENTITY_TYPE=$(echo "$GROUP" | jq -r '.entityType // 100')

            # Position config
            POS_X_CONFIG=$(echo "$GROUP" | jq -c '.position.x // 0')
            POS_Y_CONFIG=$(echo "$GROUP" | jq -c '.position.y // 0')
            POS_Z_CONFIG=$(echo "$GROUP" | jq -c '.position.z // 0')

            # Rigid body config
            HAS_RIGID_BODY=$(echo "$GROUP" | jq -r '.rigidBody != null')
            if [ "$HAS_RIGID_BODY" = "true" ]; then
                VEL_X_CONFIG=$(echo "$GROUP" | jq -c '.rigidBody.velocity.x // 0')
                VEL_Y_CONFIG=$(echo "$GROUP" | jq -c '.rigidBody.velocity.y // 0')
                VEL_Z_CONFIG=$(echo "$GROUP" | jq -c '.rigidBody.velocity.z // 0')
                MASS=$(echo "$GROUP" | jq -r '.rigidBody.mass // 1.0')
                LINEAR_DRAG=$(echo "$GROUP" | jq -r '.rigidBody.linearDrag // 0.1')
                ANGULAR_DRAG=$(echo "$GROUP" | jq -r '.rigidBody.angularDrag // 0.1')
                INERTIA=$(echo "$GROUP" | jq -r '.rigidBody.inertia // 1.0')
            fi

            for ((i=0; i<COUNT; i++)); do
                CURRENT_ENTITY_ID=$((CURRENT_ENTITY_ID + 1))

                POS_X=$(get_value "$POS_X_CONFIG" "$i")
                POS_Y=$(get_value "$POS_Y_CONFIG" "$i")
                POS_Z=$(get_value "$POS_Z_CONFIG" "$i")

                # Spawn entity
                api POST "/api/containers/$CONTAINER_ID/commands" \
                    "{
                        \"commandName\": \"spawn\",
                        \"matchId\": $MATCH_ID,
                        \"playerId\": $PLAYER_ID,
                        \"parameters\": {
                            \"matchId\": $MATCH_ID,
                            \"playerId\": $PLAYER_ID,
                            \"entityType\": $ENTITY_TYPE
                        }
                    }" > /dev/null

                api POST "/api/containers/$CONTAINER_ID/tick" > /dev/null

                # Attach rigid body if configured
                if [ "$HAS_RIGID_BODY" = "true" ]; then
                    VEL_X=$(get_value "$VEL_X_CONFIG" "$i")
                    VEL_Y=$(get_value "$VEL_Y_CONFIG" "$i")
                    VEL_Z=$(get_value "$VEL_Z_CONFIG" "$i")

                    api POST "/api/containers/$CONTAINER_ID/commands" \
                        "{
                            \"commandName\": \"attachRigidBody\",
                            \"matchId\": $MATCH_ID,
                            \"playerId\": $PLAYER_ID,
                            \"parameters\": {
                                \"entityId\": $CURRENT_ENTITY_ID,
                                \"positionX\": $POS_X,
                                \"positionY\": $POS_Y,
                                \"positionZ\": $POS_Z,
                                \"velocityX\": $VEL_X,
                                \"velocityY\": $VEL_Y,
                                \"velocityZ\": $VEL_Z,
                                \"mass\": $MASS,
                                \"linearDrag\": $LINEAR_DRAG,
                                \"angularDrag\": $ANGULAR_DRAG,
                                \"inertia\": $INERTIA
                            }
                        }" > /dev/null

                    api POST "/api/containers/$CONTAINER_ID/tick" > /dev/null
                fi
            done
        done

        # Progress
        COMPLETED_MATCHES=$(( (c * NUM_MATCHES) + MATCH_NUM ))
        PERCENT=$(( (COMPLETED_MATCHES * 100) / TOTAL_MATCHES ))
        echo -ne "\r  Progress: Container $CONTAINER_NUM/$NUM_CONTAINERS, Match $MATCH_NUM/$NUM_MATCHES ($PERCENT%)"
    done
    echo ""
done

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

# Summary
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}       Seed Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Config File:    $CONFIG_FILE"
echo "API URL:        $API_URL"
echo "Containers:     $NUM_CONTAINERS"
echo "Matches/Container: $NUM_MATCHES"
echo "Entities/Match: $ENTITY_COUNT_PER_MATCH"
echo "Total Matches:  $TOTAL_MATCHES"
echo "Total Entities: $TOTAL_ENTITIES"
echo "Duration:       ${DURATION}s"
echo ""
echo "Container IDs: ${CONTAINER_IDS[*]}"
echo ""
