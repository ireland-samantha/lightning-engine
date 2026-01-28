package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.RigidBody;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Vector3;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.RigidBodyRepository;

/**
 * Domain service for rigid body lifecycle operations.
 */
public class RigidBodyService {

    private final RigidBodyRepository rigidBodyRepository;

    public RigidBodyService(RigidBodyRepository rigidBodyRepository) {
        this.rigidBodyRepository = rigidBodyRepository;
    }

    /**
     * Attach a rigid body to an entity.
     *
     * @param entityId the entity ID
     * @param position initial position
     * @param velocity initial velocity
     * @param mass mass (must be positive)
     * @param linearDrag linear drag coefficient (0-1)
     * @param angularDrag angular drag coefficient (0-1)
     * @param inertia moment of inertia (must be positive)
     * @return the created rigid body
     * @throws IllegalArgumentException if mass or inertia is not positive
     */
    public RigidBody attachRigidBody(
            long entityId,
            Vector3 position,
            Vector3 velocity,
            float mass,
            float linearDrag,
            float angularDrag,
            float inertia
    ) {
        RigidBody rigidBody = RigidBody.create(
                entityId, position, velocity, mass, linearDrag, angularDrag, inertia
        );
        rigidBodyRepository.save(rigidBody);
        return rigidBody;
    }

    /**
     * Find a rigid body by entity ID.
     *
     * @param entityId the entity ID
     * @return the rigid body
     * @throws EntityNotFoundException if not found
     */
    public RigidBody findById(long entityId) {
        return rigidBodyRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException("entityId"));
    }

    /**
     * Delete a rigid body from an entity.
     *
     * @param entityId the entity ID
     */
    public void delete(long entityId) {
        rigidBodyRepository.delete(entityId);
    }

    /**
     * Check if an entity has a rigid body.
     *
     * @param entityId the entity ID
     * @return true if the entity has a rigid body
     */
    public boolean exists(long entityId) {
        return rigidBodyRepository.exists(entityId);
    }
}
