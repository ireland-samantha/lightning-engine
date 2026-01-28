package ca.samanthaireland.lightning.engine.ext.modules.domain.repository;

import ca.samanthaireland.lightning.engine.ext.modules.domain.Projectile;

import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for Projectile entities.
 */
public interface ProjectileRepository {

    /**
     * Save a projectile and return the saved instance with assigned ID.
     *
     * @param matchId the match to create the projectile in
     * @param projectile the projectile to save
     * @return the saved projectile with assigned ID
     */
    Projectile save(long matchId, Projectile projectile);

    /**
     * Update an existing projectile.
     *
     * @param projectile the projectile with updated values
     */
    void update(Projectile projectile);

    /**
     * Find a projectile by its entity ID.
     *
     * @param projectileId the projectile entity ID
     * @return the projectile if found
     */
    Optional<Projectile> findById(long projectileId);

    /**
     * Find all projectile entity IDs.
     *
     * @return set of all projectile entity IDs
     */
    Set<Long> findAllIds();

    /**
     * Delete a projectile by removing all its components.
     *
     * @param projectileId the projectile entity ID to delete
     */
    void delete(long projectileId);

    /**
     * Mark a projectile for pending destruction.
     *
     * @param projectileId the projectile entity ID
     */
    void markPendingDestroy(long projectileId);

    /**
     * Check if a projectile entity exists.
     *
     * @param projectileId the projectile entity ID
     * @return true if the projectile exists
     */
    boolean exists(long projectileId);
}
