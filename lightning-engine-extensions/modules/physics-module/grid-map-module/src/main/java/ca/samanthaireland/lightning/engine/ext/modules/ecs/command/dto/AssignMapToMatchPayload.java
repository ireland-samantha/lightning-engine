package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the assignMapToMatch command.
 */
public record AssignMapToMatchPayload(
        @JsonProperty("matchId") long matchId,
        @JsonProperty("mapId") long mapId
) {
}
