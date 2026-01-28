package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.module.ModuleContext;
import ca.samanthaireland.lightning.engine.ext.modules.GridMapExports;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Position;
import ca.samanthaireland.lightning.engine.ext.modules.domain.RigidBody;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ca.samanthaireland.lightning.engine.ext.modules.RigidBodyModuleFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EcsRigidBodyRepository}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EcsRigidBodyRepository")
class EcsRigidBodyRepositoryTest {

    @Mock
    private ModuleContext context;

    @Mock
    private EntityComponentStore store;

    @Mock
    private GridMapExports gridMapExports;

    private EcsRigidBodyRepository repository;

    @BeforeEach
    void setUp() {
        // Using lenient() because not all tests use both store and gridMapExports
        lenient().when(context.getEntityComponentStore()).thenReturn(store);
        lenient().when(context.getModuleExports(GridMapExports.class)).thenReturn(gridMapExports);
        repository = new EcsRigidBodyRepository(context);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should set position through EntityModuleExports")
        void shouldSetPositionThroughExports() {
            RigidBody rigidBody = RigidBody.create(
                    42L,
                    new Vector3(1, 2, 3),
                    new Vector3(4, 5, 6),
                    1.0f
            );

            repository.save(rigidBody);

            verify(gridMapExports).setPosition(42L, 1f, 2f, 3f);
        }

        @Test
        @DisplayName("should attach all velocity components")
        void shouldAttachAllVelocityComponents() {
            RigidBody rigidBody = RigidBody.create(
                    42L,
                    new Vector3(1, 2, 3),
                    new Vector3(4, 5, 6),
                    1.0f
            );

            repository.save(rigidBody);

            ArgumentCaptor<float[]> velocityCaptor = ArgumentCaptor.forClass(float[].class);
            verify(store).attachComponents(eq(42L), eq(VELOCITY_COMPONENTS), velocityCaptor.capture());

            float[] velocity = velocityCaptor.getValue();
            assertThat(velocity[0]).isEqualTo(4f);
            assertThat(velocity[1]).isEqualTo(5f);
            assertThat(velocity[2]).isEqualTo(6f);
        }

        @Test
        @DisplayName("should attach mass component")
        void shouldAttachMassComponent() {
            RigidBody rigidBody = RigidBody.create(
                    42L, Vector3.ZERO, Vector3.ZERO, 2.5f
            );

            repository.save(rigidBody);

            verify(store).attachComponent(42L, MASS, 2.5f);
        }

        @Test
        @DisplayName("should attach flag component")
        void shouldAttachFlagComponent() {
            RigidBody rigidBody = RigidBody.create(
                    42L, Vector3.ZERO, Vector3.ZERO, 1.0f
            );

            repository.save(rigidBody);

            verify(store).attachComponent(42L, FLAG, 1.0f);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return rigid body when entity exists")
        void shouldReturnRigidBodyWhenEntityExists() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(Set.of(entityId));

            // Mock position from EntityModuleExports
            when(gridMapExports.getPosition(entityId))
                    .thenReturn(Optional.of(new Position(1f, 2f, 3f)));

            // Mock velocity and other components from store
            when(store.getComponent(entityId, VELOCITY_X)).thenReturn(4f);
            when(store.getComponent(entityId, VELOCITY_Y)).thenReturn(5f);
            when(store.getComponent(entityId, VELOCITY_Z)).thenReturn(6f);
            when(store.getComponent(entityId, ACCELERATION_X)).thenReturn(0f);
            when(store.getComponent(entityId, ACCELERATION_Y)).thenReturn(0f);
            when(store.getComponent(entityId, ACCELERATION_Z)).thenReturn(0f);
            when(store.getComponent(entityId, FORCE_X)).thenReturn(0f);
            when(store.getComponent(entityId, FORCE_Y)).thenReturn(0f);
            when(store.getComponent(entityId, FORCE_Z)).thenReturn(0f);
            when(store.getComponent(entityId, MASS)).thenReturn(2.0f);
            when(store.getComponent(entityId, LINEAR_DRAG)).thenReturn(0.1f);
            when(store.getComponent(entityId, ANGULAR_DRAG)).thenReturn(0.2f);
            when(store.getComponent(entityId, INERTIA)).thenReturn(1.5f);
            when(store.getComponent(entityId, ANGULAR_VELOCITY)).thenReturn(0f);
            when(store.getComponent(entityId, ROTATION)).thenReturn(0f);
            when(store.getComponent(entityId, TORQUE)).thenReturn(0f);

            Optional<RigidBody> result = repository.findById(entityId);

            assertThat(result).isPresent();
            RigidBody rb = result.get();
            assertThat(rb.entityId()).isEqualTo(entityId);
            assertThat(rb.position()).isEqualTo(new Vector3(1, 2, 3));
            assertThat(rb.velocity()).isEqualTo(new Vector3(4, 5, 6));
            assertThat(rb.mass()).isEqualTo(2.0f);
            assertThat(rb.linearDrag()).isEqualTo(0.1f);
            assertThat(rb.angularDrag()).isEqualTo(0.2f);
            assertThat(rb.inertia()).isEqualTo(1.5f);
        }

        @Test
        @DisplayName("should return empty when entity does not exist")
        void shouldReturnEmptyWhenEntityDoesNotExist() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(Set.of(100L, 200L));

            Optional<RigidBody> result = repository.findById(entityId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle zero mass by using default")
        void shouldHandleZeroMassByUsingDefault() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(Set.of(entityId));

            // Mock position from exports (returns empty to test fallback)
            when(gridMapExports.getPosition(entityId)).thenReturn(Optional.empty());

            // Mock with zero mass and other components
            when(store.getComponent(eq(entityId), any())).thenReturn(0f);
            when(store.getComponent(entityId, MASS)).thenReturn(0f);
            when(store.getComponent(entityId, INERTIA)).thenReturn(0f);

            Optional<RigidBody> result = repository.findById(entityId);

            assertThat(result).isPresent();
            assertThat(result.get().mass()).isEqualTo(1.0f); // Default
            assertThat(result.get().inertia()).isEqualTo(1.0f); // Default
        }
    }

