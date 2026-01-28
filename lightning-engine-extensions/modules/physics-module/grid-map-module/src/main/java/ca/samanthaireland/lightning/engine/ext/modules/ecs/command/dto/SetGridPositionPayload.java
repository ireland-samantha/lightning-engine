package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the setGridPosition command.
 */
public record SetGridPositionPayload(
        @JsonProperty("entityId") Long entityId,
        @JsonProperty("gridX") Integer gridX,
        @JsonProperty("gridY") Integer gridY,
        @JsonProperty("gridZ") Integer gridZ,
        @JsonProperty("mapId") Long mapId
) {
    /**
     * Returns the gridX with a default value of -1 if not specified.
     */
    public int xOrDefault() {
        return gridX != null ? gridX : -1;
    }

    /**
     * Returns the gridY with a default value of -1 if not specified.
     */
    public int yOrDefault() {
        return gridY != null ? gridY : -1;
    }

    /**
     * Returns the gridZ with a default value of -1 if not specified.
     */
    public int zOrDefault() {
        return gridZ != null ? gridZ : -1;
    }

    /**
     * Returns the mapId with a default value of -1 if not specified.
     */
    public long getMapId() {
        return mapId != null ? mapId : -1L;
    }

    /**
     * Returns the mapId with a default value of 0 if not specified.
     */
    public boolean hasMapId() {
        return mapId != null;
    }

    /**
     * Returns the mapId with a default value of 0 if not specified.
     */
    public boolean hasEntityId() {
        return entityId != null;
    }



    /**
     * Returns true if gridX was explicitly set in the payload.
     */
    public boolean hasGridX() {
        return gridX != null;
    }

    /**
     * Returns true if gridY was explicitly set in the payload.
     */
    public boolean hasGridY() {
        return gridY != null;
    }

    /**
     * Returns true if gridZ was explicitly set in the payload.
     */
    public boolean hasGridZ() {
        return gridZ != null;
    }
}
