package ca.samanthaireland.lightning.engine.ext.modules.domain.repository;

import ca.samanthaireland.lightning.engine.ext.modules.domain.Health;

import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for entity health data.
 */
public interface HealthRepository {

    /**
     * Attach health to an entity.
     *
     * @param entityId the entity ID
     * @param health the initial health state
     */
    void save(long entityId, Health health);

    /**
     * Find the health state of an entity.
     *
     * @param entityId the entity ID
     * @return the health state if the entity has health attached
     */
    Optional<Health> findByEntityId(long entityId);

    /**
     * Check if an entity has health attached.
     *
     * @param entityId the entity ID
     * @return true if the entity has health
     */
    boolean hasHealth(long entityId);

    /**
     * Get all entity IDs that have health attached.
     *
     * @return set of entity IDs with health
     */
    Set<Long> findAllEntityIds();
}
