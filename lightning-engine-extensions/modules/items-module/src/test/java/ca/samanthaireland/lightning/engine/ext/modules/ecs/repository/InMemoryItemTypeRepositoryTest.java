package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import ca.samanthaireland.lightning.engine.ext.modules.domain.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InMemoryItemTypeRepository}.
 */
@DisplayName("InMemoryItemTypeRepository")
class InMemoryItemTypeRepositoryTest {

    private InMemoryItemTypeRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryItemTypeRepository();
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should assign unique ID to item type")
        void shouldAssignUniqueIdToItemType() {
            long matchId = 1L;
            ItemType type = ItemType.create("Sword", 1, 2, 100f, 5f, 0f, 25f, 0f);

            ItemType saved = repository.save(matchId, type);

            assertThat(saved.id()).isGreaterThan(0);
            assertThat(saved.name()).isEqualTo("Sword");
        }

        @Test
        @DisplayName("should assign incrementing IDs")
        void shouldAssignIncrementingIds() {
            long matchId = 1L;
            ItemType type1 = ItemType.create("Sword", 1, 2, 100f, 5f, 0f, 25f, 0f);
            ItemType type2 = ItemType.create("Shield", 1, 1, 80f, 10f, 0f, 0f, 15f);

            ItemType saved1 = repository.save(matchId, type1);
            ItemType saved2 = repository.save(matchId, type2);

            assertThat(saved2.id()).isGreaterThan(saved1.id());
        }

        @Test
        @DisplayName("should isolate IDs per match")
        void shouldIsolateIdsPerMatch() {
            ItemType type = ItemType.create("Sword", 1, 2, 100f, 5f, 0f, 25f, 0f);

            ItemType saved1 = repository.save(1L, type);
            ItemType saved2 = repository.save(2L, type);

            assertThat(saved1.id()).isEqualTo(saved2.id()); // Both start at 1
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return item type when exists")
        void shouldReturnItemTypeWhenExists() {
            long matchId = 1L;
            ItemType type = ItemType.create("Sword", 1, 2, 100f, 5f, 0f, 25f, 0f);
            ItemType saved = repository.save(matchId, type);

            Optional<ItemType> result = repository.findById(matchId, saved.id());

            assertThat(result).contains(saved);
        }

        @Test
        @DisplayName("should return empty when type not exists")
        void shouldReturnEmptyWhenTypeNotExists() {
            Optional<ItemType> result = repository.findById(1L, 999L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when match not exists")
        void shouldReturnEmptyWhenMatchNotExists() {
            ItemType type = ItemType.create("Sword", 1, 2, 100f, 5f, 0f, 25f, 0f);
            ItemType saved = repository.save(1L, type);

            Optional<ItemType> result = repository.findById(2L, saved.id());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllByMatchId")
    class FindAllByMatchId {

        @Test
        @DisplayName("should return all types for match")
        void shouldReturnAllTypesForMatch() {
            long matchId = 1L;
            ItemType saved1 = repository.save(matchId, ItemType.create("Sword", 1, 2, 100f, 5f, 0f, 25f, 0f));
            ItemType saved2 = repository.save(matchId, ItemType.create("Shield", 1, 1, 80f, 10f, 0f, 0f, 15f));

            Collection<ItemType> result = repository.findAllByMatchId(matchId);

            assertThat(result).containsExactlyInAnyOrder(saved1, saved2);
        }

        @Test
        @DisplayName("should return empty collection when no types exist")
        void shouldReturnEmptyCollectionWhenNoTypesExist() {
            Collection<ItemType> result = repository.findAllByMatchId(999L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should only return types for specified match")
        void shouldOnlyReturnTypesForSpecifiedMatch() {
            ItemType saved1 = repository.save(1L, ItemType.create("Sword", 1, 2, 100f, 5f, 0f, 25f, 0f));
            repository.save(2L, ItemType.create("Shield", 1, 1, 80f, 10f, 0f, 0f, 15f));

            Collection<ItemType> result = repository.findAllByMatchId(1L);

            assertThat(result).containsExactly(saved1);
        }
    }

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("should return true when type exists")
        void shouldReturnTrueWhenTypeExists() {
            long matchId = 1L;
            ItemType type = ItemType.create("Sword", 1, 2, 100f, 5f, 0f, 25f, 0f);
            ItemType saved = repository.save(matchId, type);

            boolean result = repository.exists(matchId, saved.id());

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when type not exists")
        void shouldReturnFalseWhenTypeNotExists() {
            boolean result = repository.exists(1L, 999L);

            assertThat(result).isFalse();
        }
    }
}
