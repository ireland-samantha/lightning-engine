package ca.samanthaireland.lightning.engine.ext.modules.domain.repository;

/**
 * Interface for managing collision state during physics simulation.
 *
 * <p>Separated from BoxColliderRepository to follow Interface Segregation Principle.
 * Use this interface in the physics/collision detection system.
 */
public interface CollisionStateManager {

    /**
     * Get entity position X component value.
     *
     * @param entityId the entity ID
     * @return the position X value
     */
    float getPositionX(long entityId);

    /**
     * Get entity position Y component value.
     *
     * @param entityId the entity ID
     * @return the position Y value
     */
    float getPositionY(long entityId);

    /**
     * Get the collision handler type for an entity.
     *
     * @param entityId the entity ID
     * @return the handler type
     */
    int getHandlerType(long entityId);

    /**
     * Get collision handler param1.
     *
     * @param entityId the entity ID
     * @return the param1 value
     */
    float getHandlerParam1(long entityId);

    /**
     * Get collision handler param2.
     *
     * @param entityId the entity ID
     * @return the param2 value
     */
    float getHandlerParam2(long entityId);

    /**
     * Get the last tick when collision was handled.
     *
     * @param entityId the entity ID
     * @return the last handled tick
     */
    long getLastHandledTick(long entityId);

    /**
     * Update collision handled tick.
     *
     * @param entityId the entity ID
     * @param tick the tick value
     */
    void updateHandledTick(long entityId, long tick);

    /**
     * Update collision state for an entity.
     *
     * @param entityId the entity to update
     * @param isColliding whether the entity is colliding
     * @param collisionCount the number of collisions
     * @param lastCollisionEntity the last entity collided with
     * @param normalX collision normal X
     * @param normalY collision normal Y
     * @param penetrationDepth penetration depth
     */
    void updateCollisionState(long entityId, boolean isColliding, int collisionCount,
                              long lastCollisionEntity, float normalX, float normalY,
                              float penetrationDepth);

    /**
     * Reset collision state for an entity.
     *
     * @param entityId the entity to reset
     */
    void resetCollisionState(long entityId);
}
