package ca.samanthaireland.lightning.engine.ext.modules.domain;

/**
 * Domain entity representing a grid map with dimensions.
 *
 * <p>A map defines a bounded area where entities can be positioned.
 * All dimensions must be positive.
 */
public record GridMap(long id, int width, int height, int depth) {

    /**
     * Creates a new map with validated dimensions.
     *
     * @throws IllegalArgumentException if any dimension is not positive
     */
    public GridMap {
        if (width <= 0) {
            throw new IllegalArgumentException("Map width must be positive, got: " + width);
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Map height must be positive, got: " + height);
        }
        if (depth <= 0) {
            throw new IllegalArgumentException("Map depth must be positive, got: " + depth);
        }
    }

    /**
     * Creates a map without an assigned ID (for creation).
     */
    public static GridMap create(int width, int height, int depth) {
        return new GridMap(0, width, height, depth);
    }

    /**
     * Check if a position is within this map's bounds.
     *
     * @param position the position to check
     * @return true if position is within bounds
     */
    public boolean contains(Position position) {
        return position.x() >= 0 && position.x() < width
                && position.y() >= 0 && position.y() < height
                && position.z() >= 0 && position.z() < depth;
    }

    /**
     * Validate that a position is within bounds.
     *
     * @param position the position to validate
     * @throws PositionOutOfBoundsException if position is outside map bounds
     */
    public void isWithinBounds(Position position) {
        if (!contains(position)) {
            throw new PositionOutOfBoundsException(position, this);
        }
    }
}
