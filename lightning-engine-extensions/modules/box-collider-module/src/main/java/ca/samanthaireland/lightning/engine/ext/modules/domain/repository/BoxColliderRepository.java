package ca.samanthaireland.lightning.engine.ext.modules.domain.repository;

import ca.samanthaireland.lightning.engine.ext.modules.domain.BoxCollider;

import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for BoxCollider entities.
 *
 * <p>This interface combines CRUD operations with specialized update and state
 * management operations for backwards compatibility. For new code, prefer using
 * the focused interfaces:
 * <ul>
 *   <li>{@link ColliderPropertyUpdater} - for property updates</li>
 *   <li>{@link CollisionStateManager} - for collision state management</li>
 * </ul>
 *
 * @see ColliderPropertyUpdater
 * @see CollisionStateManager
 */
public interface BoxColliderRepository extends ColliderPropertyUpdater, CollisionStateManager {

    /**
     * Save a box collider to an entity.
     *
     * @param entityId the entity ID
     * @param collider the box collider to save
     */
    void save(long entityId, BoxCollider collider);

    /**
     * Find a box collider by entity ID.
     *
     * @param entityId the entity ID
     * @return the box collider if found
     */
    Optional<BoxCollider> findByEntityId(long entityId);

    /**
     * Check if an entity has a box collider.
     *
     * @param entityId the entity ID
     * @return true if the entity has a box collider
     */
    boolean exists(long entityId);

    /**
     * Get all entities with box colliders.
     *
     * @return set of entity IDs with box colliders
     */
    Set<Long> findAllColliderEntities();

    /**
     * Delete a box collider from an entity.
     *
     * @param entityId the entity ID
     */
    void delete(long entityId);
}
