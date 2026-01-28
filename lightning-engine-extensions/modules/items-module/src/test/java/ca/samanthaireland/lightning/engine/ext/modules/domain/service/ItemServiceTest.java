package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.ext.modules.domain.Item;
import ca.samanthaireland.lightning.engine.ext.modules.domain.ItemType;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.ItemRepository;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.ItemTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ItemService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ItemService")
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemTypeRepository itemTypeRepository;

    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemService(itemRepository, itemTypeRepository);
    }

    @Nested
    @DisplayName("spawnItem")
    class SpawnItem {

        @Test
        @DisplayName("should spawn item with type properties when type exists")
        void shouldSpawnItemWithTypePropertiesWhenTypeExists() {
            long matchId = 1L;
            long itemTypeId = 10L;
            ItemType type = new ItemType(itemTypeId, "Health Potion", 10, 1, 50f, 0.5f, 100f, 0f, 0f);
            Item savedItem = new Item(100L, itemTypeId, 5, 10, 0, -1, 100f, 200f,
                    type.name().hashCode(), 1, 50f, 0.5f, 100f, 0f, 0f);

            when(itemTypeRepository.findById(matchId, itemTypeId)).thenReturn(Optional.of(type));
            when(itemRepository.save(eq(matchId), any(Item.class))).thenReturn(savedItem);

            Item result = itemService.spawnItem(matchId, itemTypeId, 100f, 200f, 5);

            assertThat(result.id()).isEqualTo(100L);
            assertThat(result.healAmount()).isEqualTo(100f);

            ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
            verify(itemRepository).save(eq(matchId), itemCaptor.capture());
            Item capturedItem = itemCaptor.getValue();
            assertThat(capturedItem.maxStack()).isEqualTo(10);
            assertThat(capturedItem.healAmount()).isEqualTo(100f);
        }

        @Test
        @DisplayName("should spawn item with default properties when type not found")
        void shouldSpawnItemWithDefaultPropertiesWhenTypeNotFound() {
            long matchId = 1L;
            long itemTypeId = 999L;
            Item savedItem = Item.createDefault(itemTypeId, 5, 100f, 200f);
            savedItem = new Item(100L, savedItem.itemTypeId(), savedItem.stackSize(), savedItem.maxStack(),
                    savedItem.ownerEntityId(), savedItem.slotIndex(), savedItem.positionX(), savedItem.positionY(),
                    savedItem.nameHash(), savedItem.rarity(), savedItem.value(), savedItem.weight(),
                    savedItem.healAmount(), savedItem.damageBonus(), savedItem.armorValue());

            when(itemTypeRepository.findById(matchId, itemTypeId)).thenReturn(Optional.empty());
            when(itemRepository.save(eq(matchId), any(Item.class))).thenReturn(savedItem);

            Item result = itemService.spawnItem(matchId, itemTypeId, 100f, 200f, 5);

            assertThat(result.id()).isEqualTo(100L);
            assertThat(result.maxStack()).isEqualTo(100); // default max stack
        }
    }

    @Nested
    @DisplayName("pickupItem")
    class PickupItem {

        @Test
        @DisplayName("should pick up item when on ground")
        void shouldPickUpItemWhenOnGround() {
            long itemEntityId = 100L;
            long pickerEntityId = 42L;
            int slotIndex = 3;
            Item item = new Item(itemEntityId, 10L, 5, 10, 0, -1, 100f, 200f, 0, 0, 0f, 0f, 0f, 0f, 0f);

            when(itemRepository.findById(itemEntityId)).thenReturn(Optional.of(item));

            boolean result = itemService.pickupItem(itemEntityId, pickerEntityId, slotIndex);

            assertThat(result).isTrue();
            verify(itemRepository).updateOwnership(itemEntityId, pickerEntityId, slotIndex);
        }

        @Test
        @DisplayName("should fail when item not found")
        void shouldFailWhenItemNotFound() {
            long itemEntityId = 100L;
            when(itemRepository.findById(itemEntityId)).thenReturn(Optional.empty());

            boolean result = itemService.pickupItem(itemEntityId, 42L, 0);

            assertThat(result).isFalse();
            verify(itemRepository, never()).updateOwnership(anyLong(), anyLong(), anyInt());
        }

        @Test
        @DisplayName("should fail when item already owned")
        void shouldFailWhenItemAlreadyOwned() {
            long itemEntityId = 100L;
            Item item = new Item(itemEntityId, 10L, 5, 10, 50L, 0, 100f, 200f, 0, 0, 0f, 0f, 0f, 0f, 0f);

            when(itemRepository.findById(itemEntityId)).thenReturn(Optional.of(item));

            boolean result = itemService.pickupItem(itemEntityId, 42L, 0);

            assertThat(result).isFalse();
            verify(itemRepository, never()).updateOwnership(anyLong(), anyLong(), anyInt());
        }
    }

    @Nested
    @DisplayName("dropItem")
    class DropItem {

        @Test
        @DisplayName("should drop item at position")
        void shouldDropItemAtPosition() {
            long itemEntityId = 100L;
            Item item = new Item(itemEntityId, 10L, 5, 10, 42L, 0, 0f, 0f, 0, 0, 0f, 0f, 0f, 0f, 0f);

            when(itemRepository.findById(itemEntityId)).thenReturn(Optional.of(item));

            boolean result = itemService.dropItem(itemEntityId, 300f, 400f);

            assertThat(result).isTrue();
            verify(itemRepository).updateOwnership(itemEntityId, 0, -1);
            verify(itemRepository).updatePosition(itemEntityId, 300f, 400f);
        }

        @Test
        @DisplayName("should fail when item not found")
        void shouldFailWhenItemNotFound() {
            long itemEntityId = 100L;
            when(itemRepository.findById(itemEntityId)).thenReturn(Optional.empty());

            boolean result = itemService.dropItem(itemEntityId, 300f, 400f);

            assertThat(result).isFalse();
            verify(itemRepository, never()).updateOwnership(anyLong(), anyLong(), anyInt());
            verify(itemRepository, never()).updatePosition(anyLong(), anyFloat(), anyFloat());
        }
    }

    @Nested
    @DisplayName("useItem")
    class UseItem {

        @Test
        @DisplayName("should use item and reduce stack")
        void shouldUseItemAndReduceStack() {
            long itemEntityId = 100L;
            Item item = new Item(itemEntityId, 10L, 5, 10, 42L, 0, 0f, 0f, 0, 0, 0f, 0f, 100f, 0f, 0f);

            when(itemRepository.findById(itemEntityId)).thenReturn(Optional.of(item));

            float healAmount = itemService.useItem(itemEntityId, 42L);

            assertThat(healAmount).isEqualTo(100f);
            verify(itemRepository).updateStackSize(itemEntityId, 4);
            verify(itemRepository, never()).delete(anyLong());
        }

        @Test
        @DisplayName("should delete item when stack depleted")
        void shouldDeleteItemWhenStackDepleted() {
            long itemEntityId = 100L;
            Item item = new Item(itemEntityId, 10L, 1, 10, 42L, 0, 0f, 0f, 0, 0, 0f, 0f, 100f, 0f, 0f);

            when(itemRepository.findById(itemEntityId)).thenReturn(Optional.of(item));

            float healAmount = itemService.useItem(itemEntityId, 42L);

            assertThat(healAmount).isEqualTo(100f);
            verify(itemRepository).delete(itemEntityId);
            verify(itemRepository, never()).updateStackSize(anyLong(), anyInt());
        }

        @Test
        @DisplayName("should return 0 when item not found")
        void shouldReturnZeroWhenItemNotFound() {
            long itemEntityId = 100L;
            when(itemRepository.findById(itemEntityId)).thenReturn(Optional.empty());

            float healAmount = itemService.useItem(itemEntityId, 42L);

            assertThat(healAmount).isEqualTo(0f);
            verify(itemRepository, never()).delete(anyLong());
            verify(itemRepository, never()).updateStackSize(anyLong(), anyInt());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return item when found")
        void shouldReturnItemWhenFound() {
            long itemEntityId = 100L;
            Item item = new Item(itemEntityId, 10L, 5, 10, 0, -1, 100f, 200f, 0, 0, 0f, 0f, 0f, 0f, 0f);
            when(itemRepository.findById(itemEntityId)).thenReturn(Optional.of(item));

            Optional<Item> result = itemService.findById(itemEntityId);

            assertThat(result).contains(item);
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            long itemEntityId = 100L;
            when(itemRepository.findById(itemEntityId)).thenReturn(Optional.empty());

            Optional<Item> result = itemService.findById(itemEntityId);

            assertThat(result).isEmpty();
        }
    }
}
