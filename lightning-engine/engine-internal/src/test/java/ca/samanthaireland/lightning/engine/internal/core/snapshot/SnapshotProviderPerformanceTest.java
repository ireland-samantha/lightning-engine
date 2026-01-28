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
import ca.samanthaireland.lightning.engine.core.snapshot.Snapshot;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleResolver;
import ca.samanthaireland.lightning.engine.internal.core.store.ArrayEntityComponentStore;
import ca.samanthaireland.lightning.engine.internal.core.store.EcsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests for SnapshotProviderImpl.
 *
 * <p>These tests measure snapshot generation throughput and latency
 * for various entity counts to establish baselines and detect regressions.
 *
 * <p>Key performance targets (with CI variance tolerance):
 * <ul>
 *   <li>1000 entities: < 10ms per snapshot</li>
 *   <li>5000 entities: < 50ms per snapshot</li>
 *   <li>10000 entities: < 100ms per snapshot</li>
 * </ul>
 */
@Tag("performance")
@DisplayName("SnapshotProviderImpl Performance Tests")
class SnapshotProviderPerformanceTest {

    private ArrayEntityComponentStore store;
    private SnapshotProviderImpl snapshotProvider;
    private TestModuleResolver moduleResolver;

    // Test components (simulating a typical game module)
    private static final BaseComponent FLAG_COMPONENT = new TestComponent(1000, "TEST_FLAG");
    private static final BaseComponent POSITION_X = new TestComponent(1001, "POSITION_X");
    private static final BaseComponent POSITION_Y = new TestComponent(1002, "POSITION_Y");
    private static final BaseComponent POSITION_Z = new TestComponent(1003, "POSITION_Z");
    private static final BaseComponent VELOCITY_X = new TestComponent(1004, "VELOCITY_X");
    private static final BaseComponent VELOCITY_Y = new TestComponent(1005, "VELOCITY_Y");
    private static final BaseComponent VELOCITY_Z = new TestComponent(1006, "VELOCITY_Z");
    private static final BaseComponent HEALTH = new TestComponent(1007, "HEALTH");
    private static final BaseComponent MASS = new TestComponent(1008, "MASS");

    private static final List<BaseComponent> TEST_COMPONENTS = List.of(
            POSITION_X, POSITION_Y, POSITION_Z,
            VELOCITY_X, VELOCITY_Y, VELOCITY_Z,
            HEALTH, MASS
    );

    @BeforeEach
    void setUp() {
        store = new ArrayEntityComponentStore(new EcsProperties(100000, 100));
        moduleResolver = new TestModuleResolver();
        snapshotProvider = new SnapshotProviderImpl(store, moduleResolver);
    }

    @Nested
    @DisplayName("Snapshot Generation Performance")
    class SnapshotGenerationPerformance {

        @Test
        @DisplayName("Generate snapshot for 1000 entities (target: < 10ms)")
        void generateSnapshot_1000Entities() {
            int numEntities = 1000;
            long matchId = 1L;

            setupEntities(numEntities, matchId);

            // Warmup
            for (int i = 0; i < 10; i++) {
                snapshotProvider.createForMatch(matchId);
            }

            // Benchmark
            int iterations = 100;
            long startTime = System.nanoTime();

            for (int i = 0; i < iterations; i++) {
                Snapshot snapshot = snapshotProvider.createForMatch(matchId);
                assertThat(snapshot.isEmpty()).isFalse();
            }

            long elapsed = System.nanoTime() - startTime;
            double avgMs = (elapsed / 1_000_000.0) / iterations;
            double snapshotsPerSecond = iterations / (elapsed / 1_000_000_000.0);

            System.out.printf("Snapshot generation (%,d entities, %d iterations): %.3f ms avg (%.0f snapshots/sec)%n",
                    numEntities, iterations, avgMs, snapshotsPerSecond);

            assertThat(avgMs)
                    .as("Snapshot generation for 1000 entities should complete in < 10ms")
                    .isLessThan(10.0);
        }

