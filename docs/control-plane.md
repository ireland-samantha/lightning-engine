# Lightning Control Plane

The Lightning Control Plane is a cluster orchestration layer for managing multiple Lightning Engine nodes. It provides centralized node registration, intelligent match scheduling, module distribution, and autoscaling recommendations.

## Overview

The Control Plane enables horizontal scaling of Lightning Engine by coordinating a cluster of Lightning Engine nodes. Instead of clients connecting directly to individual game server nodes, they interact with the Control Plane which:

- **Tracks all cluster nodes** via heartbeat-based registration
- **Routes match creation requests** to the optimal node
- **Manages a centralized module registry** for distributing game modules
- **Provides autoscaling recommendations** based on cluster saturation
- **Offers a unified API** for cluster management

### Key Benefits

- **Simplified client integration**: Clients only need the Control Plane URL
- **Automatic load balancing**: Matches are scheduled to least-loaded nodes
- **Centralized module management**: Upload once, distribute everywhere
- **Horizontal scalability**: Add/remove nodes without client changes
- **Observability**: Single point for cluster-wide metrics and status

## Architecture

The Control Plane follows a clean architecture with pure domain logic in `lightning-control-plane-core` and Quarkus-specific providers in `lightning-control-plane`.

```
                          +----------------------+
                          |   Control Plane      |
                          |   (REST API)         |
                          +----------+-----------+
                                     |
         +---------------------------+---------------------------+
         |                           |                           |
+--------v--------+        +---------v--------+        +---------v--------+
|   Node 1        |        |   Node 2         |        |   Node 3         |
| (Lightning      |        | (Lightning       |        | (Lightning       |
|  Engine)        |        |  Engine)         |        |  Engine)         |
+-----------------+        +------------------+        +------------------+
```

### Core Components

#### 1. Node Registry (`NodeRegistryService`)

Tracks all Lightning Engine nodes in the cluster.

**Location**: `lightning-control-plane-core/src/main/java/ca/samanthaireland/lightning/controlplane/node/service/`

**Responsibilities**:
- Register new nodes with capacity information
- Process heartbeats to update node metrics and refresh TTL
- Mark nodes as draining (stops accepting new work)
- Deregister nodes from the cluster
- Maintain node health status (HEALTHY, DRAINING)

**Key Model**: `Node`
```java
public record Node(
    String nodeId,              // Unique identifier (UUID or custom)
    String advertiseAddress,    // URL for reaching the node (http://node1:8080)
    NodeStatus status,          // HEALTHY or DRAINING
    NodeCapacity capacity,      // Max containers the node can host
    NodeMetrics metrics,        // Current container/match count, CPU, memory
    Instant registeredAt,       // When node first registered
    Instant lastHeartbeat       // When node last sent heartbeat
) { ... }
```

#### 2. Scheduler (`SchedulerService`)

Selects the optimal node for hosting new matches.

**Location**: `lightning-control-plane-core/src/main/java/ca/samanthaireland/lightning/controlplane/scheduler/service/`

**Selection Algorithm**:
1. Filter to only HEALTHY nodes
2. Filter to nodes with available capacity (`containerCount < maxContainers`)
3. If preferred node is specified and available, use it
4. Otherwise, select the least-loaded node (lowest saturation)

**Saturation Calculation**:
```
saturation = activeContainers / maxContainers
```

**Exceptions**:
- `NoAvailableNodesException`: No healthy nodes exist
- `NoCapableNodesException`: All nodes are at capacity

#### 3. Match Routing (`MatchRoutingService`)

Coordinates match creation across the cluster.

**Location**: `lightning-control-plane-core/src/main/java/ca/samanthaireland/lightning/controlplane/match/service/`

**Match Creation Flow**:
1. Scheduler selects a node
2. Create container on the selected node (via HTTP)
3. Create match in the container (via HTTP)
4. Generate cluster-unique match ID: `{nodeId}-{containerId}-{matchId}`
5. Store registry entry with connection information
6. Return WebSocket URL for client connection

**Key Model**: `MatchRegistryEntry`
```java
public record MatchRegistryEntry(
    String matchId,             // Cluster-unique ID: nodeId-containerId-matchId
    String nodeId,              // Hosting node ID
    long containerId,           // Container on the node
    MatchStatus status,         // CREATING, RUNNING, FINISHED, ERROR
    Instant createdAt,          // Creation timestamp
    List<String> moduleNames,   // Enabled modules
    String advertiseAddress,    // Node's HTTP address
    String websocketUrl,        // Full WS URL for client connection
    int playerCount             // Current connected players
) { ... }
```

#### 4. Module Registry (`ModuleRegistryService`)

Centralized storage and distribution of game modules.

**Location**: `lightning-control-plane-core/src/main/java/ca/samanthaireland/lightning/controlplane/module/service/`

