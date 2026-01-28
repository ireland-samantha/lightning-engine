package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.modules.domain.*;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.BoxColliderRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ca.samanthaireland.lightning.engine.ext.modules.domain.CollisionHandlerConfig.HANDLER_NONE;

/**
 * Domain service for handling collision events.
 */
@Slf4j
public class CollisionHandlerService {

    private final BoxColliderRepository repository;
    private final Map<Integer, CollisionHandler> collisionHandlers = new HashMap<>();

    public CollisionHandlerService(BoxColliderRepository repository) {
        this.repository = repository;
    }

    /**
     * Register a collision handler.
     *
     * @param handlerType the handler type ID (use values > 0)
     * @param handler the handler implementation
     */
    public void registerHandler(int handlerType, CollisionHandler handler) {
        collisionHandlers.put(handlerType, handler);
        log.info("Registered collision handler type {}", handlerType);
    }

    /**
     * Process collision events for the given collision pairs.
     *
     * @param context the module context
     * @param collisionPairs the collision pairs to process
     * @param currentTick the current tick
     * @return list of collision events that were processed
     */
    public List<CollisionEvent> processCollisions(ModuleContext context,
                                                   List<CollisionPair> collisionPairs,
                                                   long currentTick) {
        List<CollisionEvent> collisionEvents = new ArrayList<>();

        for (CollisionPair pair : collisionPairs) {
            long entityA = pair.entityA();
            long entityB = pair.entityB();
            CollisionInfo info = pair.info();

            // Get handler types for both entities
            int handlerTypeA = repository.getHandlerType(entityA);
            int handlerTypeB = repository.getHandlerType(entityB);

            // Skip if neither entity has a handler
            if (handlerTypeA == HANDLER_NONE && handlerTypeB == HANDLER_NONE) {
                continue;
            }

            // Check if this is a new collision (collision enter)
            long lastHandledA = repository.getLastHandledTick(entityA);
            long lastHandledB = repository.getLastHandledTick(entityB);

            boolean isNewCollisionA = lastHandledA < currentTick - 1;
            boolean isNewCollisionB = lastHandledB < currentTick - 1;

            // Invoke handler for entity A
            if (handlerTypeA != HANDLER_NONE) {
                CollisionHandler handlerA = collisionHandlers.get(handlerTypeA);
                if (handlerA != null) {
                    float param1A = repository.getHandlerParam1(entityA);
                    float param2A = repository.getHandlerParam2(entityA);
                    CollisionEvent eventA = new CollisionEvent(
                            entityA, entityB, info, param1A, param2A, handlerTypeA, currentTick);
                    handlerA.handle(context, eventA);
                    repository.updateHandledTick(entityA, currentTick);
                    collisionEvents.add(eventA);
                    log.debug("Invoked handler {} for entity {} (new={})",
                            handlerTypeA, entityA, isNewCollisionA);
                }
            }

            // Invoke handler for entity B
            if (handlerTypeB != HANDLER_NONE) {
                CollisionHandler handlerB = collisionHandlers.get(handlerTypeB);
                if (handlerB != null) {
                    float param1B = repository.getHandlerParam1(entityB);
                    float param2B = repository.getHandlerParam2(entityB);
                    // Pass inverted collision info for entity B
                    CollisionEvent eventB = new CollisionEvent(
                            entityB, entityA, info.inverted(), param1B, param2B, handlerTypeB, currentTick);
                    handlerB.handle(context, eventB);
                    repository.updateHandledTick(entityB, currentTick);
                    collisionEvents.add(eventB);
                    log.debug("Invoked handler {} for entity {} (new={})",
                            handlerTypeB, entityB, isNewCollisionB);
                }
            }
        }

        if (!collisionEvents.isEmpty()) {
            log.debug("Processed {} collision events", collisionEvents.size());
        }

        return collisionEvents;
    }

    /**
     * Get registered collision handlers map.
     *
     * @return unmodifiable view of collision handlers
     */
    public Map<Integer, CollisionHandler> getHandlers() {
        return Map.copyOf(collisionHandlers);
    }
}