        @Test
        @DisplayName("Generate snapshot for 5000 entities (target: < 50ms)")
        void generateSnapshot_5000Entities() {
            int numEntities = 5000;
            long matchId = 1L;

            setupEntities(numEntities, matchId);

            // Warmup
            for (int i = 0; i < 5; i++) {
                snapshotProvider.createForMatch(matchId);
            }

            // Benchmark
            int iterations = 50;
            long startTime = System.nanoTime();

            for (int i = 0; i < iterations; i++) {
                Snapshot snapshot = snapshotProvider.createForMatch(matchId);
                assertThat(snapshot.isEmpty()).isFalse();
            }

            long elapsed = System.nanoTime() - startTime;
            double avgMs = (elapsed / 1_000_000.0) / iterations;
            double snapshotsPerSecond = iterations / (elapsed / 1_000_000_000.0);

            System.out.printf("Snapshot generation (%,d entities, %d iterations): %.3f ms avg (%.0f snapshots/sec)%n",
                    numEntities, iterations, avgMs, snapshotsPerSecond);

            assertThat(avgMs)
                    .as("Snapshot generation for 5000 entities should complete in < 50ms")
                    .isLessThan(50.0);
        }

        @Test
        @DisplayName("Generate snapshot for 10000 entities (target: < 100ms)")
        void generateSnapshot_10000Entities() {
            int numEntities = 10000;
            long matchId = 1L;

            setupEntities(numEntities, matchId);

            // Warmup
            for (int i = 0; i < 3; i++) {
                snapshotProvider.createForMatch(matchId);
            }

            // Benchmark
            int iterations = 20;
            long startTime = System.nanoTime();

            for (int i = 0; i < iterations; i++) {
                Snapshot snapshot = snapshotProvider.createForMatch(matchId);
                assertThat(snapshot.isEmpty()).isFalse();
            }

            long elapsed = System.nanoTime() - startTime;
            double avgMs = (elapsed / 1_000_000.0) / iterations;
            double snapshotsPerSecond = iterations / (elapsed / 1_000_000_000.0);

            System.out.printf("Snapshot generation (%,d entities, %d iterations): %.3f ms avg (%.0f snapshots/sec)%n",
                    numEntities, iterations, avgMs, snapshotsPerSecond);

            assertThat(avgMs)
                    .as("Snapshot generation for 10000 entities should complete in < 100ms")
                    .isLessThan(100.0);
        }
    }

    @Nested
    @DisplayName("Match Isolation Performance")
    class MatchIsolationPerformance {

        @Test
        @DisplayName("Generate snapshot with multiple matches (only target match entities)")
        void generateSnapshot_multipleMatches() {
            int entitiesPerMatch = 2000;
            int numMatches = 5;
            long targetMatchId = 3L;

            // Setup entities across multiple matches
            for (int m = 1; m <= numMatches; m++) {
                setupEntities(entitiesPerMatch, m);
            }

            // Warmup
            for (int i = 0; i < 5; i++) {
                snapshotProvider.createForMatch(targetMatchId);
            }

            // Benchmark
            int iterations = 50;
            long startTime = System.nanoTime();

            Snapshot lastSnapshot = null;
            for (int i = 0; i < iterations; i++) {
                lastSnapshot = snapshotProvider.createForMatch(targetMatchId);
            }

            long elapsed = System.nanoTime() - startTime;
            double avgMs = (elapsed / 1_000_000.0) / iterations;

            // Verify correct filtering
            int entityCount = lastSnapshot.module("TestModule")
                    .flatMap(m -> m.component("ENTITY_ID"))
                    .map(c -> c.values().size())
                    .orElse(0);

            System.out.printf("Snapshot with match isolation (%,d total entities, %d matches, target match %d): %.3f ms avg, %d entities in snapshot%n",
                    entitiesPerMatch * numMatches, numMatches, targetMatchId, avgMs, entityCount);

            assertThat(entityCount)
                    .as("Snapshot should only contain entities from target match")
                    .isEqualTo(entitiesPerMatch);

            assertThat(avgMs)
                    .as("Match-filtered snapshot should complete in < 100ms")
                    .isLessThan(100.0);
        }
    }

    @Nested
    @DisplayName("Scaling Behavior")
    class ScalingBehavior {

        @Test
        @DisplayName("Verify linear scaling with entity count")
        void verifyLinearScaling() {
            int[] entityCounts = {500, 1000, 2000, 4000};
            double[] times = new double[entityCounts.length];
            long matchId = 1L;

            for (int i = 0; i < entityCounts.length; i++) {
                store.reset();
                setupEntities(entityCounts[i], matchId);

                // Warmup
                for (int w = 0; w < 5; w++) {
                    snapshotProvider.createForMatch(matchId);
                }

                // Benchmark
                int iterations = 20;
                long startTime = System.nanoTime();
                for (int j = 0; j < iterations; j++) {
                    snapshotProvider.createForMatch(matchId);
                }
                long elapsed = System.nanoTime() - startTime;
                times[i] = (elapsed / 1_000_000.0) / iterations;
            }

            System.out.println("Scaling behavior:");
            for (int i = 0; i < entityCounts.length; i++) {
                double timePerEntity = times[i] / entityCounts[i] * 1000; // microseconds per entity
                System.out.printf("  %,5d entities: %.3f ms (%.3f µs/entity)%n",
                        entityCounts[i], times[i], timePerEntity);
            }

            // Verify roughly linear scaling (allow 3x tolerance for overhead)
            double ratio = times[3] / times[0]; // 4000 vs 500 entities
            double expectedRatio = 8.0; // 4000/500 = 8x
            double tolerance = 3.0;

            System.out.printf("  Scaling ratio (4000/500): %.2fx (expected ~%.1fx)%n", ratio, expectedRatio);

            assertThat(ratio)
                    .as("Snapshot generation should scale roughly linearly")
                    .isLessThan(expectedRatio * tolerance);
        }
    }

