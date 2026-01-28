package ca.samanthaireland.lightning.engine.ext.modules.domain;

/**
 * Domain entity representing a 3D position with float coordinates.
 *
 * <p>Used for precise positioning of entities in continuous space.
 */
public record Position(float x, float y, float z) {

    /**
     * Creates a position at the origin (0, 0, 0).
     *
     * @return position at origin
     */
    public static Position origin() {
        return new Position(0f, 0f, 0f);
    }

    /**
     * Creates a position with only x and y coordinates (z = 0).
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return position with z = 0
     */
    public static Position of(float x, float y) {
        return new Position(x, y, 0f);
    }
}
