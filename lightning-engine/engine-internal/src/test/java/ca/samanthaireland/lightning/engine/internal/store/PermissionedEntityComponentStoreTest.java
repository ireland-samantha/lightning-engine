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

import ca.samanthaireland.lightning.engine.core.exception.EcsAccessForbiddenException;
import ca.samanthaireland.lightning.engine.core.store.PermissionComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionLevel;
import ca.samanthaireland.lightning.engine.core.store.PermissionedStore;
import ca.samanthaireland.lightning.engine.internal.core.store.ArrayEntityComponentStore;
import ca.samanthaireland.lightning.engine.internal.core.store.EcsProperties;
import ca.samanthaireland.lightning.engine.internal.core.store.PermissionedEntityComponentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PermissionedEntityComponentStore")
class PermissionedEntityComponentStoreTest {

    private static final EcsProperties PROPERTIES = new EcsProperties(1000, 100);
    private static final long ENTITY_ID = 1L;

    private ArrayEntityComponentStore baseStore;
    private PermissionedStore store;

    // Components owned by "Module A"
    private PermissionComponent privateComponent;
    private PermissionComponent readComponent;
    private PermissionComponent writeComponent;

    // Components owned by "Module B"
    private PermissionComponent otherPrivateComponent;
    private PermissionComponent otherReadComponent;
    private PermissionComponent otherWriteComponent;

    @BeforeEach
    void setUp() {
        baseStore = new ArrayEntityComponentStore(PROPERTIES);
        store = PermissionedEntityComponentStore.wrap(baseStore);

        // Module A's components
        privateComponent = PermissionComponent.create("PRIVATE_A", PermissionLevel.PRIVATE);
        readComponent = PermissionComponent.create("READ_A", PermissionLevel.READ);
        writeComponent = PermissionComponent.create("WRITE_A", PermissionLevel.WRITE);

        // Module B's components
        otherPrivateComponent = PermissionComponent.create("PRIVATE_B", PermissionLevel.PRIVATE);
        otherReadComponent = PermissionComponent.create("READ_B", PermissionLevel.READ);
        otherWriteComponent = PermissionComponent.create("WRITE_B", PermissionLevel.WRITE);

        // Register all components
        store.registerComponent(privateComponent);
        store.registerComponent(readComponent);
        store.registerComponent(writeComponent);
        store.registerComponent(otherPrivateComponent);
        store.registerComponent(otherReadComponent);
        store.registerComponent(otherWriteComponent);

        // Create an entity
        store.createEntity(ENTITY_ID);
    }

    @Nested
    @DisplayName("Module accessing its own components")
    class OwnedComponentAccess {

        @BeforeEach
        void setModuleAContext() {
            store.setCurrentModuleContext(Set.of(
                    privateComponent.getId(),
                    readComponent.getId(),
                    writeComponent.getId()
            ));
        }

        @Test
        @DisplayName("can write to own PRIVATE component")
        void canWriteToOwnPrivateComponent() {
            store.attachComponent(ENTITY_ID, privateComponent, 42);
            assertThat(store.getComponent(ENTITY_ID, privateComponent)).isEqualTo(42);
        }

        @Test
        @DisplayName("can write to own READ component")
        void canWriteToOwnReadComponent() {
            store.attachComponent(ENTITY_ID, readComponent, 42);
            assertThat(store.getComponent(ENTITY_ID, readComponent)).isEqualTo(42);
        }

        @Test
        @DisplayName("can write to own WRITE component")
        void canWriteToOwnWriteComponent() {
            store.attachComponent(ENTITY_ID, writeComponent, 42);
            assertThat(store.getComponent(ENTITY_ID, writeComponent)).isEqualTo(42);
        }

        @Test
        @DisplayName("can read own PRIVATE component")
        void canReadOwnPrivateComponent() {
            store.attachComponent(ENTITY_ID, privateComponent, 42);
            assertThat(store.hasComponent(ENTITY_ID, privateComponent)).isTrue();
            assertThat(store.getComponent(ENTITY_ID, privateComponent)).isEqualTo(42);
        }

        @Test
        @DisplayName("can remove own PRIVATE component")
        void canRemoveOwnPrivateComponent() {
            store.attachComponent(ENTITY_ID, privateComponent, 42);
            store.removeComponent(ENTITY_ID, privateComponent);
            assertThat(store.hasComponent(ENTITY_ID, privateComponent)).isFalse();
        }
    }

