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


package ca.samanthaireland.lightning.engine.internal.store;

import ca.samanthaireland.lightning.engine.internal.core.store.ArrayEntityComponentStore;
import ca.samanthaireland.lightning.engine.internal.core.store.CachedEntityComponentStore;
import ca.samanthaireland.lightning.engine.internal.core.store.EcsProperties;
import ca.samanthaireland.lightning.engine.internal.core.store.QueryCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests for the ECS ArrayEntityComponentStore.
 *
 * These tests measure throughput and latency for common ECS operations
 * to establish baselines and detect performance regressions.
 *
 * NOTE: The ArrayEntityComponentStore uses ReadWriteLock for thread-safety, which adds overhead
 * to all operations. The thresholds in these tests reflect realistic expectations
 * for a thread-safe ECS implementation.
 *
 * Key performance characteristics:
 * - Entity creation: ~20M+ ops/sec (single-threaded)
 * - Component read/write: ~4M+ ops/sec (single-threaded)
 * - Component queries (getEntitiesWithComponents): ~100-500 queries/sec depending on entity count
 *   (This is the most expensive operation due to full scan + lock contention)
 * - hasComponent checks: ~30M+ checks/sec (very fast hash lookup)
 *
 * Run with: mvn test -Dtest=ArrayEntityComponentStorePerformanceTest -pl ecs
 */
@DisplayName("ArrayEntityComponentStore Performance Tests")
class ArrayEntityComponentStorePerformanceTest {

    private static final int CARDINALITY = 10; // Components per entity
    private static final int WARMUP_ITERATIONS = 1000;

    // Component IDs
    private static final long POSITION_X = 0;
    private static final long POSITION_Y = 1;
    private static final long VELOCITY_X = 2;
    private static final long VELOCITY_Y = 3;
    private static final long HEALTH = 4;
    private static final long DAMAGE = 5;
    private static final long SPEED = 6;
    private static final long ARMOR = 7;
    private static final long TEAM_ID = 8;
    private static final long FLAGS = 9;

    private ArrayEntityComponentStore store;

    @BeforeEach
    void setUp() {
        store = new ArrayEntityComponentStore(new EcsProperties(100000, 100));
    }

    @Nested
    @DisplayName("Entity Creation Performance")
    class EntityCreation {

        @Test
        @DisplayName("Create 10,000 entities sequentially")
        void createEntities_10k() {
            int numEntities = 10_000;
            warmup(() -> {
                store.reset();
                for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                    store.createEntity(i);
                }
            });

            store.reset();
            long startTime = System.nanoTime();

            for (int i = 0; i < numEntities; i++) {
                store.createEntity(i);
            }

            long elapsed = System.nanoTime() - startTime;
            double opsPerSecond = numEntities / (elapsed / 1_000_000_000.0);
            double avgNanos = (double) elapsed / numEntities;

            System.out.printf("Create %,d entities: %.2f ms (%.0f ops/sec, %.2f ns/op)%n",
                    numEntities, elapsed / 1_000_000.0, opsPerSecond, avgNanos);

            assertThat(opsPerSecond).as("Should create at least 100k entities/sec").isGreaterThan(100_000);
        }

        @Test
        @DisplayName("Create 50,000 entities sequentially")
        void createEntities_50k() {
            int numEntities = 50_000;
            store.reset();

            long startTime = System.nanoTime();

            for (int i = 0; i < numEntities; i++) {
                store.createEntity(i);
            }

            long elapsed = System.nanoTime() - startTime;
            double opsPerSecond = numEntities / (elapsed / 1_000_000_000.0);

            System.out.printf("Create %,d entities: %.2f ms (%.0f ops/sec)%n",
                    numEntities, elapsed / 1_000_000.0, opsPerSecond);

            assertThat(opsPerSecond).as("Should create at least 50k entities/sec").isGreaterThan(50_000);
        }

