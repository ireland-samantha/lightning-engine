package ca.samanthaireland.lightning.engine.ext.modules.domain;

/**
 * A collision pair detected during the tick.
 */
public record CollisionPair(long entityA, long entityB, CollisionInfo info) {
}
