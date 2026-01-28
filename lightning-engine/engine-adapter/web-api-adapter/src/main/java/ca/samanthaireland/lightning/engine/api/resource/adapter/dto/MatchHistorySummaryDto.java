/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */

package ca.samanthaireland.lightning.engine.api.resource.adapter.dto;

import java.time.Instant;

/**
 * DTO for match history summary.
 *
 * @param containerId the container ID
 * @param matchId the match ID
 * @param snapshotCount total number of snapshots
 * @param firstTick first snapshot tick (-1 if none)
 * @param lastTick last snapshot tick (-1 if none)
 * @param firstTimestamp first snapshot timestamp (null if none)
 * @param lastTimestamp last snapshot timestamp (null if none)
 */
public record MatchHistorySummaryDto(
        long containerId,
        long matchId,
        long snapshotCount,
        long firstTick,
        long lastTick,
        Instant firstTimestamp,
        Instant lastTimestamp
) {}
