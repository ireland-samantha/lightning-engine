package ca.samanthaireland.lightning.engine.ext.modules.domain;

/**
 * Domain entity representing position in 3D space.
 *
 * @deprecated use RigidBodyModule
 */
@Deprecated
public record Position(float x, float y, float z) {

    /**
     * Creates a position at the origin.
     */
    public static Position origin() {
        return new Position(0, 0, 0);
    }

    /**
     * Creates a new position from the given coordinates.
     */
    public static Position of(float x, float y, float z) {
        return new Position(x, y, z);
    }

    /**
     * Returns a new position by adding velocity to this position.
     *
     * @param velocity the velocity to add
     * @return the new position
     */
    public Position add(Velocity velocity) {
        return new Position(x + velocity.x(), y + velocity.y(), z + velocity.z());
    }
}