        @Test
        @DisplayName("Create and delete cycle (entity recycling)")
        void createDeleteCycle() {
            int cycles = 10_000;

            // Pre-fill to trigger recycling
            for (int i = 0; i < 1000; i++) {
                store.createEntity(i);
            }
            for (int i = 0; i < 1000; i++) {
                store.deleteEntity(i);
            }

            long startTime = System.nanoTime();

            for (int i = 0; i < cycles; i++) {
                long id = 10000 + i;
                store.createEntity(id);
                store.deleteEntity(id);
            }

            long elapsed = System.nanoTime() - startTime;
            double cyclesPerSecond = cycles / (elapsed / 1_000_000_000.0);

            System.out.printf("Create/delete %,d cycles: %.2f ms (%.0f cycles/sec)%n",
                    cycles, elapsed / 1_000_000.0, cyclesPerSecond);

            assertThat(cyclesPerSecond).as("Should handle at least 50k create/delete cycles/sec").isGreaterThan(50_000);
        }
    }

    @Nested
    @DisplayName("Component Operations Performance")
    class ComponentOperations {

        @Test
        @DisplayName("Attach single component to 10,000 entities")
        void attachSingleComponent_10k() {
            int numEntities = 10_000;

            // Setup: create entities
            for (int i = 0; i < numEntities; i++) {
                store.createEntity(i);
            }

            long startTime = System.nanoTime();

            for (int i = 0; i < numEntities; i++) {
                store.attachComponent(i, POSITION_X, i * 100);
            }

            long elapsed = System.nanoTime() - startTime;
            double opsPerSecond = numEntities / (elapsed / 1_000_000_000.0);

            System.out.printf("Attach single component to %,d entities: %.2f ms (%.0f ops/sec)%n",
                    numEntities, elapsed / 1_000_000.0, opsPerSecond);

            assertThat(opsPerSecond).as("Should attach at least 200k components/sec").isGreaterThan(200_000);
        }

        @Test
        @DisplayName("Attach multiple components (batch) to 10,000 entities")
        void attachMultipleComponents_10k() {
            int numEntities = 10_000;
            long[] componentIds = {POSITION_X, POSITION_Y, VELOCITY_X, VELOCITY_Y};
            float[] values = new float[4];

            // Setup: create entities
            for (int i = 0; i < numEntities; i++) {
                store.createEntity(i);
            }

            long startTime = System.nanoTime();

            for (int i = 0; i < numEntities; i++) {
                values[0] = i * 10;
                values[1] = i * 20;
                values[2] = i;
                values[3] = i * 2;
                store.attachComponents(i, componentIds, values);
            }

            long elapsed = System.nanoTime() - startTime;
            double opsPerSecond = numEntities / (elapsed / 1_000_000_000.0);

            System.out.printf("Attach 4 components (batch) to %,d entities: %.2f ms (%.0f ops/sec)%n",
                    numEntities, elapsed / 1_000_000.0, opsPerSecond);

            assertThat(opsPerSecond).as("Should attach batch at least 100k entities/sec").isGreaterThan(100_000);
        }

        @Test
        @DisplayName("Get single component from 10,000 entities")
        void getSingleComponent_10k() {
            int numEntities = 10_000;

            // Setup: create entities with component
            for (int i = 0; i < numEntities; i++) {
                store.createEntity(i);
                store.attachComponent(i, POSITION_X, i * 100);
            }

            long sum = 0;
            long startTime = System.nanoTime();

            for (int i = 0; i < numEntities; i++) {
                sum += store.getComponent(i, POSITION_X);
            }

            long elapsed = System.nanoTime() - startTime;
            double opsPerSecond = numEntities / (elapsed / 1_000_000_000.0);

            System.out.printf("Get single component from %,d entities: %.2f ms (%.0f ops/sec, checksum=%d)%n",
                    numEntities, elapsed / 1_000_000.0, opsPerSecond, sum % 1000);

            assertThat(opsPerSecond).as("Should read at least 500k components/sec").isGreaterThan(500_000);
        }

        @Test
        @DisplayName("Get multiple components (batch) from 10,000 entities")
        void getMultipleComponents_10k() {
            int numEntities = 10_000;
            long[] componentIds = {POSITION_X, POSITION_Y, VELOCITY_X, VELOCITY_Y};
            float[] buffer = new float[4];

            // Setup: create entities with components
            for (int i = 0; i < numEntities; i++) {
                store.createEntity(i);
                store.attachComponents(i, componentIds, new float[]{i, i * 2, i * 3, i * 4});
            }

            long sum = 0;
            long startTime = System.nanoTime();

            for (int i = 0; i < numEntities; i++) {
                store.getComponents(i, componentIds, buffer);
                sum += buffer[0] + buffer[1] + buffer[2] + buffer[3];
            }

            long elapsed = System.nanoTime() - startTime;
            double opsPerSecond = numEntities / (elapsed / 1_000_000_000.0);

            System.out.printf("Get 4 components (batch) from %,d entities: %.2f ms (%.0f ops/sec, checksum=%d)%n",
                    numEntities, elapsed / 1_000_000.0, opsPerSecond, sum % 1000);

            assertThat(opsPerSecond).as("Should read batch at least 200k entities/sec").isGreaterThan(200_000);
        }
    }

    @Nested
    @DisplayName("Query Performance")
    class QueryPerformance {

        @Test
        @DisplayName("Query entities with single component (10k entities, 50% match)")
        void queryEntitiesWithSingleComponent() {
            int numEntities = 10_000;

            // Setup: create entities, half with component
            for (int i = 0; i < numEntities; i++) {
                store.createEntity(i);
                if (i % 2 == 0) {
                    store.attachComponent(i, HEALTH, 100);
                }
            }

            int iterations = 100;
            long startTime = System.nanoTime();

            Set<Long> result = null;
            for (int i = 0; i < iterations; i++) {
                result = store.getEntitiesWithComponents(HEALTH);
            }

            long elapsed = System.nanoTime() - startTime;
            double queriesPerSecond = iterations / (elapsed / 1_000_000_000.0);
            double avgMs = (elapsed / 1_000_000.0) / iterations;

            System.out.printf("Query single component (%,d entities, 50%% match): %.3f ms avg (%.0f queries/sec, found %d)%n",
                    numEntities, avgMs, queriesPerSecond, result.size());

            assertThat(result).hasSize(numEntities / 2);
            assertThat(queriesPerSecond).as("Should handle at least 100 queries/sec").isGreaterThan(100);
        }

        @Test
        @DisplayName("Query entities with multiple components (10k entities, 25% match)")
        void queryEntitiesWithMultipleComponents() {
            int numEntities = 10_000;

            // Setup: create entities with varying component combinations
            for (int i = 0; i < numEntities; i++) {
                store.createEntity(i);
                if (i % 2 == 0) {
                    store.attachComponent(i, POSITION_X, i);
                    store.attachComponent(i, POSITION_Y, i);
                }
                if (i % 4 == 0) {
                    store.attachComponent(i, VELOCITY_X, i);
                    store.attachComponent(i, VELOCITY_Y, i);
                }
            }

            int iterations = 100;
            long startTime = System.nanoTime();

            Set<Long> result = null;
            for (int i = 0; i < iterations; i++) {
                result = store.getEntitiesWithComponents(POSITION_X, POSITION_Y, VELOCITY_X, VELOCITY_Y);
            }

            long elapsed = System.nanoTime() - startTime;
            double queriesPerSecond = iterations / (elapsed / 1_000_000_000.0);
            double avgMs = (elapsed / 1_000_000.0) / iterations;

            System.out.printf("Query 4 components (%,d entities, 25%% match): %.3f ms avg (%.0f queries/sec, found %d)%n",
                    numEntities, avgMs, queriesPerSecond, result.size());

            assertThat(result).hasSize(numEntities / 4);
            assertThat(queriesPerSecond).as("Should handle at least 50 queries/sec").isGreaterThan(50);
        }

        @Test
        @DisplayName("hasComponent check performance (10k entities)")
        void hasComponentPerformance() {
            int numEntities = 10_000;

            // Setup: create entities, half with component
            for (int i = 0; i < numEntities; i++) {
                store.createEntity(i);
                if (i % 2 == 0) {
                    store.attachComponent(i, HEALTH, 100);
                }
            }

            int count = 0;
            long startTime = System.nanoTime();

            for (int i = 0; i < numEntities; i++) {
                if (store.hasComponent(i, HEALTH)) {
                    count++;
                }
            }

            long elapsed = System.nanoTime() - startTime;
            double checksPerSecond = numEntities / (elapsed / 1_000_000_000.0);

            System.out.printf("hasComponent check on %,d entities: %.2f ms (%.0f checks/sec, found %d)%n",
                    numEntities, elapsed / 1_000_000.0, checksPerSecond, count);

            assertThat(count).isEqualTo(numEntities / 2);
            assertThat(checksPerSecond).as("Should check at least 500k entities/sec").isGreaterThan(500_000);
        }
    }

    @Nested
    @DisplayName("Game Loop Simulation")
    class GameLoopSimulation {

        @Test
        @DisplayName("Simulate movement system tick (1000 entities)")
        void simulateMovementTick_1k() {
            int numEntities = 1_000;
            long[] posComponents = {POSITION_X, POSITION_Y};
            long[] velComponents = {VELOCITY_X, VELOCITY_Y};
            float[] posBuffer = new float[2];
            float[] velBuffer = new float[2];
            Random random = new Random(42);

            // Setup: create entities with position and velocity
            for (int i = 0; i < numEntities; i++) {
                store.createEntity(i);
                store.attachComponents(i, posComponents, new float[]{random.nextInt(1000), random.nextInt(1000)});
                store.attachComponents(i, velComponents, new float[]{random.nextInt(10) - 5, random.nextInt(10) - 5});
            }

            int ticks = 1000;
            long startTime = System.nanoTime();

            for (int tick = 0; tick < ticks; tick++) {
                Set<Long> moveables = store.getEntitiesWithComponents(POSITION_X, POSITION_Y, VELOCITY_X, VELOCITY_Y);
                for (long entityId : moveables) {
                    store.getComponents(entityId, posComponents, posBuffer);
                    store.getComponents(entityId, velComponents, velBuffer);

                    // Update position: pos += vel
                    posBuffer[0] += velBuffer[0];
                    posBuffer[1] += velBuffer[1];

                    store.attachComponents(entityId, posComponents, posBuffer);
                }
            }

            long elapsed = System.nanoTime() - startTime;
            double ticksPerSecond = ticks / (elapsed / 1_000_000_000.0);
            double avgMs = (elapsed / 1_000_000.0) / ticks;

            System.out.printf("Movement system (%,d entities, %,d ticks): %.3f ms/tick (%.0f ticks/sec)%n",
                    numEntities, ticks, avgMs, ticksPerSecond);

            // At 60 FPS, we need < 16.67ms per frame for all systems
            // Movement should take small fraction of that
            assertThat(avgMs).as("Movement tick should complete in < 5ms").isLessThan(5.0);
        }

        @Test
        @DisplayName("Simulate movement system tick (10,000 entities)")
        void simulateMovementTick_10k() {
            int numEntities = 10_000;
            long[] posComponents = {POSITION_X, POSITION_Y};
            long[] velComponents = {VELOCITY_X, VELOCITY_Y};
            float[] posBuffer = new float[2];
            float[] velBuffer = new float[2];
            Random random = new Random(42);

            // Setup: create entities with position and velocity
            for (int i = 0; i < numEntities; i++) {
                store.createEntity(i);
                store.attachComponents(i, posComponents, new float[]{random.nextInt(1000), random.nextInt(1000)});
                store.attachComponents(i, velComponents, new float[]{random.nextInt(10) - 5, random.nextInt(10) - 5});
            }

            int ticks = 100;
            long startTime = System.nanoTime();

            for (int tick = 0; tick < ticks; tick++) {
                Set<Long> moveables = store.getEntitiesWithComponents(POSITION_X, POSITION_Y, VELOCITY_X, VELOCITY_Y);
                for (long entityId : moveables) {
                    store.getComponents(entityId, posComponents, posBuffer);
                    store.getComponents(entityId, velComponents, velBuffer);

                    posBuffer[0] += velBuffer[0];
                    posBuffer[1] += velBuffer[1];

                    store.attachComponents(entityId, posComponents, posBuffer);
                }
            }

            long elapsed = System.nanoTime() - startTime;
            double ticksPerSecond = ticks / (elapsed / 1_000_000_000.0);
            double avgMs = (elapsed / 1_000_000.0) / ticks;

            System.out.printf("Movement system (%,d entities, %,d ticks): %.3f ms/tick (%.0f ticks/sec)%n",
                    numEntities, ticks, avgMs, ticksPerSecond);

            // With 10k entities and thread-safe locking, allow up to 50ms per tick
            // Note: For 60 FPS with 10k entities, consider optimizations like:
            // - Caching query results within a tick
            // - Using single-threaded mode when possible
            // - Batching operations
            assertThat(avgMs).as("Movement tick should complete in < 50ms").isLessThan(50.0);
        }

        @Test
        @DisplayName("Simulate full game tick with multiple systems")
        void simulateFullGameTick() {
            int numEntities = 5_000;
            Random random = new Random(42);

            // Setup: create diverse entities
            for (int i = 0; i < numEntities; i++) {
                store.createEntity(i);

                // All entities have position
                store.attachComponent(i, POSITION_X, random.nextInt(1000));
                store.attachComponent(i, POSITION_Y, random.nextInt(1000));

                // 80% have velocity (moveable)
                if (i % 5 != 0) {
                    store.attachComponent(i, VELOCITY_X, random.nextInt(10) - 5);
                    store.attachComponent(i, VELOCITY_Y, random.nextInt(10) - 5);
                }

                // 60% have health (damageable)
                if (i % 5 < 3) {
                    store.attachComponent(i, HEALTH, 100);
                }

                // 40% have damage (attackers)
                if (i % 5 < 2) {
                    store.attachComponent(i, DAMAGE, 10 + random.nextInt(20));
                }

                // All have team
                store.attachComponent(i, TEAM_ID, i % 4);
            }

            int ticks = 100;
            long[] posComponents = {POSITION_X, POSITION_Y};
            long[] velComponents = {VELOCITY_X, VELOCITY_Y};
            float[] posBuffer = new float[2];
            float[] velBuffer = new float[2];

            List<Long> tickTimes = new ArrayList<>();
            long totalStartTime = System.nanoTime();

            for (int tick = 0; tick < ticks; tick++) {
                long tickStart = System.nanoTime();

                // System 1: Movement
                Set<Long> moveables = store.getEntitiesWithComponents(POSITION_X, POSITION_Y, VELOCITY_X, VELOCITY_Y);
                for (long entityId : moveables) {
                    store.getComponents(entityId, posComponents, posBuffer);
                    store.getComponents(entityId, velComponents, velBuffer);
                    posBuffer[0] += velBuffer[0];
                    posBuffer[1] += velBuffer[1];
                    store.attachComponents(entityId, posComponents, posBuffer);
                }

                // System 2: Query attackers
                Set<Long> attackers = store.getEntitiesWithComponents(DAMAGE, POSITION_X, POSITION_Y);

                // System 3: Query targets
                Set<Long> targets = store.getEntitiesWithComponents(HEALTH, POSITION_X, POSITION_Y);

                // System 4: Simple collision/damage check (simplified)
                for (long attacker : attackers) {
                    float attackerX = store.getComponent(attacker, POSITION_X);
                    float attackerY = store.getComponent(attacker, POSITION_Y);
                    float damage = store.getComponent(attacker, DAMAGE);
                    float attackerTeam = store.getComponent(attacker, TEAM_ID);

                    for (long target : targets) {
                        if (attacker == target) continue;

                        float targetTeam = store.getComponent(target, TEAM_ID);
                        if (attackerTeam == targetTeam) continue;

                        float targetX = store.getComponent(target, POSITION_X);
                        float targetY = store.getComponent(target, POSITION_Y);

                        // Simple distance check (Manhattan)
                        if (Math.abs(attackerX - targetX) + Math.abs(attackerY - targetY) < 10) {
                            float health = store.getComponent(target, HEALTH);
                            store.attachComponent(target, HEALTH, Math.max(0, health - damage));
                        }
                    }
                }

                tickTimes.add(System.nanoTime() - tickStart);
            }

            long totalElapsed = System.nanoTime() - totalStartTime;
            double avgMs = (totalElapsed / 1_000_000.0) / ticks;
            double maxMs = tickTimes.stream().mapToLong(l -> l).max().orElse(0) / 1_000_000.0;
            double minMs = tickTimes.stream().mapToLong(l -> l).min().orElse(0) / 1_000_000.0;
            double ticksPerSecond = ticks / (totalElapsed / 1_000_000_000.0);

            System.out.printf("Full game tick (%,d entities, %,d ticks): avg=%.3f ms, min=%.3f ms, max=%.3f ms (%.0f ticks/sec)%n",
                    numEntities, ticks, avgMs, minMs, maxMs, ticksPerSecond);

            // With thread-safe locking and O(n²) combat, allow higher budget
            // Real games would optimize combat with spatial partitioning
            assertThat(avgMs).as("Full game tick should complete in < 500ms").isLessThan(500.0);
        }
    }

    @Nested
    @DisplayName("Concurrent Performance")
    class ConcurrentPerformance {

        @Test
        @DisplayName("Concurrent reads while writing (reader/writer scenario)")
        void concurrentReadsWhileWriting() throws InterruptedException {
            int numEntities = 5_000;
            int testDurationMs = 2000;

            // Setup: create entities
            for (int i = 0; i < numEntities; i++) {
                store.createEntity(i);
                store.attachComponent(i, POSITION_X, i);
                store.attachComponent(i, POSITION_Y, i * 2);
            }

            AtomicLong readOps = new AtomicLong(0);
            AtomicLong writeOps = new AtomicLong(0);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch stopLatch = new CountDownLatch(1);

            // Reader threads (4)
            Thread[] readers = new Thread[4];
            for (int r = 0; r < readers.length; r++) {
                readers[r] = new Thread(() -> {
                    try {
                        startLatch.await();
                        Random random = new Random();
                        while (stopLatch.getCount() > 0) {
                            int entityId = random.nextInt(numEntities);
                            store.getComponent(entityId, POSITION_X);
                            store.getComponent(entityId, POSITION_Y);
                            readOps.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                readers[r].start();
            }

            // Writer threads (2)
            Thread[] writers = new Thread[2];
            for (int w = 0; w < writers.length; w++) {
                writers[w] = new Thread(() -> {
                    try {
                        startLatch.await();
                        Random random = new Random();
                        while (stopLatch.getCount() > 0) {
                            int entityId = random.nextInt(numEntities);
                            store.attachComponent(entityId, POSITION_X, random.nextInt(1000));
                            store.attachComponent(entityId, POSITION_Y, random.nextInt(1000));
                            writeOps.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                writers[w].start();
            }

            // Run test
            startLatch.countDown();
            Thread.sleep(testDurationMs);
            stopLatch.countDown();

            // Wait for threads
            for (Thread t : readers) t.join(1000);
            for (Thread t : writers) t.join(1000);

            double readsPerSecond = readOps.get() / (testDurationMs / 1000.0);
            double writesPerSecond = writeOps.get() / (testDurationMs / 1000.0);

            System.out.printf("Concurrent read/write (%d readers, %d writers, %d ms): %.0f reads/sec, %.0f writes/sec%n",
                    readers.length, writers.length, testDurationMs, readsPerSecond, writesPerSecond);

            assertThat(readsPerSecond).as("Should achieve at least 100k reads/sec under contention").isGreaterThan(100_000);
            assertThat(writesPerSecond).as("Should achieve at least 50k writes/sec under contention").isGreaterThan(50_000);
        }

        @Test
        @DisplayName("Concurrent entity creation from multiple threads")
        void concurrentEntityCreation() throws InterruptedException {
            int numThreads = 8;
            int entitiesPerThread = 5_000;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numThreads);

            Thread[] threads = new Thread[numThreads];
            for (int t = 0; t < numThreads; t++) {
                final int threadId = t;
                threads[t] = new Thread(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < entitiesPerThread; i++) {
                            long entityId = threadId * 100_000L + i;
                            store.createEntity(entityId);
                            store.attachComponent(entityId, POSITION_X, i);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
                threads[t].start();
            }

            long startTime = System.nanoTime();
            startLatch.countDown();
            doneLatch.await();
            long elapsed = System.nanoTime() - startTime;

            int totalEntities = numThreads * entitiesPerThread;
            double entitiesPerSecond = totalEntities / (elapsed / 1_000_000_000.0);

            System.out.printf("Concurrent entity creation (%d threads, %,d total): %.2f ms (%.0f entities/sec)%n",
                    numThreads, totalEntities, elapsed / 1_000_000.0, entitiesPerSecond);

            assertThat(entitiesPerSecond).as("Should create at least 100k entities/sec concurrently").isGreaterThan(100_000);
        }

        @Test
        @DisplayName("Concurrent queries from multiple threads")
        void concurrentQueries() throws InterruptedException {
            int numEntities = 10_000;

            // Setup: create diverse entities
            for (int i = 0; i < numEntities; i++) {
                store.createEntity(i);
                store.attachComponent(i, POSITION_X, i);
                store.attachComponent(i, POSITION_Y, i * 2);
                if (i % 2 == 0) {
                    store.attachComponent(i, VELOCITY_X, i);
                    store.attachComponent(i, VELOCITY_Y, i);
                }
                if (i % 3 == 0) {
                    store.attachComponent(i, HEALTH, 100);
                }
            }

            int numThreads = 4;
            int queriesPerThread = 100;
            AtomicLong totalResults = new AtomicLong(0);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numThreads);

            Thread[] threads = new Thread[numThreads];
            for (int t = 0; t < numThreads; t++) {
                final int threadId = t;
                threads[t] = new Thread(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < queriesPerThread; i++) {
                            Set<Long> result;
                            switch (threadId % 3) {
                                case 0 -> result = store.getEntitiesWithComponents(POSITION_X, POSITION_Y);
                                case 1 -> result = store.getEntitiesWithComponents(VELOCITY_X, VELOCITY_Y);
                                default -> result = store.getEntitiesWithComponents(HEALTH);
                            }
                            totalResults.addAndGet(result.size());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
                threads[t].start();
            }

            long startTime = System.nanoTime();
            startLatch.countDown();
            doneLatch.await();
            long elapsed = System.nanoTime() - startTime;

            int totalQueries = numThreads * queriesPerThread;
            double queriesPerSecond = totalQueries / (elapsed / 1_000_000_000.0);

            System.out.printf("Concurrent queries (%d threads, %d queries): %.2f ms (%.0f queries/sec)%n",
                    numThreads, totalQueries, elapsed / 1_000_000.0, queriesPerSecond);

            // With read lock contention, concurrent queries are slower
            // The vectorsWithComponent operation holds read lock for entire scan
            assertThat(queriesPerSecond).as("Should handle at least 50 concurrent queries/sec").isGreaterThan(50);
        }
    }

    @Nested
    @DisplayName("Cached Query Performance")
    class CachedQueryPerformance {

        @Test
        @DisplayName("Cached vs uncached query comparison (10k entities)")
        void cachedVsUncachedQueryComparison() {
            int numEntities = 10_000;
            CachedEntityComponentStore cachedStore = new CachedEntityComponentStore(store);
            QueryCache cache = cachedStore.getCache();

            // Setup: create entities with components
            for (int i = 0; i < numEntities; i++) {
                store.createEntity(i);
                store.attachComponent(i, POSITION_X, i);
                store.attachComponent(i, POSITION_Y, i * 2);
                if (i % 2 == 0) {
                    store.attachComponent(i, VELOCITY_X, i);
                    store.attachComponent(i, VELOCITY_Y, i);
                }
            }

            int iterations = 1000;

            // Benchmark uncached
            long uncachedStart = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                store.getEntitiesWithComponents(POSITION_X, POSITION_Y, VELOCITY_X, VELOCITY_Y);
            }
            long uncachedElapsed = System.nanoTime() - uncachedStart;
            double uncachedAvgMs = (uncachedElapsed / 1_000_000.0) / iterations;

            // Benchmark cached (first call is cache miss, rest are hits)
            cache.clear();
            long cachedStart = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                cachedStore.getEntitiesWithComponents(POSITION_X, POSITION_Y, VELOCITY_X, VELOCITY_Y);
            }
            long cachedElapsed = System.nanoTime() - cachedStart;
            double cachedAvgMs = (cachedElapsed / 1_000_000.0) / iterations;

            double speedup = uncachedAvgMs / cachedAvgMs;

            System.out.printf("Query comparison (%,d entities, %d iterations):%n", numEntities, iterations);
            System.out.printf("  Uncached: %.4f ms/query%n", uncachedAvgMs);
            System.out.printf("  Cached:   %.4f ms/query%n", cachedAvgMs);
            System.out.printf("  Speedup:  %.1fx%n", speedup);
            System.out.printf("  Cache hits: %d, misses: %d, ratio: %.2f%%%n",
                    cache.getHitCount(), cache.getMissCount(), cache.getHitRatio() * 100);

            // Cached should be significantly faster (at least 10x for warm cache)
            assertThat(speedup).as("Cached queries should be at least 10x faster").isGreaterThan(10.0);
            assertThat(cache.getHitCount()).as("Should have cache hits").isEqualTo(iterations - 1);
            assertThat(cache.getMissCount()).as("Should have exactly one cache miss").isEqualTo(1);
        }

        @Test
        @DisplayName("Cache performance with different query patterns")
        void cachePerformanceWithDifferentPatterns() {
            int numEntities = 10_000;
            CachedEntityComponentStore cachedStore = new CachedEntityComponentStore(store);
            QueryCache cache = cachedStore.getCache();

            // Setup: create diverse entities
            for (int i = 0; i < numEntities; i++) {
                store.createEntity(i);
                store.attachComponent(i, POSITION_X, i);
                store.attachComponent(i, POSITION_Y, i * 2);
                if (i % 2 == 0) {
                    store.attachComponent(i, VELOCITY_X, i);
                    store.attachComponent(i, VELOCITY_Y, i);
                }
                if (i % 3 == 0) {
                    store.attachComponent(i, HEALTH, 100);
                }
                if (i % 4 == 0) {
                    store.attachComponent(i, DAMAGE, 10);
                }
            }

            int iterations = 500;

            // Multiple different queries, each repeated
            long startTime = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                // Query 1: Movement entities
                cachedStore.getEntitiesWithComponents(POSITION_X, POSITION_Y, VELOCITY_X, VELOCITY_Y);
                // Query 2: Damageable entities
                cachedStore.getEntitiesWithComponents(HEALTH);
                // Query 3: Attackers
                cachedStore.getEntitiesWithComponents(DAMAGE, POSITION_X, POSITION_Y);
                // Query 4: All positioned entities
                cachedStore.getEntitiesWithComponents(POSITION_X, POSITION_Y);
            }
            long elapsed = System.nanoTime() - startTime;

            int totalQueries = iterations * 4;
            double avgMs = (elapsed / 1_000_000.0) / totalQueries;
            double queriesPerSecond = totalQueries / (elapsed / 1_000_000_000.0);

            System.out.printf("Multiple query patterns (%,d entities, %d total queries):%n", numEntities, totalQueries);
            System.out.printf("  Average: %.4f ms/query (%.0f queries/sec)%n", avgMs, queriesPerSecond);
            System.out.printf("  Cache hits: %d, misses: %d, ratio: %.2f%%%n",
                    cache.getHitCount(), cache.getMissCount(), cache.getHitRatio() * 100);

            // With 4 unique queries repeated 500 times, should have 4 misses and 1996 hits
            assertThat(cache.getMissCount()).as("Should have 4 cache misses (one per unique query)").isEqualTo(4);
            assertThat(cache.getHitRatio()).as("Hit ratio should be > 99%").isGreaterThan(0.99);
            assertThat(queriesPerSecond).as("Should achieve at least 10k queries/sec with warm cache").isGreaterThan(10_000);
        }

        @Test
        @DisplayName("Game loop with cached queries (10k entities)")
        void gameLoopWithCachedQueries() {
            int numEntities = 10_000;
            CachedEntityComponentStore cachedStore = new CachedEntityComponentStore(store);
            QueryCache cache = cachedStore.getCache();
            long[] posComponents = {POSITION_X, POSITION_Y};
            long[] velComponents = {VELOCITY_X, VELOCITY_Y};
            float[] posBuffer = new float[2];
            float[] velBuffer = new float[2];
            Random random = new Random(42);

            // Setup: create entities with position and velocity
            for (int i = 0; i < numEntities; i++) {
                store.createEntity(i);
                store.attachComponents(i, posComponents, new float[]{random.nextInt(1000), random.nextInt(1000)});
                store.attachComponents(i, velComponents, new float[]{random.nextInt(10) - 5, random.nextInt(10) - 5});
            }

            int ticks = 100;

            // Benchmark WITHOUT cache (existing behavior)
            long uncachedStart = System.nanoTime();
            for (int tick = 0; tick < ticks; tick++) {
                Set<Long> moveables = store.getEntitiesWithComponents(POSITION_X, POSITION_Y, VELOCITY_X, VELOCITY_Y);
                for (long entityId : moveables) {
                    store.getComponents(entityId, posComponents, posBuffer);
                    store.getComponents(entityId, velComponents, velBuffer);
                    posBuffer[0] += velBuffer[0];
                    posBuffer[1] += velBuffer[1];
                    store.attachComponents(entityId, posComponents, posBuffer);
                }
            }
            long uncachedElapsed = System.nanoTime() - uncachedStart;
            double uncachedAvgMs = (uncachedElapsed / 1_000_000.0) / ticks;

            // Reset positions
            for (int i = 0; i < numEntities; i++) {
                store.attachComponents(i, posComponents, new float[]{random.nextInt(1000), random.nextInt(1000)});
            }

            // Benchmark WITH cache (per-tick pattern)
            cache.resetStats();
            long cachedStart = System.nanoTime();
            for (int tick = 0; tick < ticks; tick++) {
                cache.beginTick(); // Clear cache each tick
                Set<Long> moveables = cachedStore.getEntitiesWithComponents(POSITION_X, POSITION_Y, VELOCITY_X, VELOCITY_Y);
                for (long entityId : moveables) {
                    store.getComponents(entityId, posComponents, posBuffer);
                    store.getComponents(entityId, velComponents, velBuffer);
                    posBuffer[0] += velBuffer[0];
                    posBuffer[1] += velBuffer[1];
                    store.attachComponents(entityId, posComponents, posBuffer);
                }
            }
            long cachedPerTickElapsed = System.nanoTime() - cachedStart;
            double cachedPerTickAvgMs = (cachedPerTickElapsed / 1_000_000.0) / ticks;

            // Reset positions again
            for (int i = 0; i < numEntities; i++) {
                store.attachComponents(i, posComponents, new float[]{random.nextInt(1000), random.nextInt(1000)});
            }

            // Benchmark WITH cache (persistent - no clear between ticks)
            cache.clear();
            cache.resetStats();
            long cachedPersistentStart = System.nanoTime();
            for (int tick = 0; tick < ticks; tick++) {
                Set<Long> moveables = cachedStore.getEntitiesWithComponents(POSITION_X, POSITION_Y, VELOCITY_X, VELOCITY_Y);
                for (long entityId : moveables) {
                    store.getComponents(entityId, posComponents, posBuffer);
                    store.getComponents(entityId, velComponents, velBuffer);
                    posBuffer[0] += velBuffer[0];
                    posBuffer[1] += velBuffer[1];
                    store.attachComponents(entityId, posComponents, posBuffer);
                }
            }
            long cachedPersistentElapsed = System.nanoTime() - cachedPersistentStart;
            double cachedPersistentAvgMs = (cachedPersistentElapsed / 1_000_000.0) / ticks;

            System.out.printf("Game loop comparison (%,d entities, %d ticks):%n", numEntities, ticks);
            System.out.printf("  Uncached:          %.3f ms/tick%n", uncachedAvgMs);
            System.out.printf("  Cached (per-tick): %.3f ms/tick (speedup: %.1fx)%n",
                    cachedPerTickAvgMs, uncachedAvgMs / cachedPerTickAvgMs);
            System.out.printf("  Cached (persist):  %.3f ms/tick (speedup: %.1fx)%n",
                    cachedPersistentAvgMs, uncachedAvgMs / cachedPersistentAvgMs);

            // Persistent cache should show significant improvement
            assertThat(cachedPersistentAvgMs).as("Persistent cache should complete tick in < 15ms")
                    .isLessThan(15.0);
            assertThat(uncachedAvgMs / cachedPersistentAvgMs)
                    .as("Persistent cache should be at least 2x faster")
                    .isGreaterThan(2.0);
        }

        @Test
        @DisplayName("Cache invalidation overhead")
        void cacheInvalidationOverhead() {
            int numEntities = 10_000;
            QueryCache cache = new QueryCache();
            CachedEntityComponentStore cachedStore = new CachedEntityComponentStore(store, cache);

            // Setup
            for (int i = 0; i < numEntities; i++) {
                store.createEntity(i);
                store.attachComponent(i, POSITION_X, i);
                store.attachComponent(i, HEALTH, 100);
                store.attachComponent(i, DAMAGE, 10);
            }

            // Warm up cache with multiple queries
            cachedStore.getEntitiesWithComponents(POSITION_X);
            cachedStore.getEntitiesWithComponents(HEALTH);
            cachedStore.getEntitiesWithComponents(DAMAGE);
            cachedStore.getEntitiesWithComponents(POSITION_X, HEALTH);
            cachedStore.getEntitiesWithComponents(POSITION_X, DAMAGE);

            int invalidations = 10_000;

            // Measure invalidateComponent (should only remove relevant entries)
            long componentInvalidateStart = System.nanoTime();
            for (int i = 0; i < invalidations; i++) {
                cache.invalidateComponent(HEALTH);
            }
            long componentInvalidateElapsed = System.nanoTime() - componentInvalidateStart;
            double componentInvalidateNs = (double) componentInvalidateElapsed / invalidations;

            // Re-warm cache
            cachedStore.getEntitiesWithComponents(POSITION_X);
            cachedStore.getEntitiesWithComponents(HEALTH);
            cachedStore.getEntitiesWithComponents(DAMAGE);
            cachedStore.getEntitiesWithComponents(POSITION_X, HEALTH);
            cachedStore.getEntitiesWithComponents(POSITION_X, DAMAGE);

            // Measure invalidateAll
            long allInvalidateStart = System.nanoTime();
            for (int i = 0; i < invalidations; i++) {
                cache.invalidateAll();
            }
            long allInvalidateElapsed = System.nanoTime() - allInvalidateStart;
            double allInvalidateNs = (double) allInvalidateElapsed / invalidations;

            System.out.printf("Cache invalidation overhead (%d operations):%n", invalidations);
            System.out.printf("  invalidateComponent: %.1f ns/op%n", componentInvalidateNs);
            System.out.printf("  invalidateAll:       %.1f ns/op%n", allInvalidateNs);

            // Invalidation should be very fast (sub-microsecond)
            assertThat(componentInvalidateNs).as("Component invalidation should be < 10000 ns").isLessThan(10_000);
            assertThat(allInvalidateNs).as("Full invalidation should be < 10000 ns").isLessThan(10_000);
        }

        @Test
        @DisplayName("Full game tick with multiple systems using cache")
        void fullGameTickWithCache() {
            int numEntities = 5_000;
            QueryCache cache = new QueryCache();
            CachedEntityComponentStore cachedStore = new CachedEntityComponentStore(store, cache);
            Random random = new Random(42);

            // Setup: create diverse entities
            for (int i = 0; i < numEntities; i++) {
                store.createEntity(i);
                store.attachComponent(i, POSITION_X, random.nextInt(1000));
                store.attachComponent(i, POSITION_Y, random.nextInt(1000));
                if (i % 5 != 0) {
                    store.attachComponent(i, VELOCITY_X, random.nextInt(10) - 5);
                    store.attachComponent(i, VELOCITY_Y, random.nextInt(10) - 5);
                }
                if (i % 5 < 3) {
                    store.attachComponent(i, HEALTH, 100);
                }
                if (i % 5 < 2) {
                    store.attachComponent(i, DAMAGE, 10 + random.nextInt(20));
                }
                store.attachComponent(i, TEAM_ID, i % 4);
            }

            int ticks = 100;
            long[] posComponents = {POSITION_X, POSITION_Y};
            long[] velComponents = {VELOCITY_X, VELOCITY_Y};
            float[] posBuffer = new float[2];
            float[] velBuffer = new float[2];

            List<Long> tickTimes = new ArrayList<>();
            long totalStartTime = System.nanoTime();

            for (int tick = 0; tick < ticks; tick++) {
                long tickStart = System.nanoTime();

                // System 1: Movement (using cached query)
                Set<Long> moveables = cachedStore.getEntitiesWithComponents(POSITION_X, POSITION_Y, VELOCITY_X, VELOCITY_Y);
                for (long entityId : moveables) {
                    store.getComponents(entityId, posComponents, posBuffer);
                    store.getComponents(entityId, velComponents, velBuffer);
                    posBuffer[0] += velBuffer[0];
                    posBuffer[1] += velBuffer[1];
                    store.attachComponents(entityId, posComponents, posBuffer);
                }

                // System 2: Query attackers (cached)
                Set<Long> attackers = cachedStore.getEntitiesWithComponents(DAMAGE, POSITION_X, POSITION_Y);

                // System 3: Query targets (cached)
                Set<Long> targets = cachedStore.getEntitiesWithComponents(HEALTH, POSITION_X, POSITION_Y);

                // System 4: Simple collision/damage check
                for (long attacker : attackers) {
                    float attackerX = store.getComponent(attacker, POSITION_X);
                    float attackerY = store.getComponent(attacker, POSITION_Y);
                    float damage = store.getComponent(attacker, DAMAGE);
                    float attackerTeam = store.getComponent(attacker, TEAM_ID);

                    for (long target : targets) {
                        if (attacker == target) continue;

                        float targetTeam = store.getComponent(target, TEAM_ID);
                        if (attackerTeam == targetTeam) continue;

                        float targetX = store.getComponent(target, POSITION_X);
                        float targetY = store.getComponent(target, POSITION_Y);

                        if (Math.abs(attackerX - targetX) + Math.abs(attackerY - targetY) < 10) {
                            float health = store.getComponent(target, HEALTH);
                            store.attachComponent(target, HEALTH, Math.max(0, health - damage));
                        }
                    }
                }

                tickTimes.add(System.nanoTime() - tickStart);
            }

            long totalElapsed = System.nanoTime() - totalStartTime;
            double avgMs = (totalElapsed / 1_000_000.0) / ticks;
            double maxMs = tickTimes.stream().mapToLong(l -> l).max().orElse(0) / 1_000_000.0;
            double minMs = tickTimes.stream().mapToLong(l -> l).min().orElse(0) / 1_000_000.0;

            System.out.printf("Full game tick WITH CACHE (%,d entities, %,d ticks):%n", numEntities, ticks);
            System.out.printf("  avg=%.3f ms, min=%.3f ms, max=%.3f ms%n", avgMs, minMs, maxMs);
            System.out.printf("  Cache stats: hits=%d, misses=%d, ratio=%.2f%%%n",
                    cache.getHitCount(), cache.getMissCount(), cache.getHitRatio() * 100);

            // With caching, should be much faster than uncached version
            assertThat(avgMs).as("Full game tick with cache should complete in < 300ms").isLessThan(300.0);
            assertThat(cache.getHitRatio()).as("Cache hit ratio should be > 95%").isGreaterThan(0.95);
        }
    }

    @Nested
    @DisplayName("Memory Efficiency")
    class MemoryEfficiency {

        @Test
        @DisplayName("Measure memory per entity (approximate)")
        void measureMemoryPerEntity() {
            int numEntities = 50_000;

            // Force GC and get baseline
            System.gc();
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            long beforeMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            // Create entities with full component set
            for (int i = 0; i < numEntities; i++) {
                store.createEntity(i);
                for (int c = 0; c < CARDINALITY; c++) {
                    store.attachComponent(i, c, i * c);
                }
            }

            // Measure after
            System.gc();
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            long afterMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            long usedMemory = afterMemory - beforeMemory;
            double bytesPerEntity = (double) usedMemory / numEntities;

            System.out.printf("Memory for %,d entities (%d components each): %.2f MB (%.1f bytes/entity)%n",
                    numEntities, CARDINALITY, usedMemory / (1024.0 * 1024.0), bytesPerEntity);

            // With 10 longs per entity (8 bytes each) + overhead, expect ~100-200 bytes per entity
            assertThat(bytesPerEntity).as("Should use reasonable memory per entity").isLessThan(500);
        }
    }

    private void warmup(Runnable operation) {
        for (int i = 0; i < 3; i++) {
            operation.run();
        }
    }
}
