package ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload DTO for the spawn command.
 *
 * <p>Note: Position is now handled by GridMapModule's SetEntityPositionCommand.
 */
public record SpawnPayload(
        @JsonProperty("matchId") long matchId,
        @JsonProperty("playerId") long playerId,
        @JsonProperty("entityType") long entityType
) {
}
