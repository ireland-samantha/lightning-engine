package ca.samanthaireland.lightning.engine.ext.modules.domain.repository;

import ca.samanthaireland.lightning.engine.ext.modules.domain.Position;

import java.util.Optional;

/**
 * Repository interface for entity positions.
 */
public interface PositionRepository {

    /**
     * Save the position of an entity.
     *
     * @param entityId the entity ID
     * @param position the position to save
     */
    void save(long entityId, Position position);

    /**
     * Find the position of an entity.
     *
     * @param entityId the entity ID
     * @return the position if the entity has grid position components
     */
    Optional<Position> findByEntityId(long entityId);
}
