package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the attachBoxCollider command.
 */
public record AttachBoxColliderPayload(
        @JsonProperty("entityId") Long entityId,
        @JsonProperty("width") Float width,
        @JsonProperty("height") Float height,
        @JsonProperty("depth") Float depth,
        @JsonProperty("offsetX") Float offsetX,
        @JsonProperty("offsetY") Float offsetY,
        @JsonProperty("offsetZ") Float offsetZ,
        @JsonProperty("layer") Integer layer,
        @JsonProperty("mask") Integer mask,
        @JsonProperty("isTrigger") Boolean isTrigger
) {
    public boolean hasEntityId() {
        return entityId != null && entityId != 0;
    }

    public long getEntityId() {
        return entityId != null ? entityId : 0L;
    }

    public float getWidth() {
        return width != null ? width : 1.0f;
    }

    public float getHeight() {
        return height != null ? height : 1.0f;
    }

    public float getDepth() {
        return depth != null ? depth : 1.0f;
    }

    public float getOffsetX() {
        return offsetX != null ? offsetX : 0f;
    }

    public float getOffsetY() {
        return offsetY != null ? offsetY : 0f;
    }

    public float getOffsetZ() {
        return offsetZ != null ? offsetZ : 0f;
    }

    public int getLayer() {
        return layer != null ? layer : 1;
    }

    public int getMask() {
        return mask != null ? mask : -1;
    }

    public boolean getIsTrigger() {
        return isTrigger != null && isTrigger;
    }
}