    @Nested
    @DisplayName("findAllIds")
    class FindAllIds {

        @Test
        @DisplayName("should return all entity IDs with rigid body flag")
        void shouldReturnAllEntityIdsWithRigidBodyFlag() {
            Set<Long> expectedIds = Set.of(1L, 2L, 3L);
            when(store.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(expectedIds);

            Set<Long> result = repository.findAllIds();

            assertThat(result).isEqualTo(expectedIds);
        }

        @Test
        @DisplayName("should return empty set when no entities exist")
        void shouldReturnEmptySetWhenNoEntitiesExist() {
            when(store.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(Set.of());

            Set<Long> result = repository.findAllIds();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("should return true when entity exists")
        void shouldReturnTrueWhenEntityExists() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(Set.of(entityId, 100L));

            boolean result = repository.exists(entityId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when entity does not exist")
        void shouldReturnFalseWhenEntityDoesNotExist() {
            long entityId = 42L;
            when(store.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(Set.of(100L, 200L));

            boolean result = repository.exists(entityId);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should remove all core components")
        void shouldRemoveAllCoreComponents() {
            long entityId = 42L;

            repository.delete(entityId);

            for (var component : CORE_COMPONENTS) {
                verify(store).removeComponent(entityId, component);
            }
        }

        @Test
        @DisplayName("should remove flag component")
        void shouldRemoveFlagComponent() {
            long entityId = 42L;

            repository.delete(entityId);

            verify(store).removeComponent(entityId, FLAG);
        }
    }

    @Nested
    @DisplayName("updatePosition")
    class UpdatePosition {

        @Test
        @DisplayName("should set position through EntityModuleExports")
        void shouldSetPositionThroughExports() {
            long entityId = 42L;
            Vector3 position = new Vector3(10, 20, 30);

            repository.updatePosition(entityId, position);

            verify(gridMapExports).setPosition(entityId, 10f, 20f, 30f);
        }
    }

    @Nested
    @DisplayName("updateVelocity")
    class UpdateVelocity {

        @Test
        @DisplayName("should attach velocity components")
        void shouldAttachVelocityComponents() {
            long entityId = 42L;
            Vector3 velocity = new Vector3(5, 10, 15);

            repository.updateVelocity(entityId, velocity);

            ArgumentCaptor<float[]> captor = ArgumentCaptor.forClass(float[].class);
            verify(store).attachComponents(eq(entityId), eq(VELOCITY_COMPONENTS), captor.capture());

            float[] values = captor.getValue();
            assertThat(values[0]).isEqualTo(5f);
            assertThat(values[1]).isEqualTo(10f);
            assertThat(values[2]).isEqualTo(15f);
        }
    }

    @Nested
    @DisplayName("updateForce")
    class UpdateForce {

        @Test
        @DisplayName("should attach force components")
        void shouldAttachForceComponents() {
            long entityId = 42L;
            Vector3 force = new Vector3(100, 200, 300);

            repository.updateForce(entityId, force);

            ArgumentCaptor<float[]> captor = ArgumentCaptor.forClass(float[].class);
            verify(store).attachComponents(eq(entityId), eq(FORCE_COMPONENTS), captor.capture());

            float[] values = captor.getValue();
            assertThat(values[0]).isEqualTo(100f);
            assertThat(values[1]).isEqualTo(200f);
            assertThat(values[2]).isEqualTo(300f);
        }
    }

    @Nested
    @DisplayName("updateAngular")
    class UpdateAngular {

        @Test
        @DisplayName("should attach angular components")
        void shouldAttachAngularComponents() {
            long entityId = 42L;
            float angularVelocity = 1.5f;
            float rotation = 3.14f;
            float torque = 10.0f;

            repository.updateAngular(entityId, angularVelocity, rotation, torque);

            verify(store).attachComponent(entityId, ANGULAR_VELOCITY, angularVelocity);
            verify(store).attachComponent(entityId, ROTATION, rotation);
            verify(store).attachComponent(entityId, TORQUE, torque);
        }
    }

    @Nested
    @DisplayName("dynamic store resolution")
    class DynamicStoreResolution {

        @Test
        @DisplayName("should use current store from context, not cached reference")
        void shouldUseCurrentStoreFromContextNotCachedReference() {
            // This test proves the fix for the stale store reference bug.
            // Previously, repositories captured the store at construction time,
            // which caused issues when the JWT was issued and context updated.

            // Create a second mock store (simulating JWT-scoped store)
            EntityComponentStore newStore = mock(EntityComponentStore.class);

            // Simulate what happens when JWT is issued: context returns new store
            when(context.getEntityComponentStore()).thenReturn(newStore);

            // Perform an operation
            long entityId = 99L;
            when(newStore.getEntitiesWithComponents(List.of(FLAG)))
                    .thenReturn(Set.of(entityId));

            // Verify repository uses the NEW store, not the old one
            repository.findAllIds();

            // The new store should be called, not the original 'store' mock
            verify(newStore).getEntitiesWithComponents(List.of(FLAG));
            verify(store, never()).getEntitiesWithComponents(anyList());
        }
    }
}
