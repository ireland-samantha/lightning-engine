# CLAUDE.md

# Lightning Engine - Claude Code Guidelines

A production-grade multi-match game server framework built on Java 25 and Quarkus, featuring ECS architecture, hot-reloadable modules, and real-time WebSocket streaming.

## Project Overview

**Lightning Engine** is an experimental game server that enables:
- Multi-game execution on a single JVM via isolated containers with separate ClassLoaders
- Tick-based simulation with independent game loops per container
- Entity-Component-System (ECS) for entity management with O(1) component access
- Hot-reloadable modules for runtime game logic updates
- WebSocket streaming of ECS snapshots (full or delta-compressed)
- REST API for resource, match, and container management
- React-based admin dashboard

## Technology Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Java 25 (with preview features, virtual threads) |
| **Build** | Maven 3.9+ (multi-module) |
| **Backend** | Quarkus 3.x (REST + WebSocket) |
| **Frontend** | React 18+ with TypeScript, Material-UI |
| **Persistence** | MongoDB 6.0+ |
| **Testing** | JUnit 5, Mockito, Playwright, Testcontainers |
| **Auth** | JWT + BCrypt (role-based access control) |

## Core Principles

1. **Separation of Concerns (SoC)**: Each module has one clear responsibility. Don't mix HTTP handling with business logic, or persistence with domain rules.

2. **Single Responsibility Principle (SRP)**: A module should have only one reason to change. Split modules that do too much.

3. **Dependency Injection (DI)**: All dependencies are injected, not instantiated internally. This enables testing and swapping implementations.

4. **Depend on Abstractions**: Depend on contracts/interfaces, not implementations.

5. **Clean Architecture Layers**:
   - **Core** (`engine-core/`): Pure domain abstractions, no framework dependencies
   - **Implementation** (`engine-internal/`): Production implementations of core interfaces
   - **Adapters** (`engine-adapter/`): Framework-specific adapters (REST clients, SDKs)
   - **Providers** (`webservice/`): Quarkus REST/WebSocket endpoints and MongoDB persistence

6. **API-First Design**: Define contracts before implementation

## Project Structure

```
lightning-engine/
├── pom.xml                           # Root Maven POM
├── CLAUDE.md                         # Development guidelines
├── openapi.yaml                      # OpenAPI 3.0 specification
├── docker-compose.yml                # Local development infrastructure
├── build.sh                          # Build automation
│
├── utils/                            # Shared utilities
│
├── auth/                             # Authentication module
│   └── JWT, BCrypt, RBAC
│
├── lightning-engine/                 # Core engine modules
│   ├── engine-core/                  # Core abstractions (NO framework deps)
│   │   ├── command/                  # Command queue system
│   │   ├── container/                # ExecutionContainer interface
│   │   ├── entity/                   # ECS domain models
│   │   ├── exception/                # Custom exception hierarchy
│   │   ├── match/                    # Match representation
│   │   ├── resources/                # Resource management
│   │   ├── session/                  # Player session tracking
│   │   ├── snapshot/                 # ECS state serialization
│   │   ├── store/                    # Abstract component store
│   │   └── system/                   # ECS system registration
│   │
│   ├── engine-internal/              # Implementation layer
│   │   ├── container/                # InMemoryExecutionContainer
│   │   ├── store/                    # Array-based ECS implementation
│   │   ├── match/                    # Match service impl
│   │   ├── snapshot/                 # Snapshot compression
│   │   └── session/                  # Session service impl
│   │
│   ├── engine-adapter/               # Adapters & SDKs
│   │   ├── web-api-adapter/          # REST client library
│   │   └── game-sdk/                 # Game development SDK
│   │
│   ├── rendering-core/               # NanoVG-based GUI framework
│   │
│   └── webservice/
│       └── quarkus-web-api/          # Main Quarkus application
│           ├── api/
│           │   ├── rest/             # REST endpoints
│           │   ├── websocket/        # WebSocket snapshot streaming
│           │   ├── auth/             # JWT authentication
│           │   ├── config/           # Quarkus configuration
│           │   ├── dto/              # Request/response DTOs
│           │   ├── error/            # Exception mappers
│           │   └── persistence/      # MongoDB integration
│           │
│           └── src/main/frontend/    # React admin dashboard
│
└── lightning-engine-extensions/      # Game-specific modules
    ├── modules/                      # Pluggable game modules
    │   ├── entity-module/            # Entity spawning & lifecycle
    │   ├── grid-map-module/          # Grid-based map system
    │   ├── rigid-body-module/        # Physics simulation
    │   ├── box-collider-module/      # Collision detection
    │   ├── health-module/            # Health & damage
    │   ├── items-module/             # Item management
    │   └── move-module/              # Entity movement
    │
    └── game-masters/                 # AI backend integrations
```

