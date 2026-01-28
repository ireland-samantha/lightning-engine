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

import ca.samanthaireland.lightning.engine.core.entity.CoreComponents;
import ca.samanthaireland.lightning.engine.core.snapshot.ComponentData;
import ca.samanthaireland.lightning.engine.core.snapshot.ModuleData;
import ca.samanthaireland.lightning.engine.core.snapshot.Snapshot;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleResolver;
import ca.samanthaireland.lightning.engine.ext.module.ModuleVersion;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Predicate;

/**
 * Implementation of {@link SnapshotProvider} that creates snapshots from the ECS store.
 *
 * <p>Snapshots contain component values for all entities that match the filter criteria.
 * Supports filtering by:
 * <ul>
 *   <li>Match ID - only entities belonging to the specified match</li>
 *   <li>Player ID (optional) - only entities owned by the specified player</li>
 * </ul>
 *
 * <p>Uses a columnar data format for efficient serialization:
 * <pre>
 * {
 *   "ModuleName": {
 *     "COMPONENT_A": [value1, value2, ...],
 *     "COMPONENT_B": [value1, value2, ...]
 *   }
 * }
 * </pre>
 */
@Slf4j
public class SnapshotProviderImpl implements SnapshotProvider {

    private final EntityComponentStore entityStore;
    private final ModuleResolver moduleResolver;

    private volatile List<ModuleComponentMapping> cachedMappings;

    public SnapshotProviderImpl(EntityComponentStore entityStore, ModuleResolver moduleResolver) {
        this.entityStore = Objects.requireNonNull(entityStore, "entityStore must not be null");
        this.moduleResolver = Objects.requireNonNull(moduleResolver, "moduleResolver must not be null");
    }

    @Override
    public Snapshot createForMatch(long matchId) {
        return createSnapshot(
                new SnapshotFilter(matchId, Optional.empty())
        );
    }

    @Override
    public Snapshot createForMatchAndPlayer(long matchId, long playerId) {
        return createSnapshot(
                new SnapshotFilter(matchId, Optional.of(playerId))
        );
    }

    /**
     * Creates a snapshot using the specified filter criteria.
     */
    private Snapshot createSnapshot(SnapshotFilter filter) {
        List<ModuleComponentMapping> mappings = getOrBuildMappings();
        if (mappings.isEmpty()) {
            log.debug("No module components registered, returning empty snapshot");
            return Snapshot.empty();
        }

        Set<Long> candidateEntities = findCandidateEntities();
        if (candidateEntities.isEmpty()) {
            log.debug("No entities found with registered components");
            return Snapshot.empty();
        }

        Predicate<Long> entityFilter = buildEntityFilter(filter);
        Set<Long> matchingEntities = filterEntities(candidateEntities, entityFilter);

        if (matchingEntities.isEmpty()) {
            log.debug("No entities match filter criteria: {}", filter);
            return Snapshot.empty();
        }

        List<ModuleData> moduleDataList = buildSnapshotData(
                mappings, matchingEntities, filter.matchId()
        );

        log.debug("Created snapshot with {} modules, {} entities for filter: {}",
                moduleDataList.size(), matchingEntities.size(), filter);

        return new Snapshot(moduleDataList);
    }

    /**
     * Finds all entities that have any of the registered flag components.
     */
    private Set<Long> findCandidateEntities() {
        Set<Long> entities = new HashSet<>();

        for (BaseComponent flagComponent : getFlagComponents()) {
            Set<Long> found = entityStore.getEntitiesWithComponents(
                    List.of(flagComponent, CoreComponents.MATCH_ID)
            );
            if (found != null && !found.isEmpty()) {
                entities.addAll(found);
            }
        }

        return entities;
    }

    /**
     * Builds a predicate that filters entities based on the snapshot filter criteria.
     *
     * <p>Optimized to use single getComponent() call instead of hasComponent() + getComponent().
     */
    private Predicate<Long> buildEntityFilter(SnapshotFilter filter) {
        float matchIdFloat = (float) filter.matchId();

        // Optimized: single getComponent call, NaN check handles missing component
        Predicate<Long> matchFilter = entityId -> {
            float storedMatchId = entityStore.getComponent(entityId, CoreComponents.MATCH_ID);
            return storedMatchId == matchIdFloat; // NaN != any value, so this handles missing
        };

        if (filter.playerId().isEmpty()) {
            return matchFilter;
        }

        float playerIdFloat = filter.playerId().get().floatValue();

        return matchFilter.and(entityId -> {
            float ownerId = entityStore.getComponent(entityId, CoreComponents.OWNER_ID);
            return ownerId == playerIdFloat; // NaN != any value
        });
    }

    /**
     * Filters entities using the provided predicate.
     *
     * <p>Pre-allocates result set with estimated capacity to reduce rehashing.
     */
    private Set<Long> filterEntities(Set<Long> entities, Predicate<Long> filter) {
        // Pre-allocate with reasonable capacity (assume ~25% match rate)
        Set<Long> result = new HashSet<>(Math.max(16, entities.size() / 4));
        for (Long entityId : entities) {
            if (filter.test(entityId)) {
                result.add(entityId);
            }
        }
        return result;
    }

