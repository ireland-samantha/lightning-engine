# Lightning Engine  
![Status](https://img.shields.io/badge/status-experimental-blueviolet)
![Java](https://img.shields.io/badge/java-25-blue)
![License](https://img.shields.io/github/license/ireland-samantha/lightning-engine)
![Build](https://github.com/ireland-samantha/lightning-engine/actions/workflows/maven.yml/badge.svg)

## What is this?
Lightning Engine is an open-source, unapologetically Java multiplayer server and backend game development framework.

The core idea: Run **multiple games and matches concurrently on the same JVM**, each inside an **isolated execution container** with its own ClassLoader, game loop, ECS store, and hot-reloadable game modules. 
So, multi-game, muti-match development with isolation on the JVM level instead of process level.

With Lightning, deploying a backend for a game looks like:
1. Building a JAR with your game logic  
2. Uploading it to a Lightning instance  
3. Triggering hot reload  
4. Spawning containers and matches  
5. Subscribing to state updates, and triggering commands, via Websocket.

## Why?
Mostly for fun, but also [some other reasons](docs/why.md) 

## Warning
Lightning Engine is an experimental hobby project, not production software. It exists for fun, learning, and the pure joy of over-engineering. 

It is:
 - experimental
 - unstable
 - aggressively over-engineered
 - powered by Java, hubris, and curiosity
 - built via ~vibe coding~ pair programming (read: tokens go burr while I yell at it for making mistakes) with Claude Code.

If any of this sounds appealing, we are currently looking for fellow travelers
who enjoy complexity, puzzles, and whacky ideas executed thoughtfully.

## Key Capabilities

### Execution Containers
- Isolated runtime environments using **ClassLoader isolation**
- Independent game loops, ECS stores, and resources
- Multiple games running safely on the same server

### ECS (Entity Component System)
- Array-based ECS storage with **O(1) component access** (because lightning and all)
- Float-based values for cache efficiency
- Per-tick query caching
- Match-scoped entity filtering

### Hot-Reloadable Modules
- Upload JARs at runtime
- Enable/disable modules per match
- Reload game logic **without restarting the server**

### Real-Time Streaming
- WebSocket streaming of ECS snapshots
- Full or delta-compressed updates every tick
- Designed for web or native clients

### Web Dashboard
- React-based admin UI
- Container & match management
- Live ECS inspection
- Command console
- Resource browser

### Testing & Automation
- Headless mode for GPU-free testing
- JUnit integration tests
- Playwright E2E tests
- Testcontainers for infrastructure tests

### Security
- JWT-based authentication
- Hierarchical, role-based access control
- Offline token generation for CI/CD
- Modules authenticate during installation and are issued a JWT identifying what components they can modify.


---

## Core Concepts

| Concept | Description |
|------|------------|
| **Execution Container** | Isolated runtime with its own ECS, modules, resources, and game loop |
| **Match** | A game session running inside a container |
| **Module** | Hot-reloadable game logic packaged as a JAR |
| **Command** | Server-side action queued and executed during ticks |
| **Snapshot** | Serialized ECS state streamed to clients |

---

## Tech Stack

| Layer | Technology |
|-----|-----------|
| Language | Java 25 |
| Backend | Quarkus 3.x (REST + WebSocket) |
| Build | Maven (multi-module) |
| Frontend | React + TypeScript + Material-UI |
| Testing | JUnit 5, Playwright, Testcontainers |
| Persistence | MongoDB |
| API Docs | OpenAPI 3.0 |

---

## 🏁 Quick Start

### Option 1: Docker (Fastest)

```bash
docker run -d \
  --name lightning-engine \
  -p 8080:8080 \
  -e ADMIN_INITIAL_PASSWORD=your-secure-password \
  -e AUTH_JWT_SECRET=your-jwt-secret-at-least-32-chars \
  samanthacireland/lightning-engine:latest
```

### Option 2: Docker Compose (Recommended)

Includes MongoDB for snapshot persistence:

```bash
git clone https://github.com/ireland-samantha/lightning-engine.git
cd lightning-engine

export ADMIN_INITIAL_PASSWORD=your-secure-password
docker compose up -d
```

### Option 3: Build from Source

```bash
git clone https://github.com/ireland-samantha/lightning-engine.git
cd lightning-engine

./build.sh build            # Build all modules
./build.sh docker           # Build Docker image
./build.sh integration-test # Run integration tests
```

This starts:
- **lightning-engine** - The game server on port 8080
- **mongodb** - For snapshot history persistence

### Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `ADMIN_INITIAL_PASSWORD` | Yes | Password for the admin user |
| `AUTH_JWT_SECRET` | Yes | Secret for JWT signing (use a long random string, 32+ chars) |
| `CORS_ORIGINS` | Production | Allowed CORS origins (e.g., `https://yourdomain.com`) |
| `QUARKUS_MONGODB_CONNECTION_STRING` | No | MongoDB connection (default: `mongodb://mongodb:27017`) |
| `SNAPSHOT_PERSISTENCE_ENABLED` | No | Enable snapshot history (default: `true` with MongoDB) |

Open the React admin dashboard at:

```
http://localhost:8080/admin/dashboard
```
![demo.png](docs/demo.png)
**Credentials:**
- Username: `admin`
- Password: The value you set in `ADMIN_INITIAL_PASSWORD` (or check logs for the generated password)

---

## Local Development Setup

### Prerequisites

- Java 25 (with preview features enabled)
- Maven 3.9+
- Node.js 18+ and npm (for frontend)
- MongoDB 6.0+ (running locally or via Docker)
- Docker (for running integration tests)

### Build from Source
First, take a deep breath.

```bash
# Clone the repository
git clone https://github.com/ireland-samantha/lightning-engine.git
cd lightning-engine
```

Use the `build.sh` script for common tasks:

```bash
./build.sh clean            # Clean build artifacts
./build.sh build            # Build all modules (skip tests)
./build.sh test             # Run unit tests
./build.sh docker           # Build Docker image
./build.sh integration-test # Build Docker + run integration tests
./build.sh all              # Full pipeline: clean → build → test → integration-test
```

Or use Maven directly:

```bash
# Build all modules (skip tests for faster initial build)
mvn clean install -DskipTests

# Build with tests
mvn clean install
```

### Run MongoDB

```bash
# Using Docker
docker run -d --name mongodb -p 27017:27017 mongo:6.0

# Or use your local MongoDB installation
```

### Run the Backend

```bash
# Set required environment variables
export ADMIN_INITIAL_PASSWORD=dev-password
export QUARKUS_MONGODB_CONNECTION_STRING=mongodb://localhost:27017

# Run in development mode (with hot reload)
cd lightning-engine/webservice/quarkus-web-api
mvn quarkus:dev
```

### Run the Frontend (Development)

```bash
cd lightning-engine/webservice/quarkus-web-api/src/main/frontend
npm install
npm run dev
```

The frontend dev server runs at `http://localhost:5173` and proxies API requests to the backend.

### Running Tests

```bash
# Run unit tests
./build.sh test

# Run integration tests (requires Docker)
./build.sh integration-test

# Or use Maven directly
mvn test                                    # Unit tests
mvn verify -Pacceptance-tests               # Integration tests
```

### IDE Setup

**IntelliJ IDEA** (Recommended):
1. Open the root `pom.xml` as a project
2. Enable annotation processing for Lombok (Settings → Build → Compiler → Annotation Processors)
3. Set Project SDK to Java 25
4. Enable preview features in compiler settings

**VS Code**:
1. Install "Extension Pack for Java"
2. Open the project folder
3. The extensions will auto-detect the Maven project

---

## Documentation

| Documentation                                  | Description                                    |
|------------------------------------------------|------------------------------------------------|
| [Docker](docs/docker.md)                       | Container deployment and configuration         |
| [Frontend](docs/frontend.md)                   | Web dashboard (React/TypeScript)               |
| [Game SDK](docs/game-sdk.md)                   | EngineClient, Orchestrator, GameRenderer       |
| [AI System](docs/ai.md)                        | Server-side game logic (formerly Game Masters) |
| [Module System](docs/module-system.md)         | Creating and deploying modules                 |
| [ClassLoader Isolation](docs/classloaders.md)  | Container runtime isolation                    |
| [Rendering Library](docs/rendering-library.md) | NanoVG/OpenGL GUI framework                    |
| [Architecture](docs/architecture.md)           | System design, project structure               |
| [API Reference](docs/api-reference.md)         | REST endpoints                                 |
| [Testing](docs/testing.md)                     | Headless testing, performance                  |
| [Performance](docs/performance.md)             | Performance notes + results                    |

## Next Steps

- [Game SDK](docs/game-sdk.md) - Build games with the client library
- [Architecture](docs/architecture.md) - Read the architecture docs
- [Module System](docs/module-system.md) - Create custom modules
- [API Reference](docs/api-reference.md) - Full REST API documentation

## Project Goals
- Have fun
- Maybe ship a game 

## Project Non-Goals
- Being simple  
- Being trendy  
- Protecting you from your own ideas
  
## License

MIT — Use this however you want (just not for evil tho)
