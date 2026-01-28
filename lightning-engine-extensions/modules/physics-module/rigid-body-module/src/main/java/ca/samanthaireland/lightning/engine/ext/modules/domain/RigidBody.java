package ca.samanthaireland.lightning.engine.ext.modules.domain;

/**
 * Domain entity representing a rigid body with physical properties.
 *
 * <p>A rigid body has mass, inertia, position, velocity, and drag properties.
 * All mass and inertia values must be positive.
 */
public record RigidBody(
        long entityId,
        Vector3 position,
        Vector3 velocity,
        Vector3 acceleration,
        Vector3 force,
        float mass,
        float linearDrag,
        float angularDrag,
        float angularVelocity,
        float rotation,
        float torque,
        float inertia
) {

    /**
     * Default physics timestep (1/60 second).
     */
    public static final float DEFAULT_DT = 1.0f / 60.0f;

    /**
     * Creates a new rigid body with validated properties.
     *
     * @throws IllegalArgumentException if mass or inertia is not positive
     */
    public RigidBody {
        if (mass <= 0) {
            throw new IllegalArgumentException("Mass must be positive, got: " + mass);
        }
        if (inertia <= 0) {
            throw new IllegalArgumentException("Inertia must be positive, got: " + inertia);
        }
    }

    /**
     * Creates a new rigid body with default values.
     *
     * @param entityId the entity ID
     * @param position initial position
     * @param velocity initial velocity
     * @param mass mass (must be positive)
     * @return a new RigidBody
     */
    public static RigidBody create(long entityId, Vector3 position, Vector3 velocity, float mass) {
        return new RigidBody(
                entityId,
                position,
                velocity,
                Vector3.ZERO,
                Vector3.ZERO,
                mass,
                0,
                0,
                0,
                0,
                0,
                1.0f
        );
    }

    /**
     * Creates a rigid body with all properties.
     */
    public static RigidBody create(
            long entityId,
            Vector3 position,
            Vector3 velocity,
            float mass,
            float linearDrag,
            float angularDrag,
            float inertia
    ) {
        return new RigidBody(
                entityId,
                position,
                velocity,
                Vector3.ZERO,
                Vector3.ZERO,
                mass,
                linearDrag,
                angularDrag,
                0,
                0,
                0,
                inertia
        );
    }

    /**
     * Apply a force to this rigid body.
     *
     * @param additionalForce the force to add
     * @return a new rigid body with accumulated force
     */
    public RigidBody applyForce(Vector3 additionalForce) {
        return new RigidBody(
                entityId, position, velocity, acceleration,
                force.add(additionalForce),
                mass, linearDrag, angularDrag, angularVelocity, rotation, torque, inertia
        );
    }

    /**
     * Apply torque to this rigid body.
     *
     * @param additionalTorque the torque to add
     * @return a new rigid body with accumulated torque
     */
    public RigidBody applyTorque(float additionalTorque) {
        return new RigidBody(
                entityId, position, velocity, acceleration, force,
                mass, linearDrag, angularDrag, angularVelocity, rotation,
                torque + additionalTorque, inertia
        );
    }

    /**
     * Apply an impulse (instant velocity change).
     *
     * @param impulse the impulse to apply
     * @return a new rigid body with updated velocity
     */
    public RigidBody applyImpulse(Vector3 impulse) {
        Vector3 deltaVelocity = impulse.divide(mass);
        return new RigidBody(
                entityId, position, velocity.add(deltaVelocity), acceleration, force,
                mass, linearDrag, angularDrag, angularVelocity, rotation, torque, inertia
        );
    }

    /**
     * Update the rigid body with a new velocity.
     *
     * @param newVelocity the new velocity
     * @return a new rigid body with updated velocity
     */
    public RigidBody withVelocity(Vector3 newVelocity) {
        return new RigidBody(
                entityId, position, newVelocity, acceleration, force,
                mass, linearDrag, angularDrag, angularVelocity, rotation, torque, inertia
        );
    }

    /**
     * Update the rigid body with a new position.
     *
     * @param newPosition the new position
     * @return a new rigid body with updated position
     */
    public RigidBody withPosition(Vector3 newPosition) {
        return new RigidBody(
                entityId, newPosition, velocity, acceleration, force,
                mass, linearDrag, angularDrag, angularVelocity, rotation, torque, inertia
        );
    }
}
