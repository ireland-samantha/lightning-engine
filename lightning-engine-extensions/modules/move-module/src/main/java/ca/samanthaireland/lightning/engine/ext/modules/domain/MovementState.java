package ca.samanthaireland.lightning.engine.ext.modules.domain;

/**
 * Domain entity representing the complete movement state of an entity.
 * Combines position and velocity.
 *
 * @deprecated use RigidBodyModule
 */
@Deprecated
public record MovementState(long entityId, Position position, Velocity velocity) {

    /**
     * Creates a new movement state with the given position and velocity.
     */
    public static MovementState of(long entityId, Position position, Velocity velocity) {
        return new MovementState(entityId, position, velocity);
    }

    /**
     * Creates a movement state with zero velocity.
     */
    public static MovementState atPosition(long entityId, Position position) {
        return new MovementState(entityId, position, Velocity.zero());
    }

    /**
     * Returns a new movement state with updated position after applying velocity.
     */
    public MovementState applyVelocity() {
        return new MovementState(entityId, position.add(velocity), velocity);
    }
}
