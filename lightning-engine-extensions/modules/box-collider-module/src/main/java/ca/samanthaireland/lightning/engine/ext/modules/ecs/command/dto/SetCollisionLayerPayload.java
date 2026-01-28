package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the setCollisionLayer command.
 */
public record SetCollisionLayerPayload(
        @JsonProperty("entityId") Long entityId,
        @JsonProperty("layer") Integer layer,
        @JsonProperty("mask") Integer mask
) {
    public boolean hasEntityId() {
        return entityId != null && entityId != 0;
    }

    public long getEntityId() {
        return entityId != null ? entityId : 0L;
    }

    public int getLayer() {
        return layer != null ? layer : 1;
    }

    public int getMask() {
        return mask != null ? mask : -1;
    }
}
