package ca.samanthaireland.lightning.engine.ext.modules.domain.repository;

import ca.samanthaireland.lightning.engine.ext.modules.domain.Entity;

import java.util.Optional;

/**
 * Repository interface for Entity entities.
 */
public interface EntityRepository {

    /**
     * Save an entity and return the saved instance with assigned ID.
     *
     * @param matchId the match to create the entity in
     * @param entity the entity to save
     * @return the saved entity with assigned ID
     */
    Entity save(long matchId, Entity entity);

    /**
     * Find an entity by its ID.
     *
     * @param entityId the entity ID
     * @return the entity if found
     */
    Optional<Entity> findById(long entityId);
}
