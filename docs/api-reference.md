# REST API Reference

Full API documentation: [openapi.yaml](../openapi.yaml)

Postman collection: [postman-collection.json](../postman-collection.json)

## Overview

Lightning Engine provides two API surfaces:

1. **Lightning Engine API** (default port 8080) - Container and game management
2. **Control Plane API** (default port 8081) - Cluster orchestration

## Authentication

### JWT Authentication

All Lightning Engine endpoints (except `/api/auth/login` and `/api/health`) require JWT authentication.

**Roles:**
- `admin` - Full access to all operations
- `command_manager` - Can manage matches, commands, and sessions
- `view_only` - Read-only access

Include the token in the `Authorization` header:

```bash
curl -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8080/api/...
```

### Control Plane Authentication

Control Plane node operations use the `X-Control-Plane-Token` header:

```bash
curl -H "X-Control-Plane-Token: YOUR_TOKEN" http://localhost:8081/api/nodes/...
```

---

## Endpoints Summary

### Authentication & Users

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/api/auth/login` | Login and get JWT token | None |
| POST | `/api/auth/refresh` | Refresh token | Any |
| GET | `/api/auth/me` | Get current user info | Any |
| **User Management** ||||
| GET | `/api/auth/users` | List all users | admin |
| POST | `/api/auth/users` | Create user | admin |
| GET | `/api/auth/users/{id}` | Get user by ID | admin |
| GET | `/api/auth/users/username/{username}` | Get user by username | admin |
| PUT | `/api/auth/users/{id}/password` | Update password | admin |
| PUT | `/api/auth/users/{id}/roles` | Update user roles | admin |
| POST | `/api/auth/users/{id}/roles/{roleName}` | Add role to user | admin |
| DELETE | `/api/auth/users/{id}/roles/{roleName}` | Remove role from user | admin |
| PUT | `/api/auth/users/{id}/enabled` | Enable/disable user | admin |
| DELETE | `/api/auth/users/{id}` | Delete user | admin |
| **Role Management** ||||
| GET | `/api/auth/roles` | List all roles | Any |
| POST | `/api/auth/roles` | Create role | admin |
| GET | `/api/auth/roles/{id}` | Get role by ID | Any |
| GET | `/api/auth/roles/name/{roleName}` | Get role by name | Any |
| PUT | `/api/auth/roles/{id}/description` | Update description | admin |
| PUT | `/api/auth/roles/{id}/includes` | Update role hierarchy | admin |
| DELETE | `/api/auth/roles/{id}` | Delete role | admin |

### Health

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/health` | Health check | None |

### Containers

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/containers` | List all containers | Any |
| POST | `/api/containers` | Create container | admin |
| GET | `/api/containers/{id}` | Get container details | Any |
| DELETE | `/api/containers/{id}` | Delete container | admin |
| POST | `/api/containers/{id}/start` | Start container | admin |
| POST | `/api/containers/{id}/stop` | Stop container | admin |
| POST | `/api/containers/{id}/pause` | Pause container | admin |
| POST | `/api/containers/{id}/resume` | Resume container | admin |
| GET | `/api/containers/{id}/stats` | Get container statistics | Any |

### Simulation Control

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/containers/{id}/tick` | Get current tick | Any |
| POST | `/api/containers/{id}/tick` | Advance one tick | admin, command_manager |
| POST | `/api/containers/{id}/play?intervalMs=16` | Start auto-advance | admin, command_manager |
| POST | `/api/containers/{id}/stop-auto` | Stop auto-advance | admin, command_manager |
| GET | `/api/containers/{id}/status` | Get playback status | Any |

### Container Matches

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/containers/{id}/matches` | List matches in container | Any |
| POST | `/api/containers/{id}/matches` | Create match in container | admin |
| GET | `/api/containers/{id}/matches/{matchId}` | Get match details | Any |
| DELETE | `/api/containers/{id}/matches/{matchId}` | Delete match | admin |

### Container Commands

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/containers/{id}/commands` | List available commands | Any |
| POST | `/api/containers/{id}/commands` | Queue command for next tick | admin, command_manager |
| WS | `/containers/{id}/commands?token=xxx` | Submit commands via WebSocket (Protobuf) | admin, command_manager |

