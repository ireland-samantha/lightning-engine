package ca.samanthaireland.lightning.engine.ext.modules.domain.repository;

import ca.samanthaireland.lightning.engine.ext.modules.domain.Vector3;

/**
 * Interface for updating physics properties of rigid bodies.
 *
 * <p>Separated from RigidBodyRepository to follow Interface Segregation Principle.
 * Use this interface in the physics integration system for granular updates.
 */
public interface RigidBodyPhysicsUpdater {

    /**
     * Update the position of a rigid body.
     *
     * @param entityId the entity ID
     * @param position the new position
     */
    void updatePosition(long entityId, Vector3 position);

    /**
     * Update the velocity of a rigid body.
     *
     * @param entityId the entity ID
     * @param velocity the new velocity
     */
    void updateVelocity(long entityId, Vector3 velocity);

    /**
     * Update the force accumulator of a rigid body.
     *
     * @param entityId the entity ID
     * @param force the new force
     */
    void updateForce(long entityId, Vector3 force);

    /**
     * Update the acceleration of a rigid body.
     *
     * @param entityId the entity ID
     * @param acceleration the new acceleration
     */
    void updateAcceleration(long entityId, Vector3 acceleration);

    /**
     * Update angular properties of a rigid body.
     *
     * @param entityId the entity ID
     * @param angularVelocity the new angular velocity
     * @param rotation the new rotation
     * @param torque the new torque
     */
    void updateAngular(long entityId, float angularVelocity, float rotation, float torque);
}