    /**
     * Builds the columnar snapshot data structure for matching entities.
     *
     * <p>Optimized to iterate entities once per module using batch component retrieval,
     * avoiding redundant MATCH_ID checks (entities are already pre-filtered).
     */
    private List<ModuleData> buildSnapshotData(
            List<ModuleComponentMapping> mappings,
            Set<Long> entities,
            long matchId) {

        List<ModuleData> moduleDataList = new ArrayList<>();

        // Convert to ordered list once - entities are already filtered by matchId
        List<Long> orderedEntityIds = new ArrayList<>(entities);

        if (orderedEntityIds.isEmpty()) {
            return moduleDataList;
        }

        for (ModuleComponentMapping mapping : mappings) {
            List<ComponentData> components = buildModuleDataOptimized(mapping, orderedEntityIds);

            if (!components.isEmpty()) {
                moduleDataList.add(ModuleData.of(
                        mapping.moduleName(),
                        mapping.moduleVersion(),
                        components
                ));
            }
        }

        return moduleDataList;
    }

    /**
     * Builds the component data for a single module using batch retrieval.
     *
     * <p>Optimizations:
     * <ul>
     *   <li>Uses batch getComponents() to fetch all values per entity in one call</li>
     *   <li>Pre-allocates lists with known capacity</li>
     *   <li>No redundant MATCH_ID filtering (entities are pre-filtered)</li>
     * </ul>
     */
    private List<ComponentData> buildModuleDataOptimized(
            ModuleComponentMapping mapping,
            List<Long> entityIds) {

        List<BaseComponent> components = mapping.components();
        int entityCount = entityIds.size();
        int componentCount = components.size();

        if (componentCount == 0) {
            return Collections.emptyList();
        }

        // Build component list including ENTITY_ID for batch retrieval
        List<BaseComponent> allComponents = new ArrayList<>(componentCount + 1);
        allComponents.add(CoreComponents.ENTITY_ID);
        allComponents.addAll(components);

        int totalComponents = allComponents.size();
        float[] buffer = new float[totalComponents];

        // Pre-allocate all column lists
        List<List<Float>> columnLists = new ArrayList<>(totalComponents);
        for (int i = 0; i < totalComponents; i++) {
            columnLists.add(new ArrayList<>(entityCount));
        }

        // Single pass: fetch all components per entity using batch retrieval
        for (Long entityId : entityIds) {
            entityStore.getComponents(entityId, allComponents, buffer);

            for (int i = 0; i < totalComponents; i++) {
                float value = buffer[i];
                // Only add non-null values to maintain sparse representation
                if (!Float.isNaN(value)) {
                    columnLists.get(i).add(value);
                }
            }
        }

        // Build ComponentData list
        List<ComponentData> result = new ArrayList<>();
        for (int i = 0; i < totalComponents; i++) {
            List<Float> values = columnLists.get(i);
            if (!values.isEmpty()) {
                result.add(ComponentData.of(allComponents.get(i).getName(), values));
            }
        }

        return result;
    }

    private List<ModuleComponentMapping> getOrBuildMappings() {
        List<ModuleComponentMapping> mappings = cachedMappings;
        if (mappings == null) {
            mappings = buildMappings();
            cachedMappings = mappings;
        }
        return mappings;
    }

    private List<BaseComponent> getFlagComponents() {
        List<EngineModule> modules = moduleResolver.resolveAllModules();
        List<BaseComponent> flags = new ArrayList<>();

        for (EngineModule module : modules) {
            BaseComponent flag = module.createFlagComponent();
            if (flag != null) {
                flags.add(flag);
            }
        }

        return flags;
    }

    private List<ModuleComponentMapping> buildMappings() {
        List<EngineModule> modules = moduleResolver.resolveAllModules();
        List<ModuleComponentMapping> mappings = new ArrayList<>();

        for (EngineModule module : modules) {
            List<BaseComponent> components = module.createComponents();
            if (components != null && !components.isEmpty()) {
                mappings.add(new ModuleComponentMapping(
                        module.getName(),
                        module.getVersion(),
                        components
                ));
            }
        }

        log.debug("Built component mappings for {} modules", mappings.size());
        return mappings;
    }

    /**
     * Invalidates the cached component mappings.
     * Call this when modules are added or removed.
     */
    public void invalidateCache() {
        cachedMappings = null;
        log.debug("Component mapping cache invalidated");
    }

    /**
     * Filter criteria for snapshot creation.
     */
    private record SnapshotFilter(long matchId, Optional<Long> playerId) {
        @Override
        public String toString() {
            return playerId
                    .map(pid -> "match=" + matchId + ", player=" + pid)
                    .orElse("match=" + matchId);
        }
    }

    /**
     * Mapping of module name, version, and snapshot components.
     */
    private record ModuleComponentMapping(
            String moduleName,
            ModuleVersion moduleVersion,
            List<BaseComponent> components
    ) {
    }
}
