/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */

package ca.samanthaireland.lightning.engine.api.resource.adapter.dto;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for a single historical snapshot.
 *
 * @param containerId the container ID
 * @param matchId the match ID
 * @param tick the tick number
 * @param timestamp when the snapshot was captured
 * @param data the snapshot data (module -> component -> values)
 */
public record HistorySnapshotDto(
        long containerId,
        long matchId,
        long tick,
        Instant timestamp,
        Map<String, Object> data
) {}
