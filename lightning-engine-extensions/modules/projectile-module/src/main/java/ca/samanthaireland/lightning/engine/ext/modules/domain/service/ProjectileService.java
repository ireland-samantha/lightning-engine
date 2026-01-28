package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.ext.modules.domain.Projectile;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.ProjectileRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Domain service for projectile operations.
 */
@Slf4j
public class ProjectileService {

    private final ProjectileRepository projectileRepository;
    private final List<Long> destroyQueue = new ArrayList<>();

    public ProjectileService(ProjectileRepository projectileRepository) {
        this.projectileRepository = projectileRepository;
    }

    /**
     * Spawn a new projectile in the given match.
     *
     * @param matchId the match to spawn the projectile in
     * @param ownerEntityId the entity that fired the projectile
     * @param positionX starting X position
     * @param positionY starting Y position
     * @param directionX direction X component
     * @param directionY direction Y component
     * @param speed projectile speed
     * @param damage damage dealt on hit
     * @param lifetime ticks until auto-destroy (0 = no limit)
     * @param pierceCount number of targets to pierce
     * @param projectileType type identifier
     * @return the created projectile with assigned ID
     * @throws IllegalArgumentException if parameters are invalid
     */
    public Projectile spawnProjectile(
            long matchId,
            long ownerEntityId,
            float positionX,
            float positionY,
            float directionX,
            float directionY,
            float speed,
            float damage,
            float lifetime,
            float pierceCount,
            float projectileType
    ) {
        Projectile projectile = Projectile.create(
                ownerEntityId,
                positionX,
                positionY,
                directionX,
                directionY,
                speed,
                damage,
                lifetime,
                pierceCount,
                projectileType
        );

        Projectile saved = projectileRepository.save(matchId, projectile);

        log.info("Spawned projectile {} at ({}, {}) dir=({}, {}) speed={} damage={}",
                saved.id(), saved.positionX(), saved.positionY(),
                saved.directionX(), saved.directionY(), saved.speed(), saved.damage());

        return saved;
    }

    /**
     * Mark a projectile for destruction.
     *
     * @param projectileId the projectile entity ID
     */
    public void destroyProjectile(long projectileId) {
        if (projectileId == 0) {
            log.warn("destroyProjectile: missing entityId");
            return;
        }

        projectileRepository.markPendingDestroy(projectileId);
        log.debug("Marked projectile {} for destruction", projectileId);
    }

    /**
     * Find a projectile by ID.
     *
     * @param projectileId the projectile entity ID
     * @return the projectile if found
     */
    public Optional<Projectile> findById(long projectileId) {
        return projectileRepository.findById(projectileId);
    }

    /**
     * Process movement for all active projectiles.
     */
    public void processMovement() {
        Set<Long> projectileIds = projectileRepository.findAllIds();

        for (Long entityId : projectileIds) {
            Optional<Projectile> optProjectile = projectileRepository.findById(entityId);
            if (optProjectile.isEmpty()) continue;

            Projectile projectile = optProjectile.get();
            if (projectile.pendingDestroy()) continue;
            if (projectile.speed() <= 0) continue;

            Projectile moved = projectile.withMovement();
            projectileRepository.update(moved);

            log.trace("Projectile {} moved to ({}, {})", entityId, moved.positionX(), moved.positionY());
        }
    }

    /**
     * Process lifetime for all projectiles, queuing expired ones for destruction.
     */
    public void processLifetime() {
        Set<Long> projectileIds = projectileRepository.findAllIds();

        for (Long entityId : projectileIds) {
            Optional<Projectile> optProjectile = projectileRepository.findById(entityId);
            if (optProjectile.isEmpty()) continue;

            Projectile projectile = optProjectile.get();

            if (projectile.pendingDestroy()) {
                destroyQueue.add(entityId);
                continue;
            }

            if (projectile.lifetime() <= 0) continue; // No lifetime limit

            Projectile ticked = projectile.withTick();
            projectileRepository.update(ticked);

            if (ticked.hasExpired()) {
                log.debug("Projectile {} expired after {} ticks", entityId, ticked.ticksAlive());
                destroyQueue.add(entityId);
            }
        }
    }

    /**
     * Clean up all projectiles queued for destruction.
     */
    public void processCleanup() {
        if (destroyQueue.isEmpty()) return;

        for (Long entityId : destroyQueue) {
            projectileRepository.delete(entityId);
            log.debug("Cleaned up projectile {}", entityId);
        }

        destroyQueue.clear();
    }

    /**
     * Get the current destroy queue size (for testing).
     *
     * @return number of projectiles queued for destruction
     */
    public int getDestroyQueueSize() {
        return destroyQueue.size();
    }
}