**Responsibilities**:
- Store module JAR files with metadata
- Version management (multiple versions per module)
- Distribute modules to specific nodes or all nodes
- Download modules for node consumption

**Key Model**: `ModuleMetadata`
```java
public record ModuleMetadata(
    String name,            // Module name (e.g., "entity-module")
    String version,         // Semantic version (e.g., "1.0.0")
    String description,     // Human-readable description
    String fileName,        // Original JAR filename
    long fileSize,          // Size in bytes
    String checksum,        // SHA-256 checksum
    Instant uploadedAt,     // Upload timestamp
    String uploadedBy       // Uploader identifier
) { ... }
```

#### 5. Autoscaler (`AutoscalerService`)

Analyzes cluster state and recommends scaling actions.

**Location**: `lightning-control-plane-core/src/main/java/ca/samanthaireland/lightning/controlplane/autoscaler/service/`

**Scaling Logic**:
- **Scale Up**: When cluster saturation exceeds `scaleUpThreshold` (default: 80%)
- **Scale Down**: When cluster saturation falls below `scaleDownThreshold` (default: 30%)
- **Cooldown**: Prevents rapid scaling oscillation (default: 300 seconds)

**Key Model**: `ScalingRecommendation`
```java
public record ScalingRecommendation(
    ScalingAction action,       // NONE, SCALE_UP, SCALE_DOWN
    int currentNodes,           // Current healthy node count
    int recommendedNodes,       // Target node count
    int nodeDelta,              // Nodes to add (positive) or remove (negative)
    double currentSaturation,   // Current cluster saturation (0.0 - 1.0)
    double targetSaturation,    // Expected saturation after scaling
    String reason,              // Human-readable explanation
    Instant timestamp           // When recommendation was generated
) { ... }
```

#### 6. Cluster Service (`ClusterService`)

Provides cluster-wide status and node listing.

**Location**: `lightning-control-plane-core/src/main/java/ca/samanthaireland/lightning/controlplane/cluster/service/`

**Key Model**: `ClusterStatus`
```java
public record ClusterStatus(
    int totalNodes,             // All registered nodes
    int healthyNodes,           // HEALTHY nodes
    int drainingNodes,          // DRAINING nodes
    int totalContainers,        // Sum of containers across nodes
    int totalMatches,           // Sum of matches across nodes
    int totalCapacity,          // Sum of maxContainers across nodes
    int availableCapacity       // Sum of available slots on healthy nodes
) { ... }
```

## Node Registration Flow

Lightning Engine nodes register with the Control Plane on startup and maintain their registration via periodic heartbeats.

### Sequence Diagram

```
Node                          Control Plane
  |                                  |
  |------- POST /api/nodes/register -->
  |        {nodeId, advertiseAddress,|
  |         capacity}                |
  |                                  |
  |<-------- 201 Created -----------|
  |        {node details}            |
  |                                  |
  |                                  |
  |------- PUT /api/nodes/{id}/heartbeat -->
  |        {metrics}                 |  (every 10s)
  |                                  |
  |<-------- 200 OK ------------------|
  |        {updated node}            |
  |                                  |
  ~                                  ~
  |                                  |
  |------- DELETE /api/nodes/{id} -->
  |                                  |  (on shutdown)
  |<-------- 204 No Content ---------|
```

### Node Configuration (Lightning Engine side)

Configure nodes to register with the Control Plane via `application.properties`:

```properties
# Control Plane URL (enables registration when set)
control-plane.url=http://control-plane:8081

# Shared secret for authentication
control-plane.token=my-secret-token

# Address other services use to reach this node
control-plane.advertise-address=http://node1:8080

# Optional: Custom node ID (UUID generated if not set)
control-plane.node-id=node-1

# Maximum containers this node can host
control-plane.max-containers=100

# Heartbeat interval (should be less than TTL)
control-plane.heartbeat-interval-seconds=10
```

### Node TTL

Nodes have a configurable TTL (default: 30 seconds). If a node fails to send a heartbeat within this period, it will be removed from the registry. Heartbeats should be sent more frequently than the TTL (default: every 10 seconds).

### Node States

| Status | Description |
|--------|-------------|
| `HEALTHY` | Node is accepting new containers and matches |
| `DRAINING` | Node stops accepting new work; existing work continues |

To drain a node:
```http
POST /api/nodes/{nodeId}/drain
```

## Match Scheduling

When a client requests a new match, the Control Plane handles all coordination.

### Match Creation Flow

