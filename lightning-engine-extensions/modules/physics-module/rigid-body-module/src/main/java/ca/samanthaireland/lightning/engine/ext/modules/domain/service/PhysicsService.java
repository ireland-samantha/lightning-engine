package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.RigidBody;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Vector3;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.RigidBodyRepository;

/**
 * Domain service for physics operations on rigid bodies.
 */
public class PhysicsService {

    private final RigidBodyRepository rigidBodyRepository;

    public PhysicsService(RigidBodyRepository rigidBodyRepository) {
        this.rigidBodyRepository = rigidBodyRepository;
    }

    /**
     * Apply a force to a rigid body (accumulated until next tick).
     *
     * @param entityId the entity ID
     * @param force the force to apply
     * @throws EntityNotFoundException if entity not found
     */
    public void applyForce(long entityId, Vector3 force) {
        RigidBody rigidBody = findById(entityId);
        Vector3 currentForce = rigidBody.force();
        rigidBodyRepository.updateForce(entityId, currentForce.add(force));
    }

    /**
     * Apply an impulse (instant velocity change) to a rigid body.
     *
     * @param entityId the entity ID
     * @param impulse the impulse to apply
     * @throws EntityNotFoundException if entity not found
     */
    public void applyImpulse(long entityId, Vector3 impulse) {
        RigidBody rigidBody = findById(entityId);
        Vector3 deltaVelocity = impulse.divide(rigidBody.mass());
        Vector3 newVelocity = rigidBody.velocity().add(deltaVelocity);
        rigidBodyRepository.updateVelocity(entityId, newVelocity);
    }

    /**
     * Set the velocity of a rigid body directly.
     *
     * @param entityId the entity ID
     * @param velocity the new velocity
     * @throws EntityNotFoundException if entity not found
     */
    public void setVelocity(long entityId, Vector3 velocity) {
        validateExists(entityId);
        rigidBodyRepository.updateVelocity(entityId, velocity);
    }

    /**
     * Set the position of a rigid body directly (teleport).
     *
     * @param entityId the entity ID
     * @param position the new position
     * @throws EntityNotFoundException if entity not found
     */
    public void setPosition(long entityId, Vector3 position) {
        validateExists(entityId);
        rigidBodyRepository.updatePosition(entityId, position);
    }

    /**
     * Apply torque to a rigid body.
     *
     * @param entityId the entity ID
     * @param torque the torque to apply
     * @throws EntityNotFoundException if entity not found
     */
    public void applyTorque(long entityId, float torque) {
        RigidBody rigidBody = findById(entityId);
        rigidBodyRepository.updateAngular(
                entityId,
                rigidBody.angularVelocity(),
                rigidBody.rotation(),
                rigidBody.torque() + torque
        );
    }

    private RigidBody findById(long entityId) {
        return rigidBodyRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException("entityId"));
    }

    private void validateExists(long entityId) {
        if (!rigidBodyRepository.exists(entityId)) {
            throw new EntityNotFoundException("entityId");
        }
    }
}
