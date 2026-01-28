package ca.samanthaireland.lightning.engine.ext.modules.domain.repository;

import ca.samanthaireland.lightning.engine.ext.modules.domain.MovementState;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Position;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Velocity;

import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for movement data.
 *
 * @deprecated use RigidBodyModule
 */
@Deprecated
public interface MovementRepository {

    /**
     * Attach movement to an entity with the given position and velocity.
     *
     * @param entityId the entity ID
     * @param position the initial position
     * @param velocity the initial velocity
     */
    void save(long entityId, Position position, Velocity velocity);

    /**
     * Find the movement state of an entity.
     *
     * @param entityId the entity ID
     * @return the movement state if the entity has movement components
     */
    Optional<MovementState> findById(long entityId);

    /**
     * Get all entities that have movement components.
     *
     * @return set of entity IDs with movement components
     */
    Set<Long> findAllMoveable();

    /**
     * Update the position of an entity.
     *
     * @param entityId the entity ID
     * @param position the new position
     */
    void updatePosition(long entityId, Position position);

    /**
     * Remove movement components from an entity.
     *
     * @param entityId the entity ID
     */
    void delete(long entityId);

    /**
     * Check if an entity has movement components.
     *
     * @param entityId the entity ID
     * @return true if the entity has movement components
     */
    boolean exists(long entityId);
}