### Container Modules

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/containers/{id}/modules` | List modules in container | Any |
| POST | `/api/containers/{id}/modules/reload` | Reload modules | admin |

### Container Snapshots

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/containers/{id}/matches/{matchId}/snapshot` | Get full snapshot | Any |
| GET | `/api/containers/{id}/matches/{matchId}/snapshots/delta` | Get delta snapshot | Any |
| POST | `/api/containers/{id}/matches/{matchId}/snapshots/record` | Record snapshot to history | admin, command_manager |
| GET | `/api/containers/{id}/matches/{matchId}/snapshots/history-info` | Get history info | Any |
| DELETE | `/api/containers/{id}/matches/{matchId}/snapshots/history` | Clear snapshot history | admin |
| WS | `/ws/containers/{id}/snapshots/{matchId}` | Stream snapshots | Any |
| WS | `/ws/containers/{id}/snapshots/delta/{matchId}` | Stream delta snapshots | Any |

### Container History (MongoDB)

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/containers/{id}/history` | Get container history summary | Any |
| GET | `/api/containers/{id}/matches/{matchId}/history` | Get match history summary | Any |
| GET | `/api/containers/{id}/matches/{matchId}/history/snapshots` | Get snapshots in range | Any |
| GET | `/api/containers/{id}/matches/{matchId}/history/snapshots/latest` | Get latest N snapshots | Any |
| GET | `/api/containers/{id}/matches/{matchId}/history/snapshots/{tick}` | Get snapshot at tick | Any |
| DELETE | `/api/containers/{id}/matches/{matchId}/history` | Delete match history | admin |
| DELETE | `/api/containers/{id}/matches/{matchId}/history/older-than/{tick}` | Delete old snapshots | admin |

### Container Restore

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/containers/{id}/matches/{matchId}/restore/available` | Check if restore available | Any |
| POST | `/api/containers/{id}/matches/{matchId}/restore` | Restore match to tick | admin |
| POST | `/api/containers/{id}/restore/all` | Restore all matches | admin |
| GET | `/api/containers/{id}/restore/config` | Get restore configuration | Any |

### Container Sessions

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/containers/{id}/sessions` | List all container sessions | Any |
| GET | `/api/containers/{id}/matches/{matchId}/sessions` | List sessions for match | Any |
| POST | `/api/containers/{id}/matches/{matchId}/sessions` | Connect player to session | admin, command_manager |
| GET | `/api/containers/{id}/matches/{matchId}/sessions/active` | List active sessions | Any |
| GET | `/api/containers/{id}/matches/{matchId}/sessions/{playerId}` | Get player session | Any |
| POST | `/api/containers/{id}/matches/{matchId}/sessions/{playerId}/reconnect` | Reconnect session | admin, command_manager |
| POST | `/api/containers/{id}/matches/{matchId}/sessions/{playerId}/disconnect` | Disconnect session | admin, command_manager |
| POST | `/api/containers/{id}/matches/{matchId}/sessions/{playerId}/abandon` | Abandon session | admin, command_manager |
| GET | `/api/containers/{id}/matches/{matchId}/sessions/{playerId}/can-reconnect` | Check reconnect capability | Any |

### Container Players

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/containers/{id}/players` | List players in container | Any |
| POST | `/api/containers/{id}/players` | Create player | admin |
| GET | `/api/containers/{id}/players/{playerId}` | Get player | Any |
| DELETE | `/api/containers/{id}/players/{playerId}` | Delete player | admin |
| GET | `/api/containers/{id}/matches/{matchId}/players` | List players in match | Any |
| POST | `/api/containers/{id}/matches/{matchId}/players` | Join player to match | admin |
| GET | `/api/containers/{id}/matches/{matchId}/players/{playerId}` | Get player in match | Any |
| DELETE | `/api/containers/{id}/matches/{matchId}/players/{playerId}` | Remove player from match | admin |

### Container Metrics

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/containers/{id}/metrics` | Get container metrics | Any |
| POST | `/api/containers/{id}/metrics/reset` | Reset metrics | admin |

### Container Resources

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/containers/{id}/resources` | List resources in container | Any |
| POST | `/api/containers/{id}/resources` | Upload resource to container | admin |
| GET | `/api/containers/{id}/resources/{resourceId}` | Get resource metadata | Any |
| DELETE | `/api/containers/{id}/resources/{resourceId}` | Delete resource | admin |

### Container AI

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/containers/{id}/ai` | List available AI backends | Any |

### Modules (Global)

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/modules` | List installed modules | Any |
| GET | `/api/modules/{name}` | Get module details | Any |
| POST | `/api/modules/upload` | Upload module JAR | admin |
| POST | `/api/modules/reload` | Reload all modules | admin |
| DELETE | `/api/modules/{name}` | Uninstall module | admin |

