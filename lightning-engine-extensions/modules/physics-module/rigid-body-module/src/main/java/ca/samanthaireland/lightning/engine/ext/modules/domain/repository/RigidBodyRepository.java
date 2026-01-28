package ca.samanthaireland.lightning.engine.ext.modules.domain.repository;

import ca.samanthaireland.lightning.engine.ext.modules.domain.RigidBody;

import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for RigidBody entities.
 *
 * <p>This interface combines CRUD operations with physics update operations
 * for backwards compatibility. For new code that only needs physics updates,
 * prefer using {@link RigidBodyPhysicsUpdater}.
 *
 * @see RigidBodyPhysicsUpdater
 */
public interface RigidBodyRepository extends RigidBodyPhysicsUpdater {

    /**
     * Save a rigid body's state to the ECS store.
     *
     * @param rigidBody the rigid body to save
     */
    void save(RigidBody rigidBody);

    /**
     * Find a rigid body by its entity ID.
     *
     * @param entityId the entity ID
     * @return the rigid body if found
     */
    Optional<RigidBody> findById(long entityId);

    /**
     * Get all entity IDs that have rigid body components.
     *
     * @return set of entity IDs
     */
    Set<Long> findAllIds();

    /**
     * Check if an entity has rigid body components.
     *
     * @param entityId the entity ID
     * @return true if the entity has rigid body components
     */
    boolean exists(long entityId);

    /**
     * Delete rigid body components from an entity.
     *
     * @param entityId the entity ID
     */
    void delete(long entityId);
}
