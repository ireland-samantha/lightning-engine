package ca.samanthaireland.lightning.engine.ext.modules;

import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.system.EngineSystem;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.modules.domain.CollisionEvent;
import ca.samanthaireland.lightning.engine.ext.modules.domain.CollisionHandler;
import ca.samanthaireland.lightning.engine.ext.modules.domain.CollisionPair;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.BoxColliderRepository;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.BoxColliderService;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.CollisionDetectionService;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.CollisionHandlerService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.*;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.repository.EcsBoxColliderRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static ca.samanthaireland.lightning.engine.ext.modules.BoxColliderModuleFactory.ALL_COMPONENTS;
import static ca.samanthaireland.lightning.engine.ext.modules.BoxColliderModuleFactory.FLAG;

/**
 * BoxCollider module implementation.
 *
 * <p>Provides axis-aligned bounding box (AABB) collision detection using the
 * domain/repository pattern for clean separation of concerns.
 */
@Slf4j
public class BoxColliderModule implements EngineModule {
    private final ModuleContext context;
    private final BoxColliderService boxColliderService;
    private final CollisionDetectionService collisionDetectionService;
    private final CollisionHandlerService collisionHandlerService;

    // Collision pairs detected this tick (for event processing)
    private final List<CollisionPair> collisionPairs = new ArrayList<>();

    // Collision events emitted this tick
    private final List<CollisionEvent> collisionEvents = new ArrayList<>();

    // Tick counter for collision enter detection
    private long currentTick = 0;

    public BoxColliderModule(ModuleContext context) {
        this.context = context;

        BoxColliderRepository repository = new EcsBoxColliderRepository(
                context.getEntityComponentStore()
        );

        this.boxColliderService = new BoxColliderService(repository);
        this.collisionDetectionService = new CollisionDetectionService(repository);
        this.collisionHandlerService = new CollisionHandlerService(repository);
    }

    /**
     * Constructor for testing with injected services.
     */
    BoxColliderModule(ModuleContext context,
                      BoxColliderService boxColliderService,
                      CollisionDetectionService collisionDetectionService,
                      CollisionHandlerService collisionHandlerService) {
        this.context = context;
        this.boxColliderService = boxColliderService;
        this.collisionDetectionService = collisionDetectionService;
        this.collisionHandlerService = collisionHandlerService;
    }

    /**
     * Register a collision handler.
     *
     * @param handlerType the handler type ID (use values > 0)
     * @param handler the handler implementation
     */
    public void registerCollisionHandler(int handlerType, CollisionHandler handler) {
        collisionHandlerService.registerHandler(handlerType, handler);
    }

    /**
     * Get collision events from the last tick.
     */
    public List<CollisionEvent> getCollisionEvents() {
        return new ArrayList<>(collisionEvents);
    }

    /**
     * Get collision pairs detected in the last tick.
     * Useful for game logic that needs to respond to collisions.
     */
    public List<CollisionPair> getCollisionPairs() {
        return new ArrayList<>(collisionPairs);
    }

    @Override
    public List<EngineSystem> createSystems() {
        return List.of(
                createCollisionDetectionSystem(),
                createHandleCollisionSystem(),
                createCleanupSystem()
        );
    }

    @Override
    public List<EngineCommand> createCommands() {
        return List.of(
                AttachBoxColliderCommand.create(boxColliderService),
                AttachCollisionHandlerCommand.create(boxColliderService),
                SetColliderSizeCommand.create(boxColliderService),
                SetCollisionLayerCommand.create(boxColliderService),
                DeleteBoxColliderCommand.create(boxColliderService)
        );
    }

    @Override
    public List<BaseComponent> createComponents() {
        return ALL_COMPONENTS;
    }

    @Override
    public BaseComponent createFlagComponent() {
        return FLAG;
    }

    @Override
    public String getName() {
        return "BoxColliderModule";
    }

    // ========== Systems ==========

    /**
     * Collision detection system using O(n^2) AABB checks.
     */
    private EngineSystem createCollisionDetectionSystem() {
        return () -> {
            currentTick++;
            collisionPairs.clear();
            collisionEvents.clear();

            List<CollisionPair> detected = collisionDetectionService.detectCollisions(currentTick);
            collisionPairs.addAll(detected);
        };
    }

    /**
     * Handle collision system - processes collision events and invokes handlers.
     */
    private EngineSystem createHandleCollisionSystem() {
        return () -> {
            List<CollisionEvent> events = collisionHandlerService.processCollisions(
                    context, collisionPairs, currentTick);
            collisionEvents.addAll(events);
        };
    }

    /**
     * Cleanup system for deleted colliders.
     */
    private EngineSystem createCleanupSystem() {
        return () -> boxColliderService.processDeleteQueue();
    }
}