### AI (Global)

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/ai` | List installed AI backends | Any |
| GET | `/api/ai/{name}` | Get AI details | Any |
| POST | `/api/ai/upload` | Upload AI JAR | admin |
| POST | `/api/ai/reload` | Reload all AI backends | admin |
| DELETE | `/api/ai/{name}` | Uninstall AI backend | admin |

### Node (Self-Reporting)

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/node/metrics` | Get node metrics | None |
| GET | `/api/node/status` | Get node registration status | None |

---

## Control Plane Endpoints

### Deploy API (v1)

The primary CLI-facing endpoint for deploying games to the cluster.

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/api/v1/deploy` | Deploy a new game match | None |
| GET | `/api/v1/deploy/{matchId}` | Get deployment status | None |
| DELETE | `/api/v1/deploy/{matchId}` | Undeploy a match | None |

#### Deploy Request

```json
{
  "modules": ["EntityModule", "HealthModule"],
  "preferredNodeId": "node-1",  // optional
  "autoStart": true             // optional, defaults to true
}
```

#### Deploy Response

```json
{
  "matchId": "node-1-42-7",
  "nodeId": "node-1",
  "containerId": 42,
  "status": "RUNNING",
  "createdAt": "2026-01-28T10:30:00Z",
  "modules": ["EntityModule", "HealthModule"],
  "endpoints": {
    "http": "http://192.168.1.10:8080/api/containers/42",
    "websocket": "ws://192.168.1.10:8080/ws/containers/42/snapshots/node-1-42-7",
    "commands": "ws://192.168.1.10:8080/containers/42/commands"
  }
}
```

### Node Management

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/api/nodes/register` | Register a node | X-Control-Plane-Token |
| PUT | `/api/nodes/{nodeId}/heartbeat` | Send heartbeat | X-Control-Plane-Token |
| POST | `/api/nodes/{nodeId}/drain` | Mark node as draining | X-Control-Plane-Token |
| DELETE | `/api/nodes/{nodeId}` | Deregister node | X-Control-Plane-Token |

### Cluster Status

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/cluster/nodes` | List all cluster nodes | None |
| GET | `/api/cluster/nodes/{nodeId}` | Get node by ID | None |
| GET | `/api/cluster/status` | Get cluster health overview | None |

### Match Routing

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/api/matches/create` | Create match on best node | None |
| GET | `/api/matches` | List all matches | None |
| GET | `/api/matches/{matchId}` | Get match details | None |
| DELETE | `/api/matches/{matchId}` | Delete match | None |
| POST | `/api/matches/{matchId}/finish` | Mark match as finished | None |
| PUT | `/api/matches/{matchId}/players` | Update player count | None |

### Module Registry

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/modules` | List all modules in registry | None |
| POST | `/api/modules` | Upload module to registry | None |
| GET | `/api/modules/{name}` | List module versions | None |
| GET | `/api/modules/{name}/{version}` | Get module metadata | None |
| GET | `/api/modules/{name}/{version}/download` | Download module JAR | None |
| DELETE | `/api/modules/{name}/{version}` | Delete module version | None |
| POST | `/api/modules/{name}/{version}/distribute` | Distribute to all nodes | None |
| POST | `/api/modules/{name}/{version}/distribute/{nodeId}` | Distribute to specific node | None |

### Autoscaler

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/autoscaler/recommendation` | Get scaling recommendation | None |
| POST | `/api/autoscaler/acknowledge` | Acknowledge scaling action | None |
| GET | `/api/autoscaler/status` | Get autoscaler status | None |

### Dashboard

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/dashboard/overview` | Get cluster overview | None |
| GET | `/api/dashboard/nodes` | Get paginated nodes | None |
| GET | `/api/dashboard/matches` | Get paginated matches | None |

---

## Authentication Examples

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin"}'

# Response:
# {
#   "jwtToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "username": "admin",
#   "roles": ["admin"],
#   "expiresAt": "2024-01-11T00:00:00Z"
# }
```

### Refresh Token

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Authorization: Bearer $TOKEN"
```

### Get Current User

```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

### Create User (Admin Only)

```bash
curl -X POST http://localhost:8080/api/auth/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username": "newuser", "password": "password123", "roles": ["view_only"]}'
```

### Create Role with Hierarchy (Admin Only)

```bash
curl -X POST http://localhost:8080/api/auth/roles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "game_operator", "description": "Can manage games", "includedRoles": ["command_manager"]}'
```

---

