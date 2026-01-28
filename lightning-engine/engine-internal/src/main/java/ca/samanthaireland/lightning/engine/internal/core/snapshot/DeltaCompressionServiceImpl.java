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
import ca.samanthaireland.lightning.engine.core.snapshot.DeltaCompressionService;
import ca.samanthaireland.lightning.engine.core.snapshot.DeltaSnapshot;
import ca.samanthaireland.lightning.engine.core.snapshot.Snapshot;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Implementation of {@link DeltaCompressionService} that computes efficient deltas
 * between snapshots.
 *
 * <p>The algorithm works by:
 * <ol>
 *   <li>Extracting entity IDs from both snapshots using ENTITY_ID component</li>
 *   <li>Computing added/removed entities by set difference</li>
 *   <li>For each entity present in both, comparing component values</li>
 *   <li>Recording only values that changed</li>
 * </ol>
 */
@Slf4j
public class DeltaCompressionServiceImpl implements DeltaCompressionService {

    private static final String ENTITY_ID_COMPONENT = CoreComponents.ENTITY_ID.getName();

    @Override
    public DeltaSnapshot computeDelta(long matchId, long fromTick, Snapshot from, long toTick, Snapshot to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Snapshots cannot be null");
        }

        Map<String, Map<String, List<Float>>> fromData = from.toLegacyFormat();
        Map<String, Map<String, List<Float>>> toData = to.toLegacyFormat();

        // Extract entity IDs from both snapshots
        Set<Long> fromEntities = extractEntityIds(fromData);
        Set<Long> toEntities = extractEntityIds(toData);

        // Compute added and removed entities
        Set<Long> addedEntities = new LinkedHashSet<>(toEntities);
        addedEntities.removeAll(fromEntities);

        Set<Long> removedEntities = new LinkedHashSet<>(fromEntities);
        removedEntities.removeAll(toEntities);

        // Compute changed components for entities that exist in both
        Map<String, Map<String, Map<Long, Float>>> changedComponents = computeChangedComponents(
                fromData, toData, fromEntities, toEntities);

        DeltaSnapshot delta = new DeltaSnapshot(
                matchId,
                fromTick,
                toTick,
                changedComponents,
                addedEntities,
                removedEntities
        );

        log.debug("Computed delta from tick {} to {}: {} changes, {} added, {} removed",
                fromTick, toTick, delta.changeCount(), addedEntities.size(), removedEntities.size());