```
Client                        Control Plane                     Node
  |                                  |                            |
  |--- POST /api/matches/create ---->|                            |
  |    {moduleNames}                 |                            |
  |                                  |--- (scheduler selects) --> |
  |                                  |                            |
  |                                  |--- POST /api/containers -->|
  |                                  |    {modules}               |
  |                                  |<-- 201 {containerId} ------|
  |                                  |                            |
  |                                  |--- POST /api/matches ----->|
  |                                  |    {containerId, modules}  |
  |                                  |<-- 201 {matchId} ---------|
  |                                  |                            |
  |<-- 201 {matchId, websocketUrl} --|                            |
  |                                  |                            |
  |                                  |                            |
  |=============== WebSocket connection directly to node =========|
  |                                  |                            |
```

### Match Response

```json
{
  "matchId": "node-1-42-1",
  "nodeId": "node-1",
  "containerId": 42,
  "status": "RUNNING",
  "createdAt": "2026-01-28T10:30:00Z",
  "moduleNames": ["entity-module", "move-module"],
  "websocketUrl": "ws://node1:8080/ws/containers/42/matches/1/snapshots",
  "playerCount": 0
}
```

### Preferred Node Selection

Clients can request a specific node:

```json
{
  "moduleNames": ["entity-module"],
  "preferredNodeId": "node-1"
}
```

If the preferred node is healthy and has capacity, it will be used. Otherwise, the scheduler falls back to least-loaded selection.

## Autoscaling

The autoscaler analyzes cluster saturation and recommends scaling actions.

### Configuration

Configure via `application.properties`:

```properties
# Enable/disable autoscaler
autoscaler.enabled=true

# Scale up when saturation exceeds this (default: 80%)
autoscaler.scale-up-threshold=0.8

# Scale down when saturation falls below this (default: 30%)
autoscaler.scale-down-threshold=0.3

# Target saturation for scaling calculations (default: 60%)
autoscaler.target-saturation=0.6

# Minimum nodes to maintain
autoscaler.min-nodes=1

# Maximum nodes allowed
autoscaler.max-nodes=100

# Cooldown between scaling actions (default: 5 minutes)
autoscaler.cooldown-seconds=300
```

### Scaling Thresholds

```
0%                    30%                    60%                    80%                   100%
|------ SCALE_DOWN ----|-------- NONE --------|-------- NONE --------|------ SCALE_UP ------|
                       ^                       ^                      ^
                   scaleDown              targetSaturation        scaleUp
                   Threshold                                      Threshold
```

### Getting Recommendations

```http
GET /api/autoscaler/recommendation
```

Response:
```json
{
  "action": "SCALE_UP",
  "currentNodes": 3,
  "recommendedNodes": 5,
  "nodeDelta": 2,
  "currentSaturation": 0.85,
  "targetSaturation": 0.51,
  "reason": "Saturation (85.0%) exceeds threshold (80%), recommending 2 additional node(s)",
  "timestamp": "2026-01-28T10:30:00Z"
}
```

### Acknowledging Scaling Actions

After taking a scaling action, acknowledge it to start the cooldown timer:

```http
POST /api/autoscaler/acknowledge
```

During cooldown, recommendations will return `NONE` to prevent oscillation.

## API Endpoints

### Node Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/nodes/register` | Register a new node |
| `PUT` | `/api/nodes/{nodeId}/heartbeat` | Send heartbeat with metrics |
| `POST` | `/api/nodes/{nodeId}/drain` | Mark node as draining |
| `DELETE` | `/api/nodes/{nodeId}` | Deregister node |

### Cluster Status

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/cluster/nodes` | List all nodes |
| `GET` | `/api/cluster/nodes/{nodeId}` | Get specific node |
| `GET` | `/api/cluster/status` | Get cluster status summary |

### Match Routing

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/matches/create` | Create a new match |
| `GET` | `/api/matches` | List all matches |
| `GET` | `/api/matches?status=RUNNING` | List matches by status |
| `GET` | `/api/matches/{matchId}` | Get match details |
| `DELETE` | `/api/matches/{matchId}` | Delete a match |
| `POST` | `/api/matches/{matchId}/finish` | Mark match as finished |
| `PUT` | `/api/matches/{matchId}/players?count=N` | Update player count |

### Module Registry

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/modules` | Upload a module (multipart) |
| `GET` | `/api/modules` | List all modules |
| `GET` | `/api/modules/{name}` | List versions of a module |
| `GET` | `/api/modules/{name}/{version}` | Get module metadata |
| `GET` | `/api/modules/{name}/{version}/download` | Download module JAR |
| `DELETE` | `/api/modules/{name}/{version}` | Delete a module |
| `POST` | `/api/modules/{name}/{version}/distribute` | Distribute to all nodes |
| `POST` | `/api/modules/{name}/{version}/distribute/{nodeId}` | Distribute to specific node |

### Autoscaler

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/autoscaler/recommendation` | Get scaling recommendation |
| `GET` | `/api/autoscaler/status` | Get autoscaler status |
| `POST` | `/api/autoscaler/acknowledge` | Acknowledge scaling action |

