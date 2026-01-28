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

import ca.samanthaireland.lightning.engine.core.entity.CoreComponents;
import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.internal.core.snapshot.DirtyInfo;
import ca.samanthaireland.lightning.engine.internal.core.store.ArrayEntityComponentStore;
import ca.samanthaireland.lightning.engine.internal.core.store.DirtyTrackingEntityComponentStore;
import ca.samanthaireland.lightning.engine.internal.core.store.EcsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DirtyTrackingEntityComponentStore} decorator.
 */
@DisplayName("DirtyTrackingEntityComponentStore")
class DirtyTrackingEntityComponentStoreTest {

    private static final EcsProperties PROPERTIES = new EcsProperties(10000, 100);
    private static final long MATCH_ID = 1L;

    private static final BaseComponent POSITION_X = new TestComponent(100, "POSITION_X");
    private static final BaseComponent POSITION_Y = new TestComponent(101, "POSITION_Y");
    private static final BaseComponent VELOCITY_X = new TestComponent(102, "VELOCITY_X");

    private ArrayEntityComponentStore baseStore;
    private DirtyTrackingEntityComponentStore dirtyStore;

    @BeforeEach
    void setUp() {
        baseStore = new ArrayEntityComponentStore(PROPERTIES);
        dirtyStore = new DirtyTrackingEntityComponentStore(baseStore);
    }

    @Nested
    @DisplayName("Entity Creation Tracking")
    class EntityCreationTracking {

        @Test
        @DisplayName("marks newly created entity as added")
        void marksNewEntityAsAdded() {
            long entityId = dirtyStore.createEntityForMatch(MATCH_ID);

            DirtyInfo dirty = dirtyStore.consumeDirtyInfo(MATCH_ID);

            assertThat(dirty.added()).containsExactly(entityId);
            assertThat(dirty.modified()).isEmpty();
            assertThat(dirty.removed()).isEmpty();
        }

        @Test
        @DisplayName("tracks multiple entity creations for same match")
        void tracksMultipleCreations() {
            long entity1 = dirtyStore.createEntityForMatch(MATCH_ID);
            long entity2 = dirtyStore.createEntityForMatch(MATCH_ID);
            long entity3 = dirtyStore.createEntityForMatch(MATCH_ID);

            DirtyInfo dirty = dirtyStore.consumeDirtyInfo(MATCH_ID);

            assertThat(dirty.added()).containsExactlyInAnyOrder(entity1, entity2, entity3);
        }

        @Test
        @DisplayName("tracks entities separately per match")
        void tracksEntitiesSeparatelyPerMatch() {
            long entity1 = dirtyStore.createEntityForMatch(MATCH_ID);
            long entity2 = dirtyStore.createEntityForMatch(2L);

            DirtyInfo dirty1 = dirtyStore.consumeDirtyInfo(MATCH_ID);
            DirtyInfo dirty2 = dirtyStore.consumeDirtyInfo(2L);

            assertThat(dirty1.added()).containsExactly(entity1);
            assertThat(dirty2.added()).containsExactly(entity2);
        }
    }

    @Nested
    @DisplayName("Entity Deletion Tracking")
    class EntityDeletionTracking {

        @Test
        @DisplayName("marks deleted entity as removed")
        void marksDeletedEntityAsRemoved() {
            long entityId = dirtyStore.createEntityForMatch(MATCH_ID);
            dirtyStore.consumeDirtyInfo(MATCH_ID); // Clear added state

            dirtyStore.deleteEntity(entityId);

            DirtyInfo dirty = dirtyStore.consumeDirtyInfo(MATCH_ID);
            assertThat(dirty.removed()).containsExactly(entityId);
            assertThat(dirty.modified()).isEmpty();
            assertThat(dirty.added()).isEmpty();
        }

