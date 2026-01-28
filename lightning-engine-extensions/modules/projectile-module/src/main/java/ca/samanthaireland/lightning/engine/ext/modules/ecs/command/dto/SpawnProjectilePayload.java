package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the spawnProjectile command.
 */
public record SpawnProjectilePayload(
        @JsonProperty("matchId") long matchId,
        @JsonProperty("ownerEntityId") long ownerEntityId,
        @JsonProperty("positionX") Float positionX,
        @JsonProperty("positionY") Float positionY,
        @JsonProperty("directionX") Float directionX,
        @JsonProperty("directionY") Float directionY,
        @JsonProperty("speed") Float speed,
        @JsonProperty("damage") Float damage,
        @JsonProperty("lifetime") Float lifetime,
        @JsonProperty("pierceCount") Float pierceCount,
        @JsonProperty("projectileType") Float projectileType
) {
    /**
     * Returns the positionX with a default value of 0 if not specified.
     */
    public float getPositionX() {
        return positionX != null ? positionX : 0f;
    }

    /**
     * Returns the positionY with a default value of 0 if not specified.
     */
    public float getPositionY() {
        return positionY != null ? positionY : 0f;
    }

    /**
     * Returns the directionX with a default value of 1 if not specified.
     */
    public float getDirectionX() {
        return directionX != null ? directionX : 1f;
    }

    /**
     * Returns the directionY with a default value of 0 if not specified.
     */
    public float getDirectionY() {
        return directionY != null ? directionY : 0f;
    }

    /**
     * Returns the speed with a default value of 10 if not specified.
     */
    public float getSpeed() {
        return speed != null ? speed : 10f;
    }

    /**
     * Returns the damage with a default value of 10 if not specified.
     */
    public float getDamage() {
        return damage != null ? damage : 10f;
    }

    /**
     * Returns the lifetime with a default value of 0 (no limit) if not specified.
     */
    public float getLifetime() {
        return lifetime != null ? lifetime : 0f;
    }

    /**
     * Returns the pierceCount with a default value of 0 if not specified.
     */
    public float getPierceCount() {
        return pierceCount != null ? pierceCount : 0f;
    }

    /**
     * Returns the projectileType with a default value of 0 if not specified.
     */
    public float getProjectileType() {
        return projectileType != null ? projectileType : 0f;
    }
}
