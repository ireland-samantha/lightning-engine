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


package ca.samanthaireland.lightning.engine.quarkus.api.persistence;

import ca.samanthaireland.lightning.engine.core.match.Match;
import ca.samanthaireland.lightning.engine.core.match.MatchService;
import ca.samanthaireland.lightning.engine.core.snapshot.SnapshotRestoreService;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.ComponentRegistry;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Default implementation of {@link SnapshotRestoreService}.
 *
 * <p>This service restores match state from MongoDB snapshots by:
 * <ol>
 *   <li>Loading the snapshot document from MongoDB</li>
 *   <li>Clearing existing entities for the match</li>
 *   <li>Recreating entities with their component values</li>
 *   <li>Updating the simulation tick counter</li>
 * </ol>
 */
public class DefaultSnapshotRestoreService implements SnapshotRestoreService {
    private static final Logger log = LoggerFactory.getLogger(DefaultSnapshotRestoreService.class);

    private final SnapshotHistoryRepository historyRepository;
    private final EntityComponentStore store;
    private final MatchService matchService;
    private final ComponentRegistry componentRegistry;

    public DefaultSnapshotRestoreService(
            SnapshotHistoryRepository historyRepository,
            EntityComponentStore store,
            MatchService matchService,
            ComponentRegistry componentRegistry) {
        this.historyRepository = historyRepository;
        this.store = store;
        this.matchService = matchService;
        this.componentRegistry = componentRegistry;
    }

    @Override
    public RestoreResult restoreMatch(long matchId) {
        return restoreMatch(matchId, -1);
    }

    @Override
    public RestoreResult restoreMatch(long matchId, long tick) {
        log.info("Restoring match {} to tick {}", matchId, tick < 0 ? "latest" : tick);

        // 1. Find snapshot
        Optional<SnapshotDocument> snapshotOpt = tick < 0
            ? historyRepository.findLastByMatchId(matchId)
            : historyRepository.findByMatchIdAndTick(matchId, tick);

        if (snapshotOpt.isEmpty()) {
            String msg = tick < 0
                ? "No snapshots found for match " + matchId
                : "No snapshot found for match " + matchId + " at tick " + tick;
            log.warn(msg);
            return RestoreResult.failure(matchId, msg);
        }

        SnapshotDocument snapshot = snapshotOpt.get();

        try {
            // 2. Clear existing match entities
            int clearedCount = clearMatchEntities(matchId);
            log.debug("Cleared {} existing entities for match {}", clearedCount, matchId);

            // 3. Restore entities from snapshot
            int entityCount = restoreEntities(matchId, snapshot);

            // 4. Ensure match record exists
            ensureMatchExists(matchId, snapshot);

            // Note: tick counter is managed by the game loop and resets on server restart.
            // The restored snapshot tick is recorded in the result for informational purposes.

            log.info("Restored match {} to tick {} with {} entities",
                matchId, snapshot.tick(), entityCount);

            return RestoreResult.success(matchId, snapshot.tick(), entityCount);

        } catch (Exception e) {
            log.error("Failed to restore match {}: {}", matchId, e.getMessage(), e);
            return RestoreResult.failure(matchId, e.getMessage());
        }
    }

    @Override
    public List<RestoreResult> restoreAllMatches() {
        List<Long> matchIds = historyRepository.findDistinctMatchIds();
        log.info("Found {} matches with snapshots to restore", matchIds.size());

        List<RestoreResult> results = new ArrayList<>();
        for (Long matchId : matchIds) {
            results.add(restoreMatch(matchId));
        }

        return results;
    }

    @Override
    public boolean canRestore(long matchId) {
        return historyRepository.countByMatchId(matchId) > 0;
    }

    /**
     * Clear all entities belonging to a match.
     *
     * @return the number of entities cleared
     */
    private int clearMatchEntities(long matchId) {
        // Find MATCH_ID component
        Optional<BaseComponent> matchIdComponent = componentRegistry.findByName("MATCH_ID");
        if (matchIdComponent.isEmpty()) {
            log.warn("MATCH_ID component not registered, cannot clear match entities");
            return 0;
        }

        Set<Long> matchEntities = store.getEntitiesWithComponents(
            List.of(matchIdComponent.get()));

        int count = 0;
        for (Long entityId : matchEntities) {
            float entityMatchId = store.getComponent(entityId, matchIdComponent.get());
            if ((long) entityMatchId == matchId) {
                store.deleteEntity(entityId);
                count++;
            }
        }

        return count;
    }

    /**
     * Restore entities from snapshot data.
     *
     * @return the number of entities restored
     */
    private int restoreEntities(long matchId, SnapshotDocument snapshot) {
        Map<String, Map<String, List<Float>>> data = snapshot.data();

        // Determine entity count from ENTITY_ID arrays
        int entityCount = 0;
        for (var moduleData : data.values()) {
            List<Float> entityIds = moduleData.get("ENTITY_ID");
            if (entityIds != null) {
                entityCount = Math.max(entityCount, entityIds.size());
            }
        }

        if (entityCount == 0) {
            log.debug("No entities to restore for match {}", matchId);
            return 0;
        }

        // Create entities and attach components
        for (int i = 0; i < entityCount; i++) {
            long entityId = store.createEntityForMatch(matchId);
            final int entityIndex = i;

            // Attach all other components from snapshot
            for (var moduleEntry : data.entrySet()) {
                for (var componentEntry : moduleEntry.getValue().entrySet()) {
                    String componentName = componentEntry.getKey();
                    List<Float> values = componentEntry.getValue();

                    // Skip ENTITY_ID as it's implicit
                    if ("ENTITY_ID".equals(componentName)) {
                        continue;
                    }

                    // Skip if no value for this entity
                    if (entityIndex >= values.size()) {
                        continue;
                    }

                    // Find and attach the component
                    float value = values.get(entityIndex);
                    componentRegistry.findByName(componentName).ifPresent(component ->
                        store.attachComponent(entityId, component, value));
                }
            }
        }

        log.debug("Restored {} entities for match {}", entityCount, matchId);
        return entityCount;
    }

    /**
     * Ensure the match record exists in the match service.
     */
    private void ensureMatchExists(long matchId, SnapshotDocument snapshot) {
        if (matchService.matchExists(matchId)) {
            return;
        }

        // Extract module names from snapshot data
        List<String> moduleNames = new ArrayList<>(snapshot.data().keySet());

        Match match = new Match(matchId, moduleNames, List.of());
        matchService.createMatch(match);
        log.debug("Created match record for restored match {}", matchId);
    }
}