        return delta;
    }

    @Override
    public Snapshot applyDelta(Snapshot base, DeltaSnapshot delta) {
        if (base == null || delta == null) {
            throw new IllegalArgumentException("Base snapshot and delta cannot be null");
        }

        Map<String, Map<String, List<Float>>> baseData = base.toLegacyFormat();
        Map<String, Map<String, List<Float>>> resultData = new LinkedHashMap<>();

        // Build entity ID to index mappings for the base snapshot
        Map<String, Map<Long, Integer>> moduleEntityIndexMap = buildEntityIndexMap(baseData);

        // Copy base data and apply changes
        for (Map.Entry<String, Map<String, List<Float>>> moduleEntry : baseData.entrySet()) {
            String moduleName = moduleEntry.getKey();
            Map<String, List<Float>> moduleData = moduleEntry.getValue();
            Map<String, List<Float>> resultModuleData = new LinkedHashMap<>();

            Map<Long, Integer> entityIndexMap = moduleEntityIndexMap.getOrDefault(moduleName, Map.of());

            for (Map.Entry<String, List<Float>> componentEntry : moduleData.entrySet()) {
                String componentName = componentEntry.getKey();
                List<Float> values = new ArrayList<>(componentEntry.getValue());

                // Apply changes for this component
                Map<String, Map<Long, Float>> moduleChanges = delta.changedComponents().get(moduleName);
                if (moduleChanges != null) {
                    Map<Long, Float> componentChanges = moduleChanges.get(componentName);
                    if (componentChanges != null) {
                        for (Map.Entry<Long, Float> change : componentChanges.entrySet()) {
                            Long entityId = change.getKey();
                            Integer index = entityIndexMap.get(entityId);
                            if (index != null && index < values.size()) {
                                values.set(index, change.getValue());
                            }
                        }
                    }
                }

                resultModuleData.put(componentName, values);
            }

            resultData.put(moduleName, resultModuleData);
        }

        // Handle added entities - add them to the result
        // For added entities, we need to include their component values from the delta
        for (Long addedEntityId : delta.addedEntities()) {
            for (Map.Entry<String, Map<String, Map<Long, Float>>> moduleEntry : delta.changedComponents().entrySet()) {
                String moduleName = moduleEntry.getKey();
                Map<String, List<Float>> resultModuleData = resultData.computeIfAbsent(
                        moduleName, k -> new LinkedHashMap<>());

                // Add ENTITY_ID for this entity
                List<Float> entityIds = resultModuleData.computeIfAbsent(
                        ENTITY_ID_COMPONENT, k -> new ArrayList<>());
                entityIds.add(addedEntityId.floatValue());

                // Add component values for this entity
                for (Map.Entry<String, Map<Long, Float>> componentEntry : moduleEntry.getValue().entrySet()) {
                    String componentName = componentEntry.getKey();
                    Float value = componentEntry.getValue().get(addedEntityId);
                    if (value != null) {
                        List<Float> componentValues = resultModuleData.computeIfAbsent(
                                componentName, k -> new ArrayList<>());
                        componentValues.add(value);
                    }
                }
            }
        }

        // Handle removed entities - filter them out
        if (!delta.removedEntities().isEmpty()) {
            resultData = filterRemovedEntities(resultData, delta.removedEntities());
        }

        return Snapshot.fromLegacyFormat(resultData);
    }

    /**
     * Extract entity IDs from all modules in a snapshot.
     */
    private Set<Long> extractEntityIds(Map<String, Map<String, List<Float>>> snapshotData) {
        Set<Long> entityIds = new LinkedHashSet<>();

        for (Map<String, List<Float>> moduleData : snapshotData.values()) {
            List<Float> ids = moduleData.get(ENTITY_ID_COMPONENT);
            if (ids != null) {
                for (Float id : ids) {
                    if (id != null) {
                        entityIds.add(id.longValue());
                    }
                }
            }
        }

        return entityIds;
    }

    /**
     * Compute which component values changed between the two snapshots.
     */
    private Map<String, Map<String, Map<Long, Float>>> computeChangedComponents(
            Map<String, Map<String, List<Float>>> fromData,
            Map<String, Map<String, List<Float>>> toData,
            Set<Long> fromEntities,
            Set<Long> toEntities) {

        Map<String, Map<String, Map<Long, Float>>> changes = new LinkedHashMap<>();

        // Get all modules from both snapshots
        Set<String> allModules = new LinkedHashSet<>();
        allModules.addAll(fromData.keySet());
        allModules.addAll(toData.keySet());

        for (String moduleName : allModules) {
            Map<String, List<Float>> fromModuleData = fromData.getOrDefault(moduleName, Map.of());
            Map<String, List<Float>> toModuleData = toData.getOrDefault(moduleName, Map.of());

            // Build entity ID to index mappings for this module
            Map<Long, Integer> fromEntityIndex = buildEntityIndexForModule(fromModuleData);
            Map<Long, Integer> toEntityIndex = buildEntityIndexForModule(toModuleData);

            // Get all components from both
            Set<String> allComponents = new LinkedHashSet<>();
            allComponents.addAll(fromModuleData.keySet());
            allComponents.addAll(toModuleData.keySet());

            Map<String, Map<Long, Float>> moduleChanges = new LinkedHashMap<>();

            for (String componentName : allComponents) {
                List<Float> fromValues = fromModuleData.getOrDefault(componentName, List.of());
                List<Float> toValues = toModuleData.getOrDefault(componentName, List.of());

                Map<Long, Float> componentChanges = new LinkedHashMap<>();

                // Check all entities in the target snapshot
                for (Long entityId : toEntities) {
                    Integer fromIdx = fromEntityIndex.get(entityId);
                    Integer toIdx = toEntityIndex.get(entityId);

                    if (toIdx != null && toIdx < toValues.size()) {
                        Float toValue = toValues.get(toIdx);

                        // Check if value changed or entity is new
                        if (fromIdx == null || fromIdx >= fromValues.size()) {
                            // Entity is new to this module - include value
                            componentChanges.put(entityId, toValue);
                        } else {
                            Float fromValue = fromValues.get(fromIdx);
                            if (!Objects.equals(fromValue, toValue)) {
                                // Value changed
                                componentChanges.put(entityId, toValue);
                            }
                        }
                    }
                }

                if (!componentChanges.isEmpty()) {
                    moduleChanges.put(componentName, componentChanges);
                }
            }

            if (!moduleChanges.isEmpty()) {
                changes.put(moduleName, moduleChanges);
            }
        }

        return changes;
    }

    /**
     * Build a mapping of entity ID to array index for a single module's data.
     */
    private Map<Long, Integer> buildEntityIndexForModule(Map<String, List<Float>> moduleData) {
        Map<Long, Integer> indexMap = new LinkedHashMap<>();
        List<Float> entityIds = moduleData.get(ENTITY_ID_COMPONENT);

        if (entityIds != null) {
            for (int i = 0; i < entityIds.size(); i++) {
                Float id = entityIds.get(i);
                if (id != null) {
                    indexMap.put(id.longValue(), i);
                }
            }
        }

        return indexMap;
    }

    /**
     * Build entity ID to index mappings for all modules.
     */
    private Map<String, Map<Long, Integer>> buildEntityIndexMap(
            Map<String, Map<String, List<Float>>> snapshotData) {
        Map<String, Map<Long, Integer>> result = new LinkedHashMap<>();

        for (Map.Entry<String, Map<String, List<Float>>> entry : snapshotData.entrySet()) {
            result.put(entry.getKey(), buildEntityIndexForModule(entry.getValue()));
        }

        return result;
    }

    /**
     * Filter out removed entities from the result data.
     */
    private Map<String, Map<String, List<Float>>> filterRemovedEntities(
            Map<String, Map<String, List<Float>>> data,
            Set<Long> removedEntities) {

        Map<String, Map<String, List<Float>>> result = new LinkedHashMap<>();

        for (Map.Entry<String, Map<String, List<Float>>> moduleEntry : data.entrySet()) {
            String moduleName = moduleEntry.getKey();
            Map<String, List<Float>> moduleData = moduleEntry.getValue();

            // Find indices of entities to keep
            List<Float> entityIds = moduleData.get(ENTITY_ID_COMPONENT);
            if (entityIds == null) {
                result.put(moduleName, moduleData);
                continue;
            }

            List<Integer> keepIndices = new ArrayList<>();
            for (int i = 0; i < entityIds.size(); i++) {
                Float id = entityIds.get(i);
                if (id == null || !removedEntities.contains(id.longValue())) {
                    keepIndices.add(i);
                }
            }

            // Filter all component lists
            Map<String, List<Float>> filteredModuleData = new LinkedHashMap<>();
            for (Map.Entry<String, List<Float>> componentEntry : moduleData.entrySet()) {
                List<Float> values = componentEntry.getValue();
                List<Float> filteredValues = new ArrayList<>();
                for (Integer idx : keepIndices) {
                    if (idx < values.size()) {
                        filteredValues.add(values.get(idx));
                    }
                }
                filteredModuleData.put(componentEntry.getKey(), filteredValues);
            }

            result.put(moduleName, filteredModuleData);
        }

        return result;
    }
}