## Architecture Patterns

### Fluent API Pattern

ExecutionContainer uses fluent operation accessors:
```java
container.lifecycle().start();
container.ticks().advance();
container.commands().queue(cmd);
container.modules().install(jar);
container.ai().enable(name);
container.snapshots().restore(data);
container.sessions().create(player);
container.resources().upload(resource);
```

### Module System

Modules implement `ModuleFactory` interface and are hot-loadable:
```java
public interface ModuleFactory {
    String getName();
    void initialize(ModuleContext context);
    List<ComponentDefinition> getComponents();
    List<CommandHandler<?>> getCommandHandlers();
    List<GameSystem> getSystems();
}
```

Each module defines:
- ECS components (fields in columnar store)
- Command handlers
- System logic (ticked during match execution)
- Configuration

### ECS (Entity Component System)

Array-based columnar storage for O(1) component access:
```java
// Component access
float posX = store.getFloat(entityId, "POSITION_X");
store.setFloat(entityId, "POSITION_X", newPosX);

// Snapshot serialization
{
  "matchId": 1,
  "tick": 42,
  "data": {
    "EntityModule": {
      "POSITION_X": [100, 200, 300],
      "POSITION_Y": [50, 60, 70]
    },
    "HealthModule": {
      "HEALTH": [100, 80, 50]
    }
  }
}
```

### Execution Containers

Each container provides:
- Isolated `ContainerClassLoader` for module JARs
- Separate `EntityComponentStore` (ECS data)
- Independent `GameLoop` with configurable tick rate
- Container-scoped `CommandQueue`
- Registry of enabled modules per match
- WebSocket session tracking

## Development Approach: API-First (Autonomous Mode)

When building new features, follow API-first design principles autonomously. Do not wait for approval at each step—execute the full implementation unless I intervene.

### Autonomous Workflow

1. **Design the contract first** - Define interfaces with full Javadoc
2. **Define DTOs and domain models** - Request/response records, validation annotations
3. **Define exceptions** - Custom exceptions for all failure modes
4. **Implement** - Build all layers top-to-bottom
5. **Write tests** - Unit tests, integration tests
6. **Run build** - `./build.sh all` must pass before declaring done

### When I Intervene

If I say "stop", "wait", "hold on", or express concern about direction:
- Immediately stop what you're doing
- Do not commit or continue the current approach
- Explain your current plan and reasoning
- Wait for my feedback before proceeding

### Handling Ambiguity

When requirements are unclear:
- Make a reasonable assumption and document it
- Prefer the simpler solution
- Flag the assumption in your response so I can correct if needed

### Decision Documentation

When making non-obvious architectural decisions, document the rationale:
- In code: `// Decision: Using X because Y`
- In commits: Note trade-offs considered

## Java Conventions

### General

- **Java 25** with preview features (virtual threads, pattern matching, records)
- Prefer immutability - use `final` fields, return unmodifiable collections
- Use `Optional` for nullable return values, never for parameters
- Avoid null - use `Optional`, empty collections, or throw exceptions

### Modern Java Features