### Authentication

Node registration endpoints require authentication via the `X-Control-Plane-Token` header:

```http
POST /api/nodes/register
X-Control-Plane-Token: my-secret-token
Content-Type: application/json

{
  "nodeId": "node-1",
  "advertiseAddress": "http://node1:8080",
  "capacity": {
    "maxContainers": 100
  }
}
```

## Deployment

### Running the Control Plane

The Control Plane runs as a separate Quarkus application:

```bash
cd lightning-control-plane
mvn quarkus:dev
```

Default port: `8081`

### Multi-Node Cluster Setup

#### 1. Start the Control Plane

```bash
# Control Plane (port 8081)
cd lightning-control-plane
export CONTROL_PLANE_AUTH_TOKEN=my-secret-token
mvn quarkus:dev -Dquarkus.http.port=8081
```

#### 2. Start Lightning Engine Nodes

```bash
# Node 1 (port 8080)
cd lightning-engine/webservice/quarkus-web-api
export CONTROL_PLANE_URL=http://localhost:8081
export CONTROL_PLANE_TOKEN=my-secret-token
export CONTROL_PLANE_ADVERTISE_ADDRESS=http://localhost:8080
export CONTROL_PLANE_NODE_ID=node-1
mvn quarkus:dev -Dquarkus.http.port=8080

# Node 2 (port 8082)
# In a new terminal:
cd lightning-engine/webservice/quarkus-web-api
export CONTROL_PLANE_URL=http://localhost:8081
export CONTROL_PLANE_TOKEN=my-secret-token
export CONTROL_PLANE_ADVERTISE_ADDRESS=http://localhost:8082
export CONTROL_PLANE_NODE_ID=node-2
mvn quarkus:dev -Dquarkus.http.port=8082
```

### Docker Compose Example

```yaml
version: '3.8'
services:
  control-plane:
    image: lightning-control-plane:latest
    ports:
      - "8081:8081"
    environment:
      - CONTROL_PLANE_AUTH_TOKEN=secret-token
      - AUTOSCALER_ENABLED=true
      - AUTOSCALER_MIN_NODES=2

  redis:
    image: redis:7
    ports:
      - "6379:6379"

  node-1:
    image: lightning-engine:latest
    ports:
      - "8080:8080"
    environment:
      - CONTROL_PLANE_URL=http://control-plane:8081
      - CONTROL_PLANE_TOKEN=secret-token
      - CONTROL_PLANE_ADVERTISE_ADDRESS=http://node-1:8080
      - CONTROL_PLANE_NODE_ID=node-1
    depends_on:
      - control-plane

  node-2:
    image: lightning-engine:latest
    ports:
      - "8082:8080"
    environment:
      - CONTROL_PLANE_URL=http://control-plane:8081
      - CONTROL_PLANE_TOKEN=secret-token
      - CONTROL_PLANE_ADVERTISE_ADDRESS=http://node-2:8080
      - CONTROL_PLANE_NODE_ID=node-2
    depends_on:
      - control-plane
```

### Configuration Reference

#### Control Plane Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `control-plane.node-ttl-seconds` | `30` | Node TTL in seconds |
| `control-plane.auth-token` | - | Shared secret for node authentication |
| `control-plane.require-auth` | `true` | Require authentication for node operations |

#### Autoscaler Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `autoscaler.enabled` | `true` | Enable autoscaler |
| `autoscaler.scale-up-threshold` | `0.8` | Scale up above this saturation |
| `autoscaler.scale-down-threshold` | `0.3` | Scale down below this saturation |
| `autoscaler.target-saturation` | `0.6` | Target saturation for calculations |
| `autoscaler.min-nodes` | `1` | Minimum node count |
| `autoscaler.max-nodes` | `100` | Maximum node count |
| `autoscaler.cooldown-seconds` | `300` | Cooldown between scaling actions |

#### Node Client Configuration (Lightning Engine side)

| Property | Default | Description |
|----------|---------|-------------|
| `control-plane.url` | - | Control Plane URL (enables integration) |
| `control-plane.token` | - | Shared secret for authentication |
| `control-plane.advertise-address` | - | URL where this node can be reached |
| `control-plane.node-id` | (generated) | Unique node identifier |
| `control-plane.max-containers` | `100` | Maximum containers for this node |
| `control-plane.heartbeat-interval-seconds` | `10` | Heartbeat frequency |

## Persistence

The Control Plane uses Redis for node and match registry storage:

- **Node Registry**: Redis hash with TTL-based expiration
- **Match Registry**: Redis hash for persistent match tracking
- **Module Storage**: File system storage for JAR files

Configure Redis connection via standard Quarkus properties:
```properties
quarkus.redis.hosts=redis://localhost:6379
```

Module storage path:
```properties
module-storage.base-path=/var/lightning/modules
```
