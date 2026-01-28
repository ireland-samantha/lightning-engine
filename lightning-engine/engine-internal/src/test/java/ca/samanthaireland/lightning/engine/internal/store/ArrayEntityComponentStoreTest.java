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

import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.internal.core.store.ArrayEntityComponentStore;
import ca.samanthaireland.lightning.engine.internal.core.store.EcsProperties;
import ca.samanthaireland.lightning.engine.internal.core.store.LockingEntityComponentStore;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;

import java.util.Set;

import static ca.samanthaireland.lightning.engine.core.store.EntityComponentStore.NULL;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class ArrayEntityComponentStoreTest {
    private static final EcsProperties PROPERTIES = new EcsProperties(100000, 100);
    private ArrayEntityComponentStore store = new ArrayEntityComponentStore(PROPERTIES);

    private static final int CARDINALITY = 5;

    private static final long POSITION_X = 0;
    private static final long POSITION_Y = 1;
    private static final long VELOCITY_X = 2;
    private static final long VELOCITY_Y = 3;
    private static final long ID = 4;

    @BeforeEach
    void setUp() {
        store.reset();
    }

    @Test
    void attachComponent_getComponent_two() {
        store.createEntity(1);

        store.attachComponent(1, VELOCITY_X, 5);
        store.attachComponent(1, VELOCITY_Y, 4);

        assertEquals(5, store.getComponent(1, VELOCITY_X));
        assertEquals(4, store.getComponent(1, VELOCITY_Y));
    }

    @Test
    void attachComponent_getComponent_all() {
        store.createEntity(1);

        int[] expected = new int[CARDINALITY];
        for (int i = 0; i < CARDINALITY; i++) {
            expected[i] = i * 1000;
        }

        for (int i = 0; i < CARDINALITY; i++) {
            store.attachComponent(1, i, expected[i]);
        }

        for (int i = 0; i < CARDINALITY; i++) {
            assertEquals(expected[i], store.getComponent(1, i));
        }
    }

    @Test
    void attachComponent_getComponent() {
        store.createEntity(1);
        store.attachComponent(1, VELOCITY_Y, 42);

        float actual = store.getComponent(1, VELOCITY_Y);
        assertEquals(42, actual);
    }

    @Test
    void attachComponent_hasComponent() {
        store.createEntity(1);
        store.attachComponent(1, VELOCITY_Y, 42);

        assertTrue(store.hasComponent(1, VELOCITY_Y));
    }

    @Test
    void hasComponent_noComponent() {
        store.createEntity(1);
        assertFalse(store.hasComponent(1, VELOCITY_Y));
    }

    @Test
    void hasComponent_noEntity() {
        assertFalse(store.hasComponent(1, VELOCITY_Y));
    }

    @Test
    void hasComponent_deleted() {
        store.createEntity(1);
        store.attachComponent(1, VELOCITY_Y, 42);
        assertTrue(store.hasComponent(1, VELOCITY_Y));

        store.removeComponent(1, VELOCITY_Y);
        assertFalse(store.hasComponent(1, VELOCITY_Y));
    }

    @Test
    void attachComponent_replace() {
        store.createEntity(1);
        store.attachComponent(1, VELOCITY_Y, 42);

        float actual = store.getComponent(1, VELOCITY_Y);
        assertEquals(42, actual);

        store.attachComponent(1, VELOCITY_Y, 44);

        float actual2 = store.getComponent(1, VELOCITY_Y);
        assertEquals(44, actual2);
    }

    @Test
    void attachComponent_getDifferentComponent() {
        store.createEntity(1);
        store.attachComponent(1, VELOCITY_Y, 42);

        assertEquals(NULL, store.getComponent(1, VELOCITY_X));
    }

    @Test
    void getComponent_notExisting() {
        assertEquals(NULL, store.getComponent(500, POSITION_X));
    }

    @Test
    void deleteEntity_withComponents() {
        store.createEntity(1);
        store.attachComponent(1, VELOCITY_Y, 42);

        float actual = store.getComponent(1, VELOCITY_Y);
        assertEquals(42, actual);

        store.deleteEntity(1);

        float actual2 = store.getComponent(1, VELOCITY_Y);
        assertTrue(store.isNull(actual2));
    }

    @Test
    void removeComponent() {
        store.createEntity(1);
        store.attachComponent(1, VELOCITY_Y, 42);

        float actual = store.getComponent(1, VELOCITY_Y);
        assertEquals(42, actual);

        store.removeComponent(1, VELOCITY_Y);

        float actual2 = store.getComponent(1, VELOCITY_Y);
        assertTrue(store.isNull(actual2));
    }

    @Test
    void exceedEntityLimit() {
        for (int i = 0; i < PROPERTIES.maxVectors(); i++) {
            store.createEntity(i);
        }

        assertThrows(RuntimeException.class, () -> store.createEntity(9999999));
    }

    @Test
    void reachCapacity_thenRemove_thenRepeat() {
        for (int i = 0; i < PROPERTIES.maxVectors(); i++) {
            store.createEntity(i);
            store.attachComponent(i, POSITION_X, 5);
            assertTrue(store.hasComponent(i, POSITION_X));
        }

        for (int i = 0; i < PROPERTIES.maxVectors(); i++) {
            store.deleteEntity(i);
            assertFalse(store.hasComponent(i, POSITION_X));
        }

        for (int i = 0; i < PROPERTIES.maxVectors(); i++) {
            store.createEntity(i);
            store.attachComponent(i, POSITION_X, 5);
        }

        for (int i = 0; i < PROPERTIES.maxVectors(); i++) {
            assertEquals(5, store.getComponent(i, POSITION_X));
            assertTrue(store.hasComponent(i, POSITION_X));
        }
    }

    @Test
    void attachComponent_entityNotExisting_throwsException() {
        assertThrows(EntityNotFoundException.class, () -> store.attachComponent(1, POSITION_X, 5));
    }

    @Test
    void attachComponents_entityNotExisting_throwsException() {
        assertThrows(EntityNotFoundException.class, () ->
                store.attachComponents(1, new long[]{POSITION_X, POSITION_Y}, new float[]{5, 10}));
    }

    @Test
    void getEntitiesWithComponents_multipleComponents() {
        store.createEntity(1);
        store.attachComponent(1, POSITION_X, 5);
        store.attachComponent(1, POSITION_Y, 4);

        Set<Long> found = store.getEntitiesWithComponents(POSITION_X, POSITION_Y);
        assertEquals(Set.of(1L), found);
    }

    @Test
    void getEntitiesWithComponents_multipleComponents_multipleEntities_sameComponents() {
        store.createEntity(1);
        store.attachComponent(1, POSITION_X, 5);
        store.attachComponent(1, POSITION_Y, 4);

        store.createEntity(2);
        store.attachComponent(2, POSITION_X, 5);
        store.attachComponent(2, POSITION_Y, 4);

        Set<Long> found = store.getEntitiesWithComponents(POSITION_X, POSITION_Y);
        assertEquals(Set.of(1L, 2L), found);
    }

    @Test
    void getEntitiesWithComponents_multipleComponents_multipleEntities_extraComponentDiscarded() {
        store.createEntity(1);
        store.attachComponent(1, POSITION_X, 5);
        store.attachComponent(1, POSITION_Y, 4);

        store.createEntity(2);
        store.attachComponent(2, POSITION_X, 5);
        store.attachComponent(2, POSITION_Y, 4);
        store.attachComponent(2, VELOCITY_X, 1);

        Set<Long> found = store.getEntitiesWithComponents(POSITION_X, POSITION_Y);
        assertEquals(Set.of(1L, 2L), found);
    }

    @Test
    void getEntitiesWithComponents_multipleComponents_componentDeleted_multipleEntities_extraComponentDiscarded() {
        store.createEntity(1);
        store.attachComponent(1, POSITION_X, 5);
        store.attachComponent(1, POSITION_Y, 4);

        store.createEntity(2);
        store.attachComponent(2, POSITION_X, 5);
        store.attachComponent(2, POSITION_Y, 4);
        store.attachComponent(2, VELOCITY_X, 1);
        store.removeComponent(2, VELOCITY_X);

        Set<Long> found = store.getEntitiesWithComponents(POSITION_X, POSITION_Y);
        assertEquals(Set.of(1L, 2L), found);
    }


    @Test
    void getEntitiesWithComponents_multipleComponents_notFound() {
        store.createEntity(1);
        store.attachComponent(1, POSITION_X, 5);
        store.attachComponent(1, POSITION_Y, 4);

        Set<Long> found = store.getEntitiesWithComponents(VELOCITY_X);
        assertEquals(Set.of(), found);
    }

    @Test
    void getEntitiesWithComponents_multipleComponents_deletedComponent() {
        store.createEntity(1);
        store.attachComponent(1, VELOCITY_X, 4);
        store.removeComponent(1, VELOCITY_X);

        Set<Long> found = store.getEntitiesWithComponents(VELOCITY_X);
        assertEquals(Set.of(), found);
    }

    @Test
    void getEntitiesWithComponents_multipleComponents_deletedEntity() {
        store.createEntity(1);
        store.attachComponent(1, VELOCITY_X, 4);
        store.deleteEntity(1);

        Set<Long> found = store.getEntitiesWithComponents(VELOCITY_X);
        assertEquals(Set.of(), found);
    }

    @Test
    void getComponents() {
        store.createEntity(1);
        store.attachComponent(1, VELOCITY_X, 4);
        store.attachComponent(1, VELOCITY_Y, 7);

        float[] components = new float[2];

        store.getComponents(1, new long[]{VELOCITY_X, VELOCITY_Y}, components);

        assertArrayEquals(new float[]{4, 7}, components);
    }

    @Test
    void getComponents_noComponents() {
        float[] components = new float[]{1};

        store.getComponents(1, new long[]{VELOCITY_Y}, components);

        assertArrayEquals(new float[]{1}, components);
    }

    @Test
    void getComponents_arraysNotEqual() {
        assertThrows(IllegalArgumentException.class, () -> store.getComponents(1, new long[]{VELOCITY_Y}, new float[10]));
    }

    @Test
    void attachComponents() {
        store.createEntity(1);
        store.attachComponents(1, new long[]{VELOCITY_X, VELOCITY_Y}, new float[]{54, 33});
        float[] buf = new float[2];
        store.getComponents(1, new long[]{VELOCITY_X, VELOCITY_Y}, buf);
        assertArrayEquals(new float[]{54, 33}, buf);
    }

    @Nested
    @DisplayName("Concurrency tests (with LockingEntityComponentStore)")
    class ConcurrencyTests {
        private EntityComponentStore threadSafeStore;

        @BeforeEach
        void setUpConcurrency() {
            store.reset();
            threadSafeStore = LockingEntityComponentStore.wrap(store);
        }

        @Test
        void concurrentCreateEntity_noCollisions() throws InterruptedException {
            int numThreads = 10;
            int entitiesPerThread = 100;
            Thread[] threads = new Thread[numThreads];

            for (int t = 0; t < numThreads; t++) {
                final int threadId = t;
                threads[t] = new Thread(() -> {
                    for (int i = 0; i < entitiesPerThread; i++) {
                        long id = threadId * 10000L + i;
                        threadSafeStore.createEntity(id);
                        threadSafeStore.attachComponent(id, POSITION_X, id);
                    }
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            for (int t = 0; t < numThreads; t++) {
                for (int i = 0; i < entitiesPerThread; i++) {
                    long id = t * 10000L + i;
                    assertTrue(threadSafeStore.hasComponent(id, POSITION_X),
                            "Entity " + id + " should have POSITION_X component");
                    assertEquals(id, threadSafeStore.getComponent(id, POSITION_X),
                            "Entity " + id + " should have correct value");
                }
            }
        }

        @Test
        void concurrentReadWrite_noDataCorruption() throws InterruptedException {
            for (int i = 0; i < 100; i++) {
                threadSafeStore.createEntity(i);
                threadSafeStore.attachComponent(i, POSITION_X, i * 10);
            }

            int numReaders = 5;
            int numWriters = 3;
            Thread[] readers = new Thread[numReaders];
            Thread[] writers = new Thread[numWriters];
            boolean[] readErrors = new boolean[1];

            for (int r = 0; r < numReaders; r++) {
                readers[r] = new Thread(() -> {
                    for (int iteration = 0; iteration < 1000; iteration++) {
                        for (int i = 0; i < 100; i++) {
                            float value = threadSafeStore.getComponent(i, POSITION_X);
                            if (!threadSafeStore.isNull(value) && value % 10 != 0) {
                                readErrors[0] = true;
                            }
                        }
                    }
                });
            }

            for (int w = 0; w < numWriters; w++) {
                final int writerId = w;
                writers[w] = new Thread(() -> {
                    for (int iteration = 0; iteration < 100; iteration++) {
                        for (int i = 0; i < 100; i++) {
                            threadSafeStore.attachComponent(i, POSITION_X, (writerId + 1) * 1000L + i * 10);
                        }
                    }
                });
            }

            for (Thread reader : readers) reader.start();
            for (Thread writer : writers) writer.start();

            for (Thread reader : readers) reader.join();
            for (Thread writer : writers) writer.join();

            assertFalse(readErrors[0], "Should not have any read errors due to data corruption");
        }

        @Test
        void concurrentDeleteAndCreate_noLeaks() throws InterruptedException {
            int numThreads = 4;
            Thread[] threads = new Thread[numThreads];

            for (int t = 0; t < numThreads; t++) {
                final int threadId = t;
                threads[t] = new Thread(() -> {
                    for (int cycle = 0; cycle < 50; cycle++) {
                        for (int i = 0; i < 10; i++) {
                            long id = threadId * 1000L + cycle * 10 + i;
                            threadSafeStore.createEntity(id);
                            threadSafeStore.attachComponent(id, POSITION_X, id);
                        }
                        for (int i = 0; i < 10; i++) {
                            long id = threadId * 1000L + cycle * 10 + i;
                            threadSafeStore.deleteEntity(id);
                        }
                    }
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            Set<Long> remaining = threadSafeStore.getEntitiesWithComponents(POSITION_X);
            assertTrue(remaining.isEmpty(), "All entities should have been deleted");
        }
    }
}