## Offline Token CLI (issue-api-token)

For automated services, CI/CD pipelines, or long-lived integrations, generate offline tokens using the `issue-api-token` module:

### Build

```bash
./mvnw package -pl issue-api-token -DskipTests
```

### Generate Token

```bash
# Admin token with 24h expiry (default)
java -jar issue-api-token/target/issue-api-token.jar \
  --roles=admin \
  --secret=your-jwt-secret

# Custom user and expiry
java -jar issue-api-token/target/issue-api-token.jar \
  --roles=command_manager,view_only \
  --user=ci-pipeline \
  --expiry=168  # Hours (7 days)

# Output: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### CLI Options

| Option | Default | Description |
|--------|---------|-------------|
| `--roles` | (required) | Comma-separated role names |
| `--secret` | (required) | JWT signing secret (must match backend) |
| `--user` | `api-token-user` | Username claim in token |
| `--expiry` | `24` | Token validity in hours |

### Usage in Scripts

```bash
# Generate and store token
TOKEN=$(java -jar issue-api-token.jar --roles=admin --secret=$JWT_SECRET)

# Use in curl
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/containers
```

---

## Container Examples

Execution Containers provide isolated runtime environments with ClassLoader isolation, independent game loops, and container-scoped matches.

### Create Container

```bash
curl -X POST http://localhost:8080/api/containers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my-game-server",
    "maxEntities": 10000,
    "maxMemoryMb": 512,
    "moduleNames": ["EntityModule", "RigidBodyModule"]
  }'

# Response:
# {
#   "id": 1,
#   "name": "my-game-server",
#   "status": "CREATED",
#   "tick": 0,
#   "matchCount": 0
# }
```

### List Containers

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/containers

# Response: [{"id": 1, "name": "my-game-server", "status": "CREATED"}, ...]
```

### Start/Stop/Pause/Resume Container

```bash
# Start container
curl -X POST http://localhost:8080/api/containers/1/start \
  -H "Authorization: Bearer $TOKEN"

# Stop container
curl -X POST http://localhost:8080/api/containers/1/stop \
  -H "Authorization: Bearer $TOKEN"

# Pause container (keeps state, stops ticking)
curl -X POST http://localhost:8080/api/containers/1/pause \
  -H "Authorization: Bearer $TOKEN"

# Resume container
curl -X POST http://localhost:8080/api/containers/1/resume \
  -H "Authorization: Bearer $TOKEN"
```

### Get Container Statistics

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/containers/1/stats

# Response:
# {
#   "entityCount": 150,
#   "maxEntities": 10000,
#   "usedMemoryBytes": 52428800,
#   "maxMemoryBytes": 536870912,
#   "matchCount": 3,
#   "moduleCount": 5
# }
```

---

## Simulation Control Examples

### Tick Control

```bash
# Get current tick
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/containers/1/tick
# Response: {"currentTick": 42}

# Advance one tick
curl -X POST http://localhost:8080/api/containers/1/tick \
  -H "Authorization: Bearer $TOKEN"
# Response: {"currentTick": 43}

# Start auto-advance at 60 FPS
curl -X POST "http://localhost:8080/api/containers/1/play?intervalMs=16" \
  -H "Authorization: Bearer $TOKEN"

# Stop auto-advance
curl -X POST http://localhost:8080/api/containers/1/stop-auto \
  -H "Authorization: Bearer $TOKEN"

# Get playback status
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/containers/1/status
# Response: {"isPlaying": true, "currentTick": 100, "interval": 16}
```

---

## Match Examples

### Create Match in Container

```bash
curl -X POST http://localhost:8080/api/containers/1/matches \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "enabledModuleNames": ["EntityModule", "RigidBodyModule", "RenderingModule"],
    "enabledAINames": ["BasicAI"]
  }'
# Response: {"id": 1, "containerId": 1, "enabledModuleNames": [...]}
```

### List Matches in Container

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/containers/1/matches
```

### Delete Match

```bash
curl -X DELETE http://localhost:8080/api/containers/1/matches/1 \
  -H "Authorization: Bearer $TOKEN"
```

---

## Command Examples

### List Available Commands

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/containers/1/commands
# Response:
# [
#   {"name": "spawn", "description": "Spawn entity", "module": "EntityModule", "parameters": [...]},
#   {"name": "move", "description": "Move entity", "module": "MoveModule", "parameters": [...]}
# ]
```

### Queue Command

```bash
curl -X POST http://localhost:8080/api/containers/1/commands \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "commandName": "spawn",
    "parameters": {
      "matchId": 1,
      "entityType": 1,
      "playerId": 1
    }
  }'