    @Nested
    @DisplayName("Snapshot Data Correctness")
    class SnapshotDataCorrectness {

        @Test
        @DisplayName("Verify snapshot contains all expected components")
        void verifySnapshotContents() {
            int numEntities = 100;
            long matchId = 1L;

            setupEntities(numEntities, matchId);

            Snapshot snapshot = snapshotProvider.createForMatch(matchId);

            assertThat(snapshot.hasModule("TestModule")).isTrue();

            var moduleData = snapshot.module("TestModule").orElseThrow();

            // Should have ENTITY_ID plus all test components
            assertThat(moduleData.hasComponent("ENTITY_ID")).isTrue();
            assertThat(moduleData.component("ENTITY_ID").orElseThrow().values()).hasSize(numEntities);

            for (BaseComponent component : TEST_COMPONENTS) {
                assertThat(moduleData.hasComponent(component.getName()))
                        .as("Should contain component: " + component.getName())
                        .isTrue();
                assertThat(moduleData.component(component.getName()).orElseThrow().values())
                        .as("Component " + component.getName() + " should have values for all entities")
                        .hasSize(numEntities);
            }
        }
    }

    private void setupEntities(int count, long matchId) {
        Random random = new Random(42);
        float matchIdFloat = (float) matchId;

        for (int i = 0; i < count; i++) {
            long entityId = store.createEntityForMatch(matchId);

            // Attach flag component (required for module to recognize entity)
            store.attachComponent(entityId, FLAG_COMPONENT, 1.0f);

            // Attach all test components with random values
            store.attachComponent(entityId, POSITION_X, random.nextFloat() * 10000);
            store.attachComponent(entityId, POSITION_Y, random.nextFloat() * 10000);
            store.attachComponent(entityId, POSITION_Z, random.nextFloat() * 100);
            store.attachComponent(entityId, VELOCITY_X, random.nextFloat() * 200 - 100);
            store.attachComponent(entityId, VELOCITY_Y, random.nextFloat() * 200 - 100);
            store.attachComponent(entityId, VELOCITY_Z, random.nextFloat() * 20 - 10);
            store.attachComponent(entityId, HEALTH, 100.0f);
            store.attachComponent(entityId, MASS, 1.0f + random.nextFloat() * 10);
        }
    }

    /**
     * Test component implementation.
     */
    private static class TestComponent extends BaseComponent {
        TestComponent(long id, String name) {
            super(id, name);
        }
    }

    /**
     * Test module resolver that returns a single test module.
     */
    private static class TestModuleResolver implements ModuleResolver {
        private final TestModule testModule = new TestModule();

        @Override
        public EngineModule resolveModule(String moduleName) {
            return "TestModule".equals(moduleName) ? testModule : null;
        }

        @Override
        public List<String> getAvailableModules() {
            return List.of("TestModule");
        }

        @Override
        public List<EngineModule> resolveAllModules() {
            return List.of(testModule);
        }

        @Override
        public boolean hasModule(String moduleName) {
            return "TestModule".equals(moduleName);
        }
    }

    /**
     * Test module that provides test components.
     */
    private static class TestModule implements EngineModule {
        @Override
        public List<ca.samanthaireland.lightning.engine.core.system.EngineSystem> createSystems() {
            return List.of();
        }

        @Override
        public List<ca.samanthaireland.lightning.engine.core.command.EngineCommand> createCommands() {
            return List.of();
        }

        @Override
        public List<BaseComponent> createComponents() {
            return new ArrayList<>(TEST_COMPONENTS);
        }

        @Override
        public BaseComponent createFlagComponent() {
            return FLAG_COMPONENT;
        }

        @Override
        public String getName() {
            return "TestModule";
        }
    }
}
