package ca.samanthaireland.lightning.engine.ext.modules.domain;

/**
 * Immutable 3D vector for physics calculations.
 *
 * <p>Used for position, velocity, acceleration, and force representations.
 */
public record Vector3(float x, float y, float z) {

    /**
     * Zero vector constant.
     */
    public static final Vector3 ZERO = new Vector3(0, 0, 0);

    /**
     * Add another vector to this one.
     *
     * @param other the vector to add
     * @return a new vector representing the sum
     */
    public Vector3 add(Vector3 other) {
        return new Vector3(x + other.x, y + other.y, z + other.z);
    }

    /**
     * Multiply this vector by a scalar.
     *
     * @param scalar the scalar multiplier
     * @return a new scaled vector
     */
    public Vector3 multiply(float scalar) {
        return new Vector3(x * scalar, y * scalar, z * scalar);
    }

    /**
     * Divide this vector by a scalar.
     *
     * @param scalar the scalar divisor
     * @return a new scaled vector
     * @throws IllegalArgumentException if scalar is zero
     */
    public Vector3 divide(float scalar) {
        if (scalar == 0) {
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        return new Vector3(x / scalar, y / scalar, z / scalar);
    }
}