# Response: 202 Accepted
```

### WebSocket Command Endpoint

For high-performance command submission, use the WebSocket endpoint with Protocol Buffer messages.

**Endpoint:** `ws://localhost:8080/containers/{id}/commands?token=xxx`

```javascript
// JavaScript WebSocket example
const token = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...';
const ws = new WebSocket(`ws://localhost:8080/containers/1/commands?token=${token}`);

ws.binaryType = 'arraybuffer';

ws.onopen = () => {
  console.log('Connected to command WebSocket');
};

ws.onmessage = (event) => {
  const response = CommandResponse.decode(new Uint8Array(event.data));
  console.log(`Command ${response.commandName}: ${response.status}`);
};

// Send spawn command
const request = CommandRequest.create({
  commandName: 'spawn',
  matchId: 1,
  playerId: 1,
  spawn: { entityType: 100, positionX: 10, positionY: 20 }
});
ws.send(CommandRequest.encode(request).finish());
```

---

## Player & Session Examples

### Create Player

```bash
curl -X POST http://localhost:8080/api/containers/1/players \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}'
# Response: {"id": 1}
```

### Join Player to Match

```bash
curl -X POST http://localhost:8080/api/containers/1/matches/1/players \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"playerId": 1}'
```

### Connect Session

```bash
curl -X POST http://localhost:8080/api/containers/1/matches/1/sessions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"playerId": 1}'
```

### Disconnect/Reconnect Session

```bash
# Disconnect
curl -X POST http://localhost:8080/api/containers/1/matches/1/sessions/1/disconnect \
  -H "Authorization: Bearer $TOKEN"

# Check if can reconnect
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/containers/1/matches/1/sessions/1/can-reconnect
# Response: {"canReconnect": true}

# Reconnect
curl -X POST http://localhost:8080/api/containers/1/matches/1/sessions/1/reconnect \
  -H "Authorization: Bearer $TOKEN"
```

---

## Snapshot Examples

### Get Match Snapshot

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/containers/1/matches/1/snapshot

# Response:
# {
#   "matchId": 1,
#   "tick": 42,
#   "data": {
#     "EntityModule": {
#       "POSITION_X": [100.0, 200.0, 150.0],
#       "POSITION_Y": [50.0, 75.0, 100.0]
#     },
#     "HealthModule": {
#       "HEALTH": [100.0, 80.0, 50.0]
#     }
#   }
# }
```

### Get Delta Snapshot

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/containers/1/matches/1/snapshots/delta?fromTick=100&toTick=105"

# Response:
# {
#   "matchId": 1,
#   "fromTick": 100,
#   "toTick": 105,
#   "changedComponents": {...},
#   "addedEntities": [10, 11],
#   "removedEntities": [5],
#   "changeCount": 15,
#   "compressionRatio": 0.12
# }
```

### WebSocket Streaming

```javascript
// Full snapshots
const ws = new WebSocket('ws://localhost:8080/ws/containers/1/snapshots/1');
ws.onmessage = (event) => {
  const snapshot = JSON.parse(event.data);
  console.log(`Tick ${snapshot.tick}:`, snapshot.data);
};

// Delta snapshots
const wsDelta = new WebSocket('ws://localhost:8080/ws/containers/1/snapshots/delta/1');
wsDelta.onmessage = (event) => {
  const delta = JSON.parse(event.data);
  console.log(`Changes: ${delta.changeCount}`);
};
```

---

## History & Restore Examples

### Get Match History

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/containers/1/matches/1/history
```

### Get Snapshots in Range

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/containers/1/matches/1/history/snapshots?fromTick=0&toTick=100&limit=50"
```

### Get Latest Snapshots

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/containers/1/matches/1/history/snapshots/latest?limit=10"
```

### Restore Match

```bash
# Check if restore is available
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/containers/1/matches/1/restore/available
# Response: {"matchId": 1, "canRestore": true}

# Restore to specific tick
curl -X POST "http://localhost:8080/api/containers/1/matches/1/restore?tick=100" \
  -H "Authorization: Bearer $TOKEN"

# Restore to latest snapshot (tick=-1)
curl -X POST http://localhost:8080/api/containers/1/matches/1/restore \
  -H "Authorization: Bearer $TOKEN"

# Restore all matches in container
curl -X POST http://localhost:8080/api/containers/1/restore/all \
  -H "Authorization: Bearer $TOKEN"
```