```java
// Records for immutable data
public record ContainerId(UUID value) {
    public ContainerId {
        Objects.requireNonNull(value, "ContainerId cannot be null");
    }

    public static ContainerId generate() {
        return new ContainerId(UUID.randomUUID());
    }
}

// Sealed classes for restricted hierarchies
public sealed interface CommandResult permits Success, Failure {
    boolean isSuccess();
}

// Pattern matching with switch
public String handleResult(CommandResult result) {
    return switch (result) {
        case Success s -> "Command executed at tick " + s.tick();
        case Failure f -> "Command failed: " + f.reason();
    };
}

// Virtual threads
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    var futures = containerIds.stream()
        .map(id -> executor.submit(() -> fetchContainer(id)))
        .toList();
}
```

### DTOs and Records

```java
// Request DTO with validation
public record CreateMatchRequest(
    @NotBlank String name,
    @NotNull @Size(min = 1) List<String> enabledModules,
    @Min(1) @Max(128) int tickRate
) {}

// Response DTO with factory method
public record MatchResponse(
    UUID id,
    String name,
    MatchStatus status,
    int currentTick,
    Instant createdAt
) {
    public static MatchResponse from(Match match) {
        return new MatchResponse(
            match.getId().value(),
            match.getName(),
            match.getStatus(),
            match.getCurrentTick(),
            match.getCreatedAt()
        );
    }
}
```

## Quarkus Specifics

### Decoupling from Quarkus

- All services MUST have a corresponding interface in `engine-core`
- Interface methods should have no framework-specific annotations
- Quarkus annotations (`@ApplicationScoped`, `@Transactional`, `@Inject`) on implementation classes only
```java
// Interface in engine-core - no Quarkus annotations
public interface ContainerService {
    /**
     * Creates a new execution container.
     *
     * @param name the container name
     * @return the created container
     * @throws DuplicateContainerException if name already exists
     */
    ExecutionContainer create(String name);
}

// Implementation in engine-internal - Quarkus annotations here
@ApplicationScoped
public class ContainerServiceImpl implements ContainerService {
    private final ContainerRepository containerRepository;

    @Inject
    public ContainerServiceImpl(ContainerRepository containerRepository) {
        this.containerRepository = containerRepository;
    }

    @Override
    public ExecutionContainer create(String name) {
        // implementation
    }
}
```

### REST Resources (Quarkus JAX-RS)

REST resources live in `webservice/quarkus-web-api/src/main/java/.../api/rest/`:
```java
@Path("/api/containers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContainerResource {
    private final ContainerService containerService;

    @Inject
    public ContainerResource(ContainerService containerService) {
        this.containerService = containerService;
    }

    @POST
    public Response create(@Valid CreateContainerRequest request) {
        ExecutionContainer container = containerService.create(request.name());
        return Response.created(URI.create("/api/containers/" + container.getId().value()))
            .entity(ContainerResponse.from(container))
            .build();
    }

    @GET
    @Path("/{id}")
    public ContainerResponse getById(@PathParam("id") UUID id) {
        return containerService.findById(new ContainerId(id))
            .map(ContainerResponse::from)
            .orElseThrow(() -> new ContainerNotFoundException(new ContainerId(id)));
    }
}
```

### WebSocket Endpoints

```java
@ServerEndpoint("/ws/snapshots/{matchId}")
public class SnapshotWebSocket {
    @Inject
    SnapshotService snapshotService;

    @OnOpen
    public void onOpen(Session session, @PathParam("matchId") UUID matchId) {
        snapshotService.subscribe(new MatchId(matchId), session);
    }

    @OnClose
    public void onClose(Session session, @PathParam("matchId") UUID matchId) {
        snapshotService.unsubscribe(new MatchId(matchId), session);
    }
}
```

### MongoDB Persistence