        @Test
        @DisplayName("handles transient entities (created and deleted in same interval)")
        void handlesTransientEntities() {
            long entityId = dirtyStore.createEntityForMatch(MATCH_ID);
            dirtyStore.deleteEntity(entityId);

            DirtyInfo dirty = dirtyStore.consumeDirtyInfo(MATCH_ID);

            // Transient entities should be filtered out
            assertThat(dirty.added()).isEmpty();
            assertThat(dirty.removed()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Component Modification Tracking")
    class ComponentModificationTracking {

        @Test
        @DisplayName("marks entity as modified when component attached")
        void marksEntityAsModifiedOnAttach() {
            long entityId = dirtyStore.createEntityForMatch(MATCH_ID);
            dirtyStore.consumeDirtyInfo(MATCH_ID); // Clear added state

            dirtyStore.attachComponent(entityId, POSITION_X, 100.0f);

            DirtyInfo dirty = dirtyStore.consumeDirtyInfo(MATCH_ID);
            assertThat(dirty.modified()).containsExactly(entityId);
        }

        @Test
        @DisplayName("marks entity as modified when multiple components attached")
        void marksEntityAsModifiedOnBatchAttach() {
            long entityId = dirtyStore.createEntityForMatch(MATCH_ID);
            dirtyStore.consumeDirtyInfo(MATCH_ID);

            dirtyStore.attachComponents(entityId,
                    new long[]{POSITION_X.getId(), POSITION_Y.getId()},
                    new float[]{10.0f, 20.0f});

            DirtyInfo dirty = dirtyStore.consumeDirtyInfo(MATCH_ID);
            assertThat(dirty.modified()).containsExactly(entityId);
        }

        @Test
        @DisplayName("marks entity as modified when component removed")
        void marksEntityAsModifiedOnRemove() {
            long entityId = dirtyStore.createEntityForMatch(MATCH_ID);
            dirtyStore.attachComponent(entityId, POSITION_X, 100.0f);
            dirtyStore.consumeDirtyInfo(MATCH_ID);

            dirtyStore.removeComponent(entityId, POSITION_X);

            DirtyInfo dirty = dirtyStore.consumeDirtyInfo(MATCH_ID);
            assertThat(dirty.modified()).containsExactly(entityId);
        }

        @Test
        @DisplayName("does not mark newly added entity as modified")
        void doesNotMarkAddedEntityAsModified() {
            long entityId = dirtyStore.createEntityForMatch(MATCH_ID);
            dirtyStore.attachComponent(entityId, POSITION_X, 100.0f);

            DirtyInfo dirty = dirtyStore.consumeDirtyInfo(MATCH_ID);

            // Entity should be in added, not modified
            assertThat(dirty.added()).containsExactly(entityId);
            assertThat(dirty.modified()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Consume and Clear")
    class ConsumeAndClear {

        @Test
        @DisplayName("clears dirty state after consume")
        void clearsDirtyStateAfterConsume() {
            long entityId = dirtyStore.createEntityForMatch(MATCH_ID);
            dirtyStore.consumeDirtyInfo(MATCH_ID);

            DirtyInfo dirty = dirtyStore.consumeDirtyInfo(MATCH_ID);

            assertThat(dirty.hasChanges()).isFalse();
        }

        @Test
        @DisplayName("returns empty info for unknown match")
        void returnsEmptyForUnknownMatch() {
            DirtyInfo dirty = dirtyStore.consumeDirtyInfo(999L);

            assertThat(dirty.hasChanges()).isFalse();
            assertThat(dirty.added()).isEmpty();
            assertThat(dirty.modified()).isEmpty();
            assertThat(dirty.removed()).isEmpty();
        }

        @Test
        @DisplayName("peek does not clear state")
        void peekDoesNotClearState() {
            long entityId = dirtyStore.createEntityForMatch(MATCH_ID);

            DirtyInfo peek1 = dirtyStore.peekDirtyInfo(MATCH_ID);
            DirtyInfo peek2 = dirtyStore.peekDirtyInfo(MATCH_ID);

            assertThat(peek1.added()).containsExactly(entityId);
            assertThat(peek2.added()).containsExactly(entityId);
        }
    }

    @Nested
    @DisplayName("Reset")
    class Reset {

        @Test
        @DisplayName("clears all dirty state on reset")
        void clearsAllDirtyStateOnReset() {
            dirtyStore.createEntityForMatch(MATCH_ID);
            dirtyStore.createEntityForMatch(2L);

            dirtyStore.reset();

            DirtyInfo dirty1 = dirtyStore.consumeDirtyInfo(MATCH_ID);
            DirtyInfo dirty2 = dirtyStore.consumeDirtyInfo(2L);

            assertThat(dirty1.hasChanges()).isFalse();
            assertThat(dirty2.hasChanges()).isFalse();
        }
    }

    @Nested
    @DisplayName("Delegation")
    class Delegation {

        @Test
        @DisplayName("delegates read operations to underlying store")
        void delegatesReadOperations() {
            long entityId = dirtyStore.createEntityForMatch(MATCH_ID);
            dirtyStore.attachComponent(entityId, POSITION_X, 42.0f);

            assertThat(dirtyStore.getComponent(entityId, POSITION_X)).isEqualTo(42.0f);
            assertThat(dirtyStore.hasComponent(entityId, POSITION_X)).isTrue();
            assertThat(dirtyStore.getEntityCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("delegates query operations to underlying store")
        void delegatesQueryOperations() {
            long entityId = dirtyStore.createEntityForMatch(MATCH_ID);
            dirtyStore.attachComponent(entityId, POSITION_X, 42.0f);

            var entities = dirtyStore.getEntitiesWithComponents(POSITION_X);
            assertThat(entities).contains(entityId);
        }
    }

    @Nested
    @DisplayName("DirtyInfo Helper Methods")
    class DirtyInfoHelperMethods {

        @Test
        @DisplayName("hasChanges returns true when changes exist")
        void hasChangesReturnsTrue() {
            dirtyStore.createEntityForMatch(MATCH_ID);

            DirtyInfo dirty = dirtyStore.consumeDirtyInfo(MATCH_ID);

            assertThat(dirty.hasChanges()).isTrue();
        }

        @Test
        @DisplayName("totalChanges returns correct count")
        void totalChangesReturnsCorrectCount() {
            long e1 = dirtyStore.createEntityForMatch(MATCH_ID);
            long e2 = dirtyStore.createEntityForMatch(MATCH_ID);

            DirtyInfo dirty = dirtyStore.consumeDirtyInfo(MATCH_ID);

            assertThat(dirty.totalChanges()).isEqualTo(2);
            assertThat(dirty.addedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("isModificationOnly returns true for only modifications")
        void isModificationOnlyReturnsTrue() {
            long entityId = dirtyStore.createEntityForMatch(MATCH_ID);
            dirtyStore.consumeDirtyInfo(MATCH_ID);

            dirtyStore.attachComponent(entityId, POSITION_X, 100.0f);

            DirtyInfo dirty = dirtyStore.consumeDirtyInfo(MATCH_ID);

            assertThat(dirty.isModificationOnly()).isTrue();
            assertThat(dirty.hasStructuralChanges()).isFalse();
        }

        @Test
        @DisplayName("hasStructuralChanges returns true for adds/removes")
        void hasStructuralChangesReturnsTrue() {
            dirtyStore.createEntityForMatch(MATCH_ID);

            DirtyInfo dirty = dirtyStore.consumeDirtyInfo(MATCH_ID);

            assertThat(dirty.hasStructuralChanges()).isTrue();
        }
    }

    private static class TestComponent extends BaseComponent {
        TestComponent(long id, String name) {
            super(id, name);
        }
    }
}