    @Nested
    @DisplayName("Module accessing other module's PRIVATE components")
    class PrivateComponentAccess {

        @BeforeEach
        void setModuleAContext() {
            // Module A is active, trying to access Module B's components
            store.setCurrentModuleContext(Set.of(
                    privateComponent.getId(),
                    readComponent.getId(),
                    writeComponent.getId()
            ));

            // Module B has set up some data
            store.clearCurrentModuleContext();
            store.setCurrentModuleContext(Set.of(otherPrivateComponent.getId()));
            store.attachComponent(ENTITY_ID, otherPrivateComponent, 100);
            store.clearCurrentModuleContext();

            // Switch back to Module A
            store.setCurrentModuleContext(Set.of(
                    privateComponent.getId(),
                    readComponent.getId(),
                    writeComponent.getId()
            ));
        }

        @Test
        @DisplayName("cannot read other module's PRIVATE component")
        void cannotReadOtherPrivateComponent() {
            assertThatThrownBy(() -> store.getComponent(ENTITY_ID, otherPrivateComponent))
                    .isInstanceOf(EcsAccessForbiddenException.class)
                    .hasMessageContaining("PRIVATE");
        }

        @Test
        @DisplayName("cannot check hasComponent on other module's PRIVATE component")
        void cannotHasComponentOtherPrivate() {
            assertThatThrownBy(() -> store.hasComponent(ENTITY_ID, otherPrivateComponent))
                    .isInstanceOf(EcsAccessForbiddenException.class);
        }

        @Test
        @DisplayName("cannot write to other module's PRIVATE component")
        void cannotWriteToOtherPrivateComponent() {
            assertThatThrownBy(() -> store.attachComponent(ENTITY_ID, otherPrivateComponent, 200))
                    .isInstanceOf(EcsAccessForbiddenException.class)
                    .hasMessageContaining("PRIVATE");
        }

        @Test
        @DisplayName("cannot remove other module's PRIVATE component")
        void cannotRemoveOtherPrivateComponent() {
            assertThatThrownBy(() -> store.removeComponent(ENTITY_ID, otherPrivateComponent))
                    .isInstanceOf(EcsAccessForbiddenException.class);
        }

