package ca.samanthaireland.lightning.engine.ext.modules.domain;

/**
 * Domain entity representing velocity in 3D space.
 *
 * @deprecated use RigidBodyModule
 */
@Deprecated
public record Velocity(float x, float y, float z) {

    /**
     * Creates a zero velocity.
     */
    public static Velocity zero() {
        return new Velocity(0, 0, 0);
    }

    /**
     * Creates a new velocity from the given components.
     */
    public static Velocity of(float x, float y, float z) {
        return new Velocity(x, y, z);
    }
}
