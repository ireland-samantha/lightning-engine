package ca.samanthaireland.lightning.engine.ext.modules.domain;

/**
 * Axis-aligned bounding box.
 */
public record AABB(float minX, float minY, float maxX, float maxY) {

    public float centerX() {
        return (minX + maxX) / 2;
    }

    public float centerY() {
        return (minY + maxY) / 2;
    }

    public float width() {
        return maxX - minX;
    }

    public float height() {
        return maxY - minY;
    }

    /**
     * Test if this AABB intersects another.
     */
    public boolean intersects(AABB other) {
        return minX < other.maxX && maxX > other.minX &&
               minY < other.maxY && maxY > other.minY;
    }
}
