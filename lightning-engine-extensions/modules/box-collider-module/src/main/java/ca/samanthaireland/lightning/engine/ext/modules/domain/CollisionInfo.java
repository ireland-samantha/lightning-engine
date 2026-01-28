package ca.samanthaireland.lightning.engine.ext.modules.domain;

/**
 * Collision info containing normal and penetration depth.
 */
public record CollisionInfo(float normalX, float normalY, float penetrationDepth) {

    /**
     * Return inverted collision info (for the other entity in the pair).
     */
    public CollisionInfo inverted() {
        return new CollisionInfo(-normalX, -normalY, penetrationDepth);
    }
}
