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

package ca.samanthaireland.lightning.engine.acceptance.fixture;

import ca.samanthaireland.lightning.engine.api.resource.adapter.EngineClient;
import org.assertj.core.api.Assertions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fluent assertions for snapshot isolation verification.
 *
 * <p>Usage:
 * <pre>{@code
 * SnapshotAssertions.assertThat(snapshot)
 *     .hasModule("EntityModule")
 *     .hasEntityCount(2)
 *     .containsOnlyEntityIds(entity1, entity2);
 *
 * SnapshotAssertions.assertIsolation(snapshot1, snapshot2)
 *     .entitiesAreDisjoint()
 *     .haveUniqueIds();
 * }</pre>
 */
public class SnapshotAssertions {

    /**
     * Create assertions for a single snapshot.
     */
    public static SnapshotAssertion assertThat(EngineClient.Snapshot snapshot) {
        return new SnapshotAssertion(snapshot);
    }

    /**
     * Create isolation assertions between two snapshots.
     */
    public static IsolationAssertion assertIsolation(EngineClient.Snapshot snapshot1, EngineClient.Snapshot snapshot2) {
        return new IsolationAssertion(snapshot1, snapshot2);
    }

    /**
     * Assertions for a single snapshot.
     */
    public static class SnapshotAssertion {
        private final EngineClient.Snapshot snapshot;

        SnapshotAssertion(EngineClient.Snapshot snapshot) {
            this.snapshot = snapshot;
        }

        /**
         * Assert that the snapshot has a specific module.
         */
        public SnapshotAssertion hasModule(String moduleName) {
            Assertions.assertThat(snapshot.hasModule(moduleName))
                    .as("Snapshot should have module '%s'", moduleName)
                    .isTrue();
            return this;
        }

        /**
         * Assert that the snapshot has exactly the specified number of entities.
         */
        public SnapshotAssertion hasEntityCount(int count) {
            List<Float> entityIds = snapshot.entityIds();
            Assertions.assertThat(entityIds)
                    .as("Snapshot should have exactly %d entities", count)
                    .hasSize(count);
            return this;
        }

        /**
         * Assert that the snapshot contains only the specified entity IDs.
         */
        public SnapshotAssertion containsOnlyEntityIds(long... expectedIds) {
            List<Long> actualIds = snapshot.entityIds().stream()
                    .map(Float::longValue)
                    .toList();
            Assertions.assertThat(actualIds)
                    .as("Snapshot should contain only the specified entity IDs")
                    .containsExactlyInAnyOrder(box(expectedIds));
            return this;
        }

        /**
         * Assert that the snapshot does not contain the specified entity IDs.
         */
        public SnapshotAssertion doesNotContainEntityIds(long... entityIds) {
            List<Long> actualIds = snapshot.entityIds().stream()
                    .map(Float::longValue)
                    .toList();
            for (long entityId : entityIds) {
                Assertions.assertThat(actualIds)
                        .as("Snapshot should not contain entity ID %d", entityId)
                        .doesNotContain(entityId);
            }
            return this;
        }

        /**
         * Get the entity IDs as a list of longs.
         */
        public List<Long> entityIdList() {
            return snapshot.entityIds().stream()
                    .map(Float::longValue)
                    .toList();
        }

        /**
         * Get a component value for the first entity.
         */
        public float componentValue(String moduleName, String componentName) {
            return snapshot.module(moduleName).first(componentName, 0);
        }

        /**
         * Get all component values.
         */
        public List<Float> componentValues(String moduleName, String componentName) {
            return snapshot.module(moduleName).component(componentName);
        }

        /**
         * Start assertions for a specific entity by ID.
         */
        public EntityAssertion hasEntity(long entityId) {
            List<Long> ids = entityIdList();
            int index = ids.indexOf(entityId);
            Assertions.assertThat(index)
                    .as("Snapshot should contain entity %d", entityId)
                    .isGreaterThanOrEqualTo(0);
            return new EntityAssertion(snapshot, index, entityId);
        }

        /**
         * Start assertions for an entity at a specific index.
         */
        public EntityAssertion entityAt(int index) {
            List<Float> ids = snapshot.entityIds();
            Assertions.assertThat(index)
                    .as("Entity index should be valid")
                    .isBetween(0, ids.size() - 1);
            long entityId = ids.get(index).longValue();
            return new EntityAssertion(snapshot, index, entityId);
        }

        private Long[] box(long[] longs) {
            Long[] result = new Long[longs.length];
            for (int i = 0; i < longs.length; i++) {
                result[i] = longs[i];
            }
            return result;
        }
    }

    /**
     * Assertions for a specific entity within a snapshot.
     */
    public static class EntityAssertion {
        private final EngineClient.Snapshot snapshot;
        private final int index;
        private final long entityId;

        EntityAssertion(EngineClient.Snapshot snapshot, int index, long entityId) {
            this.snapshot = snapshot;
            this.index = index;
            this.entityId = entityId;
        }

