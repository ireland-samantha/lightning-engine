/*
 * Copyright (c) 2026 Samantha Ireland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package ca.samanthaireland.lightning.engine.internal.core.snapshot;

import ca.samanthaireland.lightning.engine.core.snapshot.Snapshot;
import ca.samanthaireland.lightning.engine.core.snapshot.SnapshotHistory;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link SnapshotHistory} with configurable retention.
 *
 * <p>Maintains a sliding window of snapshots per match, automatically evicting
 * old snapshots when the maximum count is exceeded.
 */
@Slf4j
public class InMemorySnapshotHistory implements SnapshotHistory {

    private static final int DEFAULT_MAX_SNAPSHOTS_PER_MATCH = 100;

    private final int maxSnapshotsPerMatch;
    private final Map<Long, NavigableMap<Long, Snapshot>> historyByMatch;

    public InMemorySnapshotHistory() {
        this(DEFAULT_MAX_SNAPSHOTS_PER_MATCH);
    }

    public InMemorySnapshotHistory(int maxSnapshotsPerMatch) {
        this.maxSnapshotsPerMatch = maxSnapshotsPerMatch;
        this.historyByMatch = new ConcurrentHashMap<>();
    }

    @Override
    public void recordSnapshot(long matchId, long tick, Snapshot snapshot) {
        NavigableMap<Long, Snapshot> matchHistory = historyByMatch.computeIfAbsent(
                matchId, k -> Collections.synchronizedNavigableMap(new TreeMap<>()));

        synchronized (matchHistory) {
            matchHistory.put(tick, snapshot);

            // Evict old snapshots if we exceed the limit
            while (matchHistory.size() > maxSnapshotsPerMatch) {
                Long oldestTick = matchHistory.firstKey();
                matchHistory.remove(oldestTick);
                log.trace("Evicted snapshot at tick {} for match {} (history size: {})",
                        oldestTick, matchId, matchHistory.size());
            }
        }

        log.trace("Recorded snapshot at tick {} for match {} (history size: {})",
                tick, matchId, matchHistory.size());
    }

    @Override
    public Optional<Snapshot> getSnapshot(long matchId, long tick) {
        NavigableMap<Long, Snapshot> matchHistory = historyByMatch.get(matchId);
        if (matchHistory == null) {
            return Optional.empty();
        }

        synchronized (matchHistory) {
            return Optional.ofNullable(matchHistory.get(tick));
        }
    }

    @Override
    public Optional<TickedSnapshot> getLatestSnapshot(long matchId) {
        NavigableMap<Long, Snapshot> matchHistory = historyByMatch.get(matchId);
        if (matchHistory == null || matchHistory.isEmpty()) {
            return Optional.empty();
        }

        synchronized (matchHistory) {
            if (matchHistory.isEmpty()) {
                return Optional.empty();
            }
            Map.Entry<Long, Snapshot> entry = matchHistory.lastEntry();
            return Optional.of(new TickedSnapshot(entry.getKey(), entry.getValue()));
        }
    }

    @Override
    public Optional<TickedSnapshot> getOldestSnapshot(long matchId) {
        NavigableMap<Long, Snapshot> matchHistory = historyByMatch.get(matchId);
        if (matchHistory == null || matchHistory.isEmpty()) {
            return Optional.empty();
        }

        synchronized (matchHistory) {
            if (matchHistory.isEmpty()) {
                return Optional.empty();
            }
            Map.Entry<Long, Snapshot> entry = matchHistory.firstEntry();
            return Optional.of(new TickedSnapshot(entry.getKey(), entry.getValue()));
        }
    }

    @Override
    public void clearHistory(long matchId) {
        NavigableMap<Long, Snapshot> removed = historyByMatch.remove(matchId);
        if (removed != null) {
            log.debug("Cleared {} snapshots for match {}", removed.size(), matchId);
        }
    }

    @Override
    public int getSnapshotCount(long matchId) {
        NavigableMap<Long, Snapshot> matchHistory = historyByMatch.get(matchId);
        if (matchHistory == null) {
            return 0;
        }

        synchronized (matchHistory) {
            return matchHistory.size();
        }
    }

    /**
     * Get the maximum number of snapshots retained per match.
     */
    public int getMaxSnapshotsPerMatch() {
        return maxSnapshotsPerMatch;
    }

    /**
     * Get all ticks that have snapshots stored for a match.
     */
    public Set<Long> getAvailableTicks(long matchId) {
        NavigableMap<Long, Snapshot> matchHistory = historyByMatch.get(matchId);
        if (matchHistory == null) {
            return Set.of();
        }

        synchronized (matchHistory) {
            return new LinkedHashSet<>(matchHistory.keySet());
        }
    }
}
