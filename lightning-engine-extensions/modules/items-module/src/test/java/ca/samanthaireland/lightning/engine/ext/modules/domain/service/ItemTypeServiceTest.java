package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.ext.modules.domain.ItemType;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.ItemTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ItemTypeService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ItemTypeService")
class ItemTypeServiceTest {

    @Mock
    private ItemTypeRepository itemTypeRepository;

    private ItemTypeService itemTypeService;

    @BeforeEach
    void setUp() {
        itemTypeService = new ItemTypeService(itemTypeRepository);
    }

    @Nested
    @DisplayName("createItemType")
    class CreateItemType {

        @Test
        @DisplayName("should create item type with specified properties")
        void shouldCreateItemTypeWithSpecifiedProperties() {
            long matchId = 1L;
            ItemType savedType = new ItemType(100L, "Health Potion", 10, 1, 50f, 0.5f, 100f, 0f, 0f);
            when(itemTypeRepository.save(eq(matchId), any(ItemType.class))).thenReturn(savedType);

            ItemType result = itemTypeService.createItemType(
                    matchId, "Health Potion", 10, 1, 50f, 0.5f, 100f, 0f, 0f);

            assertThat(result).isEqualTo(savedType);
            verify(itemTypeRepository).save(eq(matchId), any(ItemType.class));
        }

        @Test
        @DisplayName("should throw for invalid name")
        void shouldThrowForInvalidName() {
            assertThatThrownBy(() -> itemTypeService.createItemType(
                    1L, "", 10, 1, 50f, 0.5f, 100f, 0f, 0f))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw for invalid maxStack")
        void shouldThrowForInvalidMaxStack() {
            assertThatThrownBy(() -> itemTypeService.createItemType(
                    1L, "Sword", 0, 1, 50f, 0.5f, 0f, 10f, 0f))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return item type when found")
        void shouldReturnItemTypeWhenFound() {
            long matchId = 1L;
            long itemTypeId = 100L;
            ItemType itemType = new ItemType(itemTypeId, "Sword", 1, 2, 100f, 5f, 0f, 25f, 0f);
            when(itemTypeRepository.findById(matchId, itemTypeId)).thenReturn(Optional.of(itemType));

            Optional<ItemType> result = itemTypeService.findById(matchId, itemTypeId);

            assertThat(result).contains(itemType);
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            long matchId = 1L;
            long itemTypeId = 100L;
            when(itemTypeRepository.findById(matchId, itemTypeId)).thenReturn(Optional.empty());

            Optional<ItemType> result = itemTypeService.findById(matchId, itemTypeId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAllItemTypes")
    class GetAllItemTypes {

        @Test
        @DisplayName("should return all item types for match")
        void shouldReturnAllItemTypesForMatch() {
            long matchId = 1L;
            ItemType type1 = new ItemType(1L, "Sword", 1, 2, 100f, 5f, 0f, 25f, 0f);
            ItemType type2 = new ItemType(2L, "Potion", 10, 0, 50f, 0.5f, 100f, 0f, 0f);
            when(itemTypeRepository.findAllByMatchId(matchId)).thenReturn(List.of(type1, type2));

            Collection<ItemType> result = itemTypeService.getAllItemTypes(matchId);

            assertThat(result).containsExactlyInAnyOrder(type1, type2);
        }

        @Test
        @DisplayName("should return empty collection when no types exist")
        void shouldReturnEmptyCollectionWhenNoTypesExist() {
            long matchId = 1L;
            when(itemTypeRepository.findAllByMatchId(matchId)).thenReturn(List.of());

            Collection<ItemType> result = itemTypeService.getAllItemTypes(matchId);

            assertThat(result).isEmpty();
        }
    }
}