        @Test
        @DisplayName("cannot query entities with other module's PRIVATE component")
        void cannotQueryWithOtherPrivateComponent() {
            assertThatThrownBy(() -> store.getEntitiesWithComponents(otherPrivateComponent))
                    .isInstanceOf(EcsAccessForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("Module accessing other module's READ components")
    class ReadComponentAccess {

        @BeforeEach
        void setModuleAContext() {
            // Module B sets up data
            store.setCurrentModuleContext(Set.of(otherReadComponent.getId()));
            store.attachComponent(ENTITY_ID, otherReadComponent, 100);
            store.clearCurrentModuleContext();

            // Switch to Module A
            store.setCurrentModuleContext(Set.of(
                    privateComponent.getId(),
                    readComponent.getId(),
                    writeComponent.getId()
            ));
        }

        @Test
        @DisplayName("can read other module's READ component")
        void canReadOtherReadComponent() {
            assertThat(store.getComponent(ENTITY_ID, otherReadComponent)).isEqualTo(100);
        }

        @Test
        @DisplayName("can check hasComponent on other module's READ component")
        void canHasComponentOtherRead() {
            assertThat(store.hasComponent(ENTITY_ID, otherReadComponent)).isTrue();
        }

        @Test
        @DisplayName("can query entities with other module's READ component")
        void canQueryWithOtherReadComponent() {
            Set<Long> entities = store.getEntitiesWithComponents(otherReadComponent);
            assertThat(entities).containsExactly(ENTITY_ID);
        }

        @Test
        @DisplayName("cannot write to other module's READ component")
        void cannotWriteToOtherReadComponent() {
            assertThatThrownBy(() -> store.attachComponent(ENTITY_ID, otherReadComponent, 200))
                    .isInstanceOf(EcsAccessForbiddenException.class)
                    .hasMessageContaining("READ");
        }

        @Test
        @DisplayName("cannot remove other module's READ component")
        void cannotRemoveOtherReadComponent() {
            assertThatThrownBy(() -> store.removeComponent(ENTITY_ID, otherReadComponent))
                    .isInstanceOf(EcsAccessForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("Module accessing other module's WRITE components")
    class WriteComponentAccess {

        @BeforeEach
        void setModuleAContext() {
            // Module B sets up data
            store.setCurrentModuleContext(Set.of(otherWriteComponent.getId()));
            store.attachComponent(ENTITY_ID, otherWriteComponent, 100);
            store.clearCurrentModuleContext();

            // Switch to Module A
            store.setCurrentModuleContext(Set.of(
                    privateComponent.getId(),
                    readComponent.getId(),
                    writeComponent.getId()
            ));
        }

        @Test
        @DisplayName("can read other module's WRITE component")
        void canReadOtherWriteComponent() {
            assertThat(store.getComponent(ENTITY_ID, otherWriteComponent)).isEqualTo(100);
        }

        @Test
        @DisplayName("can write to other module's WRITE component")
        void canWriteToOtherWriteComponent() {
            store.attachComponent(ENTITY_ID, otherWriteComponent, 200);
            assertThat(store.getComponent(ENTITY_ID, otherWriteComponent)).isEqualTo(200);
        }

        @Test
        @DisplayName("can remove other module's WRITE component")
        void canRemoveOtherWriteComponent() {
            store.removeComponent(ENTITY_ID, otherWriteComponent);
            assertThat(store.hasComponent(ENTITY_ID, otherWriteComponent)).isFalse();
        }

        @Test
        @DisplayName("can query entities with other module's WRITE component")
        void canQueryWithOtherWriteComponent() {
            Set<Long> entities = store.getEntitiesWithComponents(otherWriteComponent);
            assertThat(entities).containsExactly(ENTITY_ID);
        }
    }

    @Nested
    @DisplayName("Unregistered components")
    class UnregisteredComponents {

        private PermissionComponent unregisteredComponent;

        @BeforeEach
        void setUp() {
            unregisteredComponent = PermissionComponent.create("UNREGISTERED", PermissionLevel.WRITE);
            // Note: NOT registered with the store
            store.setCurrentModuleContext(Set.of(privateComponent.getId()));
        }

        @Test
        @DisplayName("unregistered components default to PRIVATE")
        void unregisteredDefaultsToPrivate() {
            assertThat(store.getPermissionLevel(unregisteredComponent.getId()))
                    .isEqualTo(PermissionLevel.PRIVATE);
        }

        @Test
        @DisplayName("cannot read unregistered component from other module")
        void cannotReadUnregisteredFromOtherModule() {
            assertThatThrownBy(() -> store.getComponent(ENTITY_ID, unregisteredComponent))
                    .isInstanceOf(EcsAccessForbiddenException.class);
        }

        @Test
        @DisplayName("cannot write unregistered component from other module")
        void cannotWriteUnregisteredFromOtherModule() {
            assertThatThrownBy(() -> store.attachComponent(ENTITY_ID, unregisteredComponent, 42))
                    .isInstanceOf(EcsAccessForbiddenException.class);
        }

        @Test
        @DisplayName("can access unregistered component if owned")
        void canAccessUnregisteredIfOwned() {
            store.setCurrentModuleContext(Set.of(unregisteredComponent.getId()));
            store.attachComponent(ENTITY_ID, unregisteredComponent, 42);
            assertThat(store.getComponent(ENTITY_ID, unregisteredComponent)).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("Bulk operations")
    class BulkOperations {

        @BeforeEach
        void setUp() {
            // Module B sets up data
            store.setCurrentModuleContext(Set.of(
                    otherReadComponent.getId(),
                    otherWriteComponent.getId()
            ));
            store.attachComponent(ENTITY_ID, otherReadComponent, 10);
            store.attachComponent(ENTITY_ID, otherWriteComponent, 20);
            store.clearCurrentModuleContext();

            // Switch to Module A
            store.setCurrentModuleContext(Set.of(
                    privateComponent.getId(),
                    readComponent.getId(),
                    writeComponent.getId()
            ));
        }

        @Test
        @DisplayName("attachComponents checks all components")
        void attachComponentsChecksAll() {
            // Mix of writable and non-writable components
            assertThatThrownBy(() -> store.attachComponents(
                    ENTITY_ID,
                    new long[]{otherWriteComponent.getId(), otherReadComponent.getId()},
                    new float[]{1, 2}
            )).isInstanceOf(EcsAccessForbiddenException.class);
        }

        @Test
        @DisplayName("getComponents checks all components")
        void getComponentsChecksAll() {
            float[] buf = new float[2];
            // Mix of readable and non-readable components
            assertThatThrownBy(() -> store.getComponents(
                    ENTITY_ID,
                    new long[]{otherReadComponent.getId(), otherPrivateComponent.getId()},
                    buf
            )).isInstanceOf(EcsAccessForbiddenException.class);
        }

        @Test
        @DisplayName("getEntitiesWithComponents checks all components")
        void getEntitiesWithComponentsChecksAll() {
            assertThatThrownBy(() -> store.getEntitiesWithComponents(
                    otherReadComponent.getId(),
                    otherPrivateComponent.getId()
            )).isInstanceOf(EcsAccessForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("No module context")
    class NoModuleContext {

        @BeforeEach
        void clearContext() {
            store.clearCurrentModuleContext();
        }

        @Test
        @DisplayName("can access WRITE components without context")
        void canAccessWriteWithoutContext() {
            // Set up data with context
            store.setCurrentModuleContext(Set.of(writeComponent.getId()));
            store.attachComponent(ENTITY_ID, writeComponent, 42);
            store.clearCurrentModuleContext();

            // Now access without context - WRITE allows it
            assertThat(store.getComponent(ENTITY_ID, writeComponent)).isEqualTo(42);
            store.attachComponent(ENTITY_ID, writeComponent, 100);
            assertThat(store.getComponent(ENTITY_ID, writeComponent)).isEqualTo(100);
        }

        @Test
        @DisplayName("can read READ components without context")
        void canReadReadWithoutContext() {
            store.setCurrentModuleContext(Set.of(readComponent.getId()));
            store.attachComponent(ENTITY_ID, readComponent, 42);
            store.clearCurrentModuleContext();

            assertThat(store.getComponent(ENTITY_ID, readComponent)).isEqualTo(42);
        }

        @Test
        @DisplayName("cannot write READ components without context")
        void cannotWriteReadWithoutContext() {
            assertThatThrownBy(() -> store.attachComponent(ENTITY_ID, readComponent, 42))
                    .isInstanceOf(EcsAccessForbiddenException.class);
        }

        @Test
        @DisplayName("cannot access PRIVATE components without context")
        void cannotAccessPrivateWithoutContext() {
            assertThatThrownBy(() -> store.getComponent(ENTITY_ID, privateComponent))
                    .isInstanceOf(EcsAccessForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("Entity lifecycle operations")
    class EntityLifecycle {

        @Test
        @DisplayName("createEntity does not require permissions")
        void createEntityNoPermissions() {
            store.clearCurrentModuleContext();
            store.createEntity(999L);
            // Should not throw
        }

        @Test
        @DisplayName("deleteEntity does not require permissions")
        void deleteEntityNoPermissions() {
            store.clearCurrentModuleContext();
            store.deleteEntity(ENTITY_ID);
            // Should not throw
        }
    }

    @Nested
    @DisplayName("Context management")
    class ContextManagement {

        @Test
        @DisplayName("getCurrentModuleContext returns current context")
        void getCurrentModuleContextReturnsContext() {
            Set<Long> context = Set.of(1L, 2L, 3L);
            store.setCurrentModuleContext(context);
            assertThat(store.getCurrentModuleContext()).isEqualTo(context);
        }

        @Test
        @DisplayName("clearCurrentModuleContext resets to empty")
        void clearResetsToEmpty() {
            store.setCurrentModuleContext(Set.of(1L, 2L, 3L));
            store.clearCurrentModuleContext();
            assertThat(store.getCurrentModuleContext()).isEmpty();
        }

        @Test
        @DisplayName("null context is treated as empty")
        void nullContextIsEmpty() {
            store.setCurrentModuleContext(null);
            assertThat(store.getCurrentModuleContext()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Using component IDs directly")
    class ComponentIdAccess {

        @BeforeEach
        void setUp() {
            store.setCurrentModuleContext(Set.of(writeComponent.getId()));
            store.attachComponent(ENTITY_ID, writeComponent, 42);
            store.clearCurrentModuleContext();
        }

        @Test
        @DisplayName("can access via component ID when using WRITE level")
        void canAccessViaIdWithWrite() {
            assertThat(store.getComponent(ENTITY_ID, writeComponent.getId())).isEqualTo(42);
        }

        @Test
        @DisplayName("permission check works with component ID")
        void permissionCheckWorksWithId() {
            assertThatThrownBy(() -> store.getComponent(ENTITY_ID, privateComponent.getId()))
                    .isInstanceOf(EcsAccessForbiddenException.class);
        }
    }
}
