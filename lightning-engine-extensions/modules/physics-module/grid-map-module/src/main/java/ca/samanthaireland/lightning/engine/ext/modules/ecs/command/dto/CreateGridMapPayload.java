package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the createGridMap command.
 */
public record CreateGridMapPayload(
        @JsonProperty("matchId") long matchId,
        @JsonProperty("width") Integer width,
        @JsonProperty("height") Integer height,
        @JsonProperty("depth") Integer depth
) {
    /**
     * Returns the width with a default value of 10 if not specified.
     */
    public int getWidth() {
        return width != null ? width : 10;
    }

    /**
     * Returns the height with a default value of 10 if not specified.
     */
    public int getHeight() {
        return height != null ? height : 10;
    }

    /**
     * Returns the depth with a default value of 1 if not specified.
     */
    public int getDepth() {
        return depth != null ? depth : 1;
    }
}