Repository pattern with MongoDB (using Panache or raw driver):
```java
// Domain repository interface - no MongoDB dependency
public interface MatchRepository {
    Match save(Match match);
    Optional<Match> findById(MatchId id);
    List<Match> findByContainerId(ContainerId containerId);
    void delete(MatchId id);
}

// MongoDB implementation
@ApplicationScoped
public class MongoMatchRepository implements MatchRepository {
    @Inject
    MongoClient mongoClient;

    private MongoCollection<Document> collection() {
        return mongoClient.getDatabase("lightning")
            .getCollection("matches");
    }

    @Override
    public Match save(Match match) {
        Document doc = MatchMapper.toDocument(match);
        collection().replaceOne(
            Filters.eq("_id", match.getId().value().toString()),
            doc,
            new ReplaceOptions().upsert(true)
        );
        return match;
    }

    @Override
    public Optional<Match> findById(MatchId id) {
        Document doc = collection().find(
            Filters.eq("_id", id.value().toString())
        ).first();
        return Optional.ofNullable(doc).map(MatchMapper::fromDocument);
    }
}
```

### Exception Handling

```java
// Base exception
public abstract class EngineException extends RuntimeException {
    private final String code;

    protected EngineException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}

// Domain-specific exceptions
public class ContainerNotFoundException extends EngineException {
    public ContainerNotFoundException(ContainerId id) {
        super("CONTAINER_NOT_FOUND", "Container not found: " + id.value());
    }
}

public class InvalidContainerStateException extends EngineException {
    public InvalidContainerStateException(ContainerId id, ContainerState current, ContainerState expected) {
        super("INVALID_CONTAINER_STATE",
            String.format("Container %s is in state %s, expected %s", id.value(), current, expected));
    }
}

// Quarkus exception mapper
@Provider
public class EngineExceptionMapper implements ExceptionMapper<EngineException> {
    @Override
    public Response toResponse(EngineException ex) {
        Response.Status status = switch (ex) {
            case ContainerNotFoundException _ -> Response.Status.NOT_FOUND;
            case InvalidContainerStateException _ -> Response.Status.CONFLICT;
            default -> Response.Status.INTERNAL_SERVER_ERROR;
        };

        return Response.status(status)
            .entity(new ErrorResponse(ex.getCode(), ex.getMessage(), Instant.now()))
            .build();
    }
}
```

### Plugin Configuration

```java
// Plugin interface in engine-core
public interface AiBackend {
    AiResponse complete(AiRequest request);
}

// OpenAI implementation - activated by config
@ApplicationScoped
@IfBuildProperty(name = "ai.backend", stringValue = "openai")
public class OpenAiBackend implements AiBackend {
    // implementation
}

// Anthropic implementation
@ApplicationScoped
@IfBuildProperty(name = "ai.backend", stringValue = "anthropic")
public class AnthropicBackend implements AiBackend {
    // implementation
}
```

## Naming Conventions

**Domain Models:**
- Domain model: `Match`, `ExecutionContainer` (in `engine-core/{component}/`)
- Strongly-typed ID: `MatchId`, `ContainerId` (in `engine-core/{component}/`)

**Services:**
- Service interface: `MatchService` (in `engine-core/{component}/`)
- Service implementation: `MatchServiceImpl` (in `engine-internal/{component}/`)

**Repositories:**
- Repository interface: `MatchRepository` (in `engine-core/{component}/`)
- MongoDB implementation: `MongoMatchRepository` (in `webservice/quarkus-web-api/.../persistence/`)

**DTOs:**
- Request: `CreateMatchRequest`, `UpdateMatchRequest` (in `webservice/.../dto/`)
- Response: `MatchResponse` (in `webservice/.../dto/`)

**REST Resources:**
- Resource: `MatchResource` (in `webservice/.../api/rest/`)

**Exceptions:**
- Not found: `MatchNotFoundException` (in `engine-core/{component}/exception/`)
- Invalid state: `InvalidMatchStateException` (in `engine-core/{component}/exception/`)

**Modules:**
- Module factory: `EntityModule` (in `lightning-engine-extensions/modules/{module-name}/`)
- ECS component: `EntityComponent` (in same module)

## Testing

### Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class MatchServiceImplTest {
    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private MatchServiceImpl matchService;

    @Test
    void create_withValidMatch_returnsCreatedMatch() {
        // Arrange
        Match match = MatchFixtures.pendingMatch();
        when(matchRepository.save(any())).thenReturn(match);

        // Act
        Match result = matchService.create(match);

        // Assert
        assertThat(result).isEqualTo(match);
        verify(matchRepository).save(match);
    }
}
```

### REST Resource Tests (Quarkus)
```java
@QuarkusTest
class ContainerResourceTest {
    @InjectMock
    ContainerService containerService;

    @Test
    void create_withValidRequest_returns201() {
        ExecutionContainer container = ContainerFixtures.running();
        when(containerService.create(any())).thenReturn(container);

        given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "test-container"}
                """)
        .when()
            .post("/api/containers")
        .then()
            .statusCode(201)
            .header("Location", containsString("/api/containers/"))
            .body("name", equalTo("test-container"));
    }
}
```

### Integration Tests
```java
@QuarkusTest
@TestProfile(MongoTestProfile.class)
class MongoMatchRepositoryIntegrationTest {
    @Inject
    MatchRepository matchRepository;

    @Test
    void save_persistsMatch() {
        Match match = MatchFixtures.pendingMatch();

        Match saved = matchRepository.save(match);
        Optional<Match> found = matchRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo(match.getName());
    }
}
```

### E2E Tests (Playwright
Located in `webservice/playwright-test/` for browser automation of the admin dashboard.

## Build Commands

```bash
./build.sh build              # Build all modules (skip tests)
./build.sh test               # Run unit tests
./build.sh docker             # Build Docker image
./build.sh integration-test   # Run integration tests with Docker
./build.sh all                # Full pipeline
```

**Development:**
```bash
mvn quarkus:dev               # Backend dev mode (port 8080)
npm run dev                   # Frontend dev server (port 5173)
```

**Admin Dashboard:** `http://localhost:8080/admin/dashboard`

## REST API Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET/POST /api/containers` | Container lifecycle |
| `GET/POST /api/containers/{id}/matches` | Match management |
| `POST /api/containers/{id}/modules` | Module installation |
| `POST /api/containers/{id}/ticks` | Tick control |
| `POST /api/containers/{id}/commands` | Command queueing |
| `GET/POST /api/resources` | Resource upload/download |
| `WS /ws/snapshots/{matchId}` | WebSocket snapshot streaming |

## Extension Points

Design these as pluggable from the start:

- **AI Backend**: OpenAI, Anthropic, Ollama, etc. (in `game-masters/`)
- **Persistence**: MongoDB (current), could support others
- **Authentication**: JWT (current), API key, OAuth
- **Snapshot Transport**: WebSocket (current), could support SSE, polling

## Quality Gates (Self-Enforced)

Before declaring a feature complete, verify:

- [ ] Interface has complete Javadoc
- [ ] DTOs use Java records with validation annotations
- [ ] Custom exceptions for all failure cases
- [ ] All layers implemented (resource -> service -> repository)
- [ ] New extension points are interface-based and pluggable
- [ ] Domain models encapsulate business logic
- [ ] Strongly-typed IDs for all entitieshttps://github.com/ireland-samantha/lightning-engine/tree/main
- [ ] Unit tests for all classes (>80% coverage)
- [ ] Integration tests with Testcontainers (MongoDB)
- [ ] `./build.sh all` passes
- [ ] No security vulnerabilities (secrets, injection, etc.)

## Code Quality Philosophy

- **Quality over speed**: Prioritize quality and thoughtful design over speed of iteration
- **No deprecation**: Never deprecate methods or fields. Apply the full migration/fix even if it's breaking.
- **Complete refactoring**: When making architectural changes, updatpush
- L affected code across all layers.
- **Clean breaks**: If an API or interface needs to change, change it completely.
- **Boy scout rule**: Leave code better than you found it.
- **YAGNI**: Don't build features you don't need yet.

## Git and Version Control

### Commit Messages

Use conventional commits:
```
type(scope): subject

body

footer
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

### Branch Strategy

- `main`: Production-ready code
- `feature/*`: New features
- `fix/*`: Bug fixes