        /**
         * Assert a component value for this entity.
         */
        public EntityAssertion withComponentValue(String moduleName, String componentName, float expected) {
            List<Float> values = snapshot.module(moduleName).component(componentName);
            Assertions.assertThat(values.size())
                    .as("Component %s.%s should have value at index %d", moduleName, componentName, index)
                    .isGreaterThan(index);
            Assertions.assertThat(values.get(index))
                    .as("Entity %d: %s.%s should equal %.2f", entityId, moduleName, componentName, expected)
                    .isEqualTo(expected);
            return this;
        }

        /**
         * Assert a component value is greater than expected.
         */
        public EntityAssertion withComponentGreaterThan(String moduleName, String componentName, float expected) {
            List<Float> values = snapshot.module(moduleName).component(componentName);
            Assertions.assertThat(values.size())
                    .as("Component %s.%s should have value at index %d", moduleName, componentName, index)
                    .isGreaterThan(index);
            Assertions.assertThat(values.get(index))
                    .as("Entity %d: %s.%s should be greater than %.2f", entityId, moduleName, componentName, expected)
                    .isGreaterThan(expected);
            return this;
        }

        /**
         * Assert a component value is less than expected.
         */
        public EntityAssertion withComponentLessThan(String moduleName, String componentName, float expected) {
            List<Float> values = snapshot.module(moduleName).component(componentName);
            Assertions.assertThat(values.size())
                    .as("Component %s.%s should have value at index %d", moduleName, componentName, index)
                    .isGreaterThan(index);
            Assertions.assertThat(values.get(index))
                    .as("Entity %d: %s.%s should be less than %.2f", entityId, moduleName, componentName, expected)
                    .isLessThan(expected);
            return this;
        }

        /**
         * Assert a component value is between two values.
         */
        public EntityAssertion withComponentBetween(String moduleName, String componentName, float min, float max) {
            List<Float> values = snapshot.module(moduleName).component(componentName);
            Assertions.assertThat(values.size())
                    .as("Component %s.%s should have value at index %d", moduleName, componentName, index)
                    .isGreaterThan(index);
            Assertions.assertThat(values.get(index))
                    .as("Entity %d: %s.%s should be between %.2f and %.2f", entityId, moduleName, componentName, min, max)
                    .isBetween(min, max);
            return this;
        }

        /**
         * Get the component value for this entity.
         */
        public float getComponentValue(String moduleName, String componentName) {
            List<Float> values = snapshot.module(moduleName).component(componentName);
            if (values.size() <= index) {
                return 0f;
            }
            return values.get(index);
        }

        /**
         * Return to the parent snapshot assertion.
         */
        public SnapshotAssertion and() {
            return SnapshotAssertions.assertThat(snapshot);
        }
    }

    /**
     * Assertions for isolation between two snapshots.
     */
    public static class IsolationAssertion {
        private final EngineClient.Snapshot snapshot1;
        private final EngineClient.Snapshot snapshot2;

        IsolationAssertion(EngineClient.Snapshot snapshot1, EngineClient.Snapshot snapshot2) {
            this.snapshot1 = snapshot1;
            this.snapshot2 = snapshot2;
        }

        /**
         * Assert that the entity IDs in both snapshots are completely disjoint.
         */
        public IsolationAssertion entitiesAreDisjoint() {
            Set<Long> ids1 = new HashSet<>(snapshot1.entityIds().stream()
                    .map(Float::longValue).toList());
            Set<Long> ids2 = new HashSet<>(snapshot2.entityIds().stream()
                    .map(Float::longValue).toList());

            Set<Long> intersection = new HashSet<>(ids1);
            intersection.retainAll(ids2);

            Assertions.assertThat(intersection)
                    .as("Entity IDs should not overlap between snapshots")
                    .isEmpty();
            return this;
        }

        /**
         * Assert that all entity IDs are unique (no duplicates within or across snapshots).
         */
        public IsolationAssertion haveUniqueIds() {
            List<Long> allIds = new java.util.ArrayList<>();
            allIds.addAll(snapshot1.entityIds().stream().map(Float::longValue).toList());
            allIds.addAll(snapshot2.entityIds().stream().map(Float::longValue).toList());

            Set<Long> uniqueIds = new HashSet<>(allIds);
            Assertions.assertThat(allIds)
                    .as("All entity IDs should be unique")
                    .hasSize(uniqueIds.size());
            return this;
        }
    }

    /**
     * Assert that all provided IDs are unique across all collections.
     */
    @SafeVarargs
    public static void assertAllIdsUnique(String description, List<Long>... idLists) {
        Set<Long> allIds = new HashSet<>();
        int totalCount = 0;

        for (List<Long> ids : idLists) {
            allIds.addAll(ids);
            totalCount += ids.size();
        }

        Assertions.assertThat(allIds)
                .as("%s: All IDs should be unique", description)
                .hasSize(totalCount);
    }
}
