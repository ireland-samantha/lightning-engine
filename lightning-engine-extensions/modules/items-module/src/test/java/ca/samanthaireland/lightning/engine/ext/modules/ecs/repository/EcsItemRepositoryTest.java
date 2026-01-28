package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.core.store.EntityComponentStore;
import ca.samanthaireland.lightning.engine.ext.modules.EntityModuleFactory;
import ca.samanthaireland.lightning.engine.ext.modules.GridMapModuleFactory;
import ca.samanthaireland.lightning.engine.ext.modules.ItemsModuleFactory;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ca.samanthaireland.lightning.engine.ext.modules.ItemsModuleFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EcsItemRepository}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EcsItemRepository")
class EcsItemRepositoryTest {

    @Mock
    private EntityComponentStore store;

    private EcsItemRepository repository;

    @BeforeEach
    void setUp() {
        repository = new EcsItemRepository(store);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should create entity and attach components")
        void shouldCreateEntityAndAttachComponents() {
            long matchId = 1L;
            long createdEntityId = 100L;
            Item item = Item.createDefault(10L, 5, 100f, 200f);

            when(store.createEntityForMatch(matchId)).thenReturn(createdEntityId);

            Item result = repository.save(matchId, item);

            assertThat(result.id()).isEqualTo(createdEntityId);
            assertThat(result.itemTypeId()).isEqualTo(10L);
            assertThat(result.stackSize()).isEqualTo(5);

            verify(store).createEntityForMatch(matchId);
            verify(store).attachComponent(createdEntityId, ITEM_TYPE_ID, 10L);
            verify(store).attachComponent(createdEntityId, STACK_SIZE, 5);
            verify(store).attachComponent(createdEntityId, FLAG, 1.0f);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return item when entity exists")
        void shouldReturnItemWhenEntityExists() {
            long itemEntityId = 100L;
            when(store.getEntitiesWithComponents(List.of(FLAG))).thenReturn(Set.of(itemEntityId));
            when(store.getComponent(itemEntityId, FLAG)).thenReturn(1.0f);
            when(store.getComponent(itemEntityId, ITEM_TYPE_ID)).thenReturn(10f);
            when(store.getComponent(itemEntityId, STACK_SIZE)).thenReturn(5f);
            when(store.getComponent(itemEntityId, MAX_STACK)).thenReturn(10f);
            when(store.getComponent(itemEntityId, OWNER_ENTITY_ID)).thenReturn(0f);
            when(store.getComponent(itemEntityId, SLOT_INDEX)).thenReturn(-1f);
            when(store.getComponent(itemEntityId, GridMapModuleFactory.POSITION_X)).thenReturn(100f);
            when(store.getComponent(itemEntityId, GridMapModuleFactory.POSITION_Y)).thenReturn(200f);
            when(store.getComponent(itemEntityId, ITEM_NAME_HASH)).thenReturn(12345f);
            when(store.getComponent(itemEntityId, ITEM_RARITY)).thenReturn(1f);
            when(store.getComponent(itemEntityId, ITEM_VALUE)).thenReturn(50f);
            when(store.getComponent(itemEntityId, ITEM_WEIGHT)).thenReturn(0.5f);
            when(store.getComponent(itemEntityId, HEAL_AMOUNT)).thenReturn(100f);
            when(store.getComponent(itemEntityId, DAMAGE_BONUS)).thenReturn(0f);
            when(store.getComponent(itemEntityId, ARMOR_VALUE)).thenReturn(0f);

            Optional<Item> result = repository.findById(itemEntityId);

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(itemEntityId);
            assertThat(result.get().itemTypeId()).isEqualTo(10L);
            assertThat(result.get().stackSize()).isEqualTo(5);
            assertThat(result.get().healAmount()).isEqualTo(100f);
        }

        @Test
        @DisplayName("should return empty when entity not found")
        void shouldReturnEmptyWhenEntityNotFound() {
            long itemEntityId = 100L;
            when(store.getEntitiesWithComponents(List.of(FLAG))).thenReturn(Set.of(200L));

            Optional<Item> result = repository.findById(itemEntityId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when flag is zero")
        void shouldReturnEmptyWhenFlagIsZero() {
            long itemEntityId = 100L;
            when(store.getEntitiesWithComponents(List.of(FLAG))).thenReturn(Set.of(itemEntityId));
            when(store.getComponent(itemEntityId, FLAG)).thenReturn(0f);

            Optional<Item> result = repository.findById(itemEntityId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateOwnership")
    class UpdateOwnership {

        @Test
        @DisplayName("should attach owner and slot components")
        void shouldAttachOwnerAndSlotComponents() {
            long itemEntityId = 100L;
            long ownerEntityId = 42L;
            int slotIndex = 3;

            repository.updateOwnership(itemEntityId, ownerEntityId, slotIndex);

            verify(store).attachComponent(itemEntityId, OWNER_ENTITY_ID, (float) 42L);
            verify(store).attachComponent(itemEntityId, SLOT_INDEX, (float) 3);
        }
    }

    @Nested
    @DisplayName("updatePosition")
    class UpdatePosition {

        @Test
        @DisplayName("should attach position components")
        void shouldAttachPositionComponents() {
            long itemEntityId = 100L;

            repository.updatePosition(itemEntityId, 300f, 400f);

            verify(store).attachComponent(itemEntityId, GridMapModuleFactory.POSITION_X, 300f);
            verify(store).attachComponent(itemEntityId, GridMapModuleFactory.POSITION_Y, 400f);
        }
    }

    @Nested
    @DisplayName("updateStackSize")
    class UpdateStackSize {

        @Test
        @DisplayName("should attach stack size component")
        void shouldAttachStackSizeComponent() {
            long itemEntityId = 100L;

            repository.updateStackSize(itemEntityId, 7);

            verify(store).attachComponent(itemEntityId, STACK_SIZE, (float) 7);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should remove core components and flag")
        void shouldRemoveCoreComponentsAndFlag() {
            long itemEntityId = 100L;

            repository.delete(itemEntityId);

            for (var component : CORE_COMPONENTS) {
                verify(store).removeComponent(itemEntityId, component);
            }
            verify(store).removeComponent(itemEntityId, FLAG);
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("should return true when entity exists with positive flag")
        void shouldReturnTrueWhenEntityExistsWithPositiveFlag() {
            long itemEntityId = 100L;
            when(store.getEntitiesWithComponents(List.of(FLAG))).thenReturn(Set.of(itemEntityId));
            when(store.getComponent(itemEntityId, FLAG)).thenReturn(1.0f);

            boolean result = repository.exists(itemEntityId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when entity not found")
        void shouldReturnFalseWhenEntityNotFound() {
            long itemEntityId = 100L;
            when(store.getEntitiesWithComponents(List.of(FLAG))).thenReturn(Set.of(200L));

            boolean result = repository.exists(itemEntityId);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when flag is zero")
        void shouldReturnFalseWhenFlagIsZero() {
            long itemEntityId = 100L;
            when(store.getEntitiesWithComponents(List.of(FLAG))).thenReturn(Set.of(itemEntityId));
            when(store.getComponent(itemEntityId, FLAG)).thenReturn(0f);

            boolean result = repository.exists(itemEntityId);

            assertThat(result).isFalse();
        }
    }
}
