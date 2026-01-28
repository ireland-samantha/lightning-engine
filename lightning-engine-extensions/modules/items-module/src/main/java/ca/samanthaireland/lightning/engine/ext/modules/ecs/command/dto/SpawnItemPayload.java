package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the spawnItem command.
 */
public record SpawnItemPayload(
        @JsonProperty("matchId") Long matchId,
        @JsonProperty("itemTypeId") Long itemTypeId,
        @JsonProperty("positionX") Float positionX,
        @JsonProperty("positionY") Float positionY,
        @JsonProperty("stackSize") Float stackSize
) {
    public long getMatchId() {
        return matchId != null ? matchId : 0L;
    }

    public long getItemTypeId() {
        return itemTypeId != null ? itemTypeId : 0L;
    }

    public float getPositionX() {
        return positionX != null ? positionX : 0f;
    }

    public float getPositionY() {
        return positionY != null ? positionY : 0f;
    }

    public int getStackSize() {
        return stackSize != null ? stackSize.intValue() : 1;
    }
}
