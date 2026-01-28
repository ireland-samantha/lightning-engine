package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.ext.modules.domain.*;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.BoxColliderRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Domain service for collision detection operations.
 */
@Slf4j
public class CollisionDetectionService {

    private final BoxColliderRepository repository;

    public CollisionDetectionService(BoxColliderRepository repository) {
        this.repository = repository;
    }

    /**
     * Detect all collisions between entities with box colliders.
     *
     * @param currentTick the current tick for collision tracking
     * @return list of collision pairs detected
     */
    public List<CollisionPair> detectCollisions(long currentTick) {
        Set<Long> entities = repository.findAllColliderEntities();
        List<CollisionPair> collisionPairs = new ArrayList<>();

        // Reset collision state for all entities
        for (long entity : entities) {
            repository.resetCollisionState(entity);
        }

        // Convert to list for O(n^2) iteration
        List<Long> entityList = new ArrayList<>(entities);
        int n = entityList.size();

        // Check all pairs
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                long entityA = entityList.get(i);
                long entityB = entityList.get(j);

                // Check layer filtering
                if (!canCollide(entityA, entityB)) {
                    continue;
                }

                // Get AABBs
                AABB boxA = getAABB(entityA);
                AABB boxB = getAABB(entityB);

                // Test intersection
                if (boxA.intersects(boxB)) {
                    // Calculate penetration and normal
                    CollisionInfo info = calculateCollisionInfo(boxA, boxB);

                    // Update collision state for both entities
                    updateCollisionState(entityA, entityB, info);
                    updateCollisionState(entityB, entityA, info.inverted());

                    // Record collision pair
                    collisionPairs.add(new CollisionPair(entityA, entityB, info));

                    log.debug("Collision detected: entity {} <-> entity {} (depth={})",
                            entityA, entityB, info.penetrationDepth());
                }
            }
        }

        if (!collisionPairs.isEmpty()) {
            log.debug("Detected {} collision pairs", collisionPairs.size());
        }

        return collisionPairs;
    }

    /**
     * Check if two entities can collide based on layer/mask.
     */
    public boolean canCollide(long entityA, long entityB) {
        Optional<BoxCollider> colliderA = repository.findByEntityId(entityA);
        Optional<BoxCollider> colliderB = repository.findByEntityId(entityB);

        if (colliderA.isEmpty() || colliderB.isEmpty()) {
            return false;
        }

        int layerA = colliderA.get().layer();
        int maskA = colliderA.get().mask();
        int layerB = colliderB.get().layer();
        int maskB = colliderB.get().mask();

        // A can hit B if A's mask includes B's layer, and vice versa
        return ((maskA & layerB) != 0) && ((maskB & layerA) != 0);
    }

    /**
     * Get the AABB for an entity.
     */
    public AABB getAABB(long entityId) {
        Optional<BoxCollider> colliderOpt = repository.findByEntityId(entityId);
        if (colliderOpt.isEmpty()) {
            return new AABB(0, 0, 0, 0);
        }

        BoxCollider collider = colliderOpt.get();

        // Get position from EntityModule's shared position components
        float posX = repository.getPositionX(entityId);
        float posY = repository.getPositionY(entityId);

        // Calculate center
        float centerX = posX + collider.offsetX();
        float centerY = posY + collider.offsetY();

        // Calculate half extents
        float halfW = collider.width() / 2;
        float halfH = collider.height() / 2;

        return new AABB(
                centerX - halfW, centerY - halfH,  // min
                centerX + halfW, centerY + halfH   // max
        );
    }

    /**
     * Calculate collision info (normal and penetration depth).
     */
    public CollisionInfo calculateCollisionInfo(AABB a, AABB b) {
        // Calculate overlap on each axis
        float overlapX = Math.min(a.maxX() - b.minX(), b.maxX() - a.minX());
        float overlapY = Math.min(a.maxY() - b.minY(), b.maxY() - a.minY());

        // The normal points from A to B, along the axis of minimum penetration
        float normalX, normalY, penetration;

        if (overlapX < overlapY) {
            penetration = overlapX;
            normalX = (a.centerX() < b.centerX()) ? 1 : -1;
            normalY = 0;
        } else {
            penetration = overlapY;
            normalX = 0;
            normalY = (a.centerY() < b.centerY()) ? 1 : -1;
        }

        return new CollisionInfo(normalX, normalY, penetration);
    }

    private void updateCollisionState(long entity, long otherEntity, CollisionInfo info) {
        // Get current collision count
        Optional<BoxCollider> colliderOpt = repository.findByEntityId(entity);
        if (colliderOpt.isEmpty()) {
            return;
        }

        // Note: In a real implementation, we'd need to track collision count
        // For now, we just set it to 1 when colliding
        repository.updateCollisionState(entity, true, 1, otherEntity,
                info.normalX(), info.normalY(), info.penetrationDepth());
    }
}