---

## Module Examples

### Upload Module

```bash
curl -X POST http://localhost:8080/api/modules/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@target/my-module.jar"
```

### Reload Modules

```bash
curl -X POST http://localhost:8080/api/modules/reload \
  -H "Authorization: Bearer $TOKEN"
```

### List Modules

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/modules
# Response:
# [
#   {"moduleName": "EntityModule", "flagComponentName": "ENTITY_FLAG", "enabledMatches": 3},
#   {"moduleName": "RigidBodyModule", "flagComponentName": "RIGID_BODY_FLAG", "enabledMatches": 2}
# ]
```

---

## Resource Examples

Resources are container-scoped. Each container has its own isolated resource storage.

### Upload Resource

```bash
curl -X POST http://localhost:8080/api/containers/1/resources \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@my-sprite.png" \
  -F "resourceName=player-sprite" \
  -F "resourceType=TEXTURE"
# Response: {"resourceId": 1, "resourceName": "player-sprite", "resourceType": "TEXTURE"}
```

### List Resources

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/containers/1/resources
```

### Delete Resource

```bash
curl -X DELETE -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/containers/1/resources/1
```

---

## AI Examples

### List AI Backends

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/ai
# Response:
# [
#   {"aiName": "BasicAI", "enabledMatches": 2},
#   {"aiName": "AdvancedAI", "enabledMatches": 1}
# ]
```

### Upload AI JAR

```bash
curl -X POST http://localhost:8080/api/ai/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@my-ai.jar"
```

---

## Control Plane Examples

### Register Node

```bash
curl -X POST http://localhost:8081/api/nodes/register \
  -H "X-Control-Plane-Token: $CP_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nodeId": "node-1",
    "advertiseAddress": "http://192.168.1.10:8080",
    "capacity": {"maxContainers": 10}
  }'
```

### Send Heartbeat

```bash
curl -X PUT http://localhost:8081/api/nodes/node-1/heartbeat \
  -H "X-Control-Plane-Token: $CP_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "metrics": {
      "containerCount": 3,
      "matchCount": 15,
      "cpuUsage": 45.5,
      "memoryUsedMb": 2048,
      "memoryMaxMb": 8192
    }
  }'
```

### Get Cluster Status

```bash
curl http://localhost:8081/api/cluster/status
# Response:
# {
#   "totalNodes": 5,
#   "healthyNodes": 4,
#   "drainingNodes": 1,
#   "totalCapacity": 50,
#   "usedCapacity": 23,
#   "averageSaturation": 0.46
# }
```

### Create Match via Control Plane

```bash
curl -X POST http://localhost:8081/api/matches/create \
  -H "Content-Type: application/json" \
  -d '{
    "moduleNames": ["EntityModule", "RigidBodyModule"],
    "preferredNodeId": "node-1"
  }'
# Response:
# {
#   "matchId": "abc-123",
#   "nodeId": "node-1",
#   "status": "RUNNING",
#   "advertiseAddress": "http://192.168.1.10:8080",
#   "websocketUrl": "ws://192.168.1.10:8080/ws/containers/1/snapshots/1"
# }
```

### Upload Module to Registry

```bash
curl -X POST http://localhost:8081/api/modules \
  -F "name=MyModule" \
  -F "version=1.0.0" \
  -F "description=My custom module" \
  -F "file=@my-module.jar"
```

### Distribute Module to All Nodes

```bash
curl -X POST http://localhost:8081/api/modules/MyModule/1.0.0/distribute
# Response: {"moduleName": "MyModule", "moduleVersion": "1.0.0", "nodesUpdated": 5}
```

### Get Scaling Recommendation

```bash
curl http://localhost:8081/api/autoscaler/recommendation
# Response:
# {
#   "action": "SCALE_UP",
#   "suggestedNodeCount": 7,
#   "reason": "High cluster saturation (85%)",
#   "currentSaturation": 0.85,
#   "targetSaturation": 0.70
# }
```

### Get Dashboard Overview

```bash
curl http://localhost:8081/api/dashboard/overview
# Response:
# {
#   "clusterHealth": {"status": "HEALTHY", "healthyNodes": 4, "totalNodes": 5},
#   "nodes": {"total": 5, "healthy": 4, "draining": 1},
#   "matches": {"total": 50, "running": 45, "pending": 5},
#   "capacity": {"total": 50, "used": 23, "available": 27, "saturationPercent": 46.0},
#   "autoscaler": {"enabled": true, "recommendation": "NONE"}
# }
```
