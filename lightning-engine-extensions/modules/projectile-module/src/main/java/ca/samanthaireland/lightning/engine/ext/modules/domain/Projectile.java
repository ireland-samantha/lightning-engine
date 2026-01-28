package ca.samanthaireland.lightning.engine.ext.modules.domain;

/**
 * Domain entity representing a projectile.
 *
 * <p>A projectile has an owner, damage, movement properties, and lifetime tracking.
 * Direction is always normalized.
 */
public record Projectile(
        long id,
        long ownerEntityId,
        float positionX,
        float positionY,
        float directionX,
        float directionY,
        float speed,
        float damage,
        float lifetime,
        float ticksAlive,
        float pierceCount,
        float hitsRemaining,
        float projectileType,
        boolean pendingDestroy
) {

    /**
     * Creates a new projectile with validated and normalized direction.
     *
     * @throws IllegalArgumentException if speed is negative
     */
    public Projectile {
        if (speed < 0) {
            throw new IllegalArgumentException("Projectile speed cannot be negative, got: " + speed);
        }
        if (damage < 0) {
            throw new IllegalArgumentException("Projectile damage cannot be negative, got: " + damage);
        }

        // Normalize direction
        float magnitude = (float) Math.sqrt(directionX * directionX + directionY * directionY);
        if (magnitude > 0) {
            directionX = directionX / magnitude;
            directionY = directionY / magnitude;
        }
    }

    /**
     * Creates a projectile without an assigned ID (for creation).
     */
    public static Projectile create(
            long ownerEntityId,
            float positionX,
            float positionY,
            float directionX,
            float directionY,
            float speed,
            float damage,
            float lifetime,
            float pierceCount,
            float projectileType
    ) {
        return new Projectile(
                0,
                ownerEntityId,
                positionX,
                positionY,
                directionX,
                directionY,
                speed,
                damage,
                lifetime,
                0,  // ticksAlive starts at 0
                pierceCount,
                pierceCount,  // hitsRemaining starts at pierceCount
                projectileType,
                false  // not pending destroy
        );
    }

    /**
     * Check if this projectile has expired based on its lifetime.
     *
     * @return true if lifetime is positive and ticksAlive has reached lifetime
     */
    public boolean hasExpired() {
        return lifetime > 0 && ticksAlive >= lifetime;
    }

    /**
     * Creates a copy with updated position after movement.
     *
     * @return a new Projectile with updated position
     */
    public Projectile withMovement() {
        float newPosX = positionX + directionX * speed;
        float newPosY = positionY + directionY * speed;
        return new Projectile(
                id, ownerEntityId, newPosX, newPosY,
                directionX, directionY, speed, damage,
                lifetime, ticksAlive, pierceCount, hitsRemaining,
                projectileType, pendingDestroy
        );
    }

    /**
     * Creates a copy with incremented tick count.
     *
     * @return a new Projectile with ticksAlive incremented by 1
     */
    public Projectile withTick() {
        return new Projectile(
                id, ownerEntityId, positionX, positionY,
                directionX, directionY, speed, damage,
                lifetime, ticksAlive + 1, pierceCount, hitsRemaining,
                projectileType, pendingDestroy
        );
    }

    /**
     * Creates a copy marked for destruction.
     *
     * @return a new Projectile with pendingDestroy set to true
     */
    public Projectile withPendingDestroy() {
        return new Projectile(
                id, ownerEntityId, positionX, positionY,
                directionX, directionY, speed, damage,
                lifetime, ticksAlive, pierceCount, hitsRemaining,
                projectileType, true
        );
    }

    /**
     * Creates a copy with the given ID.
     *
     * @param newId the new entity ID
     * @return a new Projectile with the assigned ID
     */
    public Projectile withId(long newId) {
        return new Projectile(
                newId, ownerEntityId, positionX, positionY,
                directionX, directionY, speed, damage,
                lifetime, ticksAlive, pierceCount, hitsRemaining,
                projectileType, pendingDestroy
        );
    }
}
