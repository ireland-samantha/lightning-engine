package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.ext.modules.domain.BoxCollider;
import ca.samanthaireland.lightning.engine.ext.modules.domain.CollisionHandlerConfig;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.BoxColliderRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Domain service for box collider operations.
 */
@Slf4j
public class BoxColliderService {

    private final BoxColliderRepository repository;
    private final List<Long> deleteQueue = new ArrayList<>();

    public BoxColliderService(BoxColliderRepository repository) {
        this.repository = repository;
    }

    /**
     * Attach a box collider to an entity.
     *
     * @param entityId the entity ID
     * @param width box width (must be positive)
     * @param height box height (must be positive)
     * @param depth box depth (must be positive)
     * @param offsetX offset from entity position X
     * @param offsetY offset from entity position Y
     * @param offsetZ offset from entity position Z
     * @param layer collision layer
     * @param mask collision mask
     * @param isTrigger whether this is a trigger collider
     * @throws IllegalArgumentException if dimensions are not positive
     */
    public void attachCollider(long entityId, float width, float height, float depth,
                               float offsetX, float offsetY, float offsetZ,
                               int layer, int mask, boolean isTrigger) {
        BoxCollider collider = BoxCollider.create(width, height, depth,
                offsetX, offsetY, offsetZ, layer, mask, isTrigger);
        repository.save(entityId, collider);

        log.info("Attached box collider to entity {}: size={}x{}, layer={}, mask={}",
                entityId, width, height, layer, mask);
    }

    /**
     * Attach a collision handler to an entity.
     *
     * @param entityId the entity ID (must have a box collider)
     * @param handlerType the handler type
     * @param param1 first handler parameter
     * @param param2 second handler parameter
     * @return true if handler was attached, false if entity has no collider
     */
    public boolean attachHandler(long entityId, int handlerType, float param1, float param2) {
        if (!repository.exists(entityId)) {
            log.warn("attachCollisionHandler: entity {} has no box collider", entityId);
            return false;
        }

        CollisionHandlerConfig config = CollisionHandlerConfig.create(handlerType, param1, param2);
        repository.saveHandlerConfig(entityId, config);

        log.info("Attached collision handler to entity {}: type={}, param1={}, param2={}",
                entityId, handlerType, param1, param2);
        return true;
    }

    /**
     * Set the size of a box collider.
     *
     * @param entityId the entity ID
     * @param width the new width
     * @param height the new height
     */
    public void setSize(long entityId, float width, float height) {
        repository.updateSize(entityId, width, height);
        log.debug("Set collider size for entity {}: {}x{}", entityId, width, height);
    }

    /**
     * Set the collision layer and mask.
     *
     * @param entityId the entity ID
     * @param layer the new layer
     * @param mask the new mask
     */
    public void setLayerMask(long entityId, int layer, int mask) {
        repository.updateLayerMask(entityId, layer, mask);
        log.debug("Set collision layer for entity {}: layer={}, mask={}", entityId, layer, mask);
    }

    /**
     * Queue a box collider for deletion.
     *
     * @param entityId the entity ID
     */
    public void queueDelete(long entityId) {
        deleteQueue.add(entityId);
        log.debug("Queued box collider deletion for entity {}", entityId);
    }

    /**
     * Process queued deletions.
     */
    public void processDeleteQueue() {
        for (Long entityId : deleteQueue) {
            repository.delete(entityId);
            log.debug("Cleaned up box collider for entity {}", entityId);
        }
        deleteQueue.clear();
    }

    /**
     * Find a box collider by entity ID.
     *
     * @param entityId the entity ID
     * @return the box collider if found
     */
    public Optional<BoxCollider> findByEntityId(long entityId) {
        return repository.findByEntityId(entityId);
    }

    /**
     * Check if an entity has a box collider.
     *
     * @param entityId the entity ID
     * @return true if the entity has a box collider
     */
    public boolean hasCollider(long entityId) {
        return repository.exists(entityId);
    }
}
