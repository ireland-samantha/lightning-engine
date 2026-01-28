package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the dropItem command.
 */
public record DropItemPayload(
        @JsonProperty("itemEntityId") Long itemEntityId,
        @JsonProperty("positionX") Float positionX,
        @JsonProperty("positionY") Float positionY
) {
    public long getItemEntityId() {
        return itemEntityId != null ? itemEntityId : 0L;
    }

    public float getPositionX() {
        return positionX != null ? positionX : 0f;
    }

    public float getPositionY() {
        return positionY != null ? positionY : 0f;
    }

    public boolean hasItemEntityId() {
        return itemEntityId != null;
    }
}
