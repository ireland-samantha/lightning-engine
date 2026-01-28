package ca.samanthaireland.lightning.engine.ext.modules.ecs.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EcsMapMatchRepository}.
 */
@DisplayName("EcsMapMatchRepository")
class EcsGridMapMatchRepositoryTest {

    private EcsMapMatchRepository repository;

    @BeforeEach
    void setUp() {
        repository = new EcsMapMatchRepository();
    }

    @Nested
    @DisplayName("assignMapToMatch")
    class AssignGridMapToMatch {

        @Test
        @DisplayName("should store the map assignment")
        void shouldStoreTheMapAssignment() {
            long matchId = 1L;
            long mapId = 100L;

            repository.assignMapToMatch(matchId, mapId);

            assertThat(repository.findMapIdByMatchId(matchId)).contains(mapId);
        }

        @Test
        @DisplayName("should replace existing assignment")
        void shouldReplaceExistingAssignment() {
            long matchId = 1L;
            long firstMapId = 100L;
            long secondMapId = 200L;

            repository.assignMapToMatch(matchId, firstMapId);
            repository.assignMapToMatch(matchId, secondMapId);

            assertThat(repository.findMapIdByMatchId(matchId)).contains(secondMapId);
        }

        @Test
        @DisplayName("should support multiple matches with different maps")
        void shouldSupportMultipleMatchesWithDifferentMaps() {
            long matchId1 = 1L;
            long matchId2 = 2L;
            long mapId1 = 100L;
            long mapId2 = 200L;

            repository.assignMapToMatch(matchId1, mapId1);
            repository.assignMapToMatch(matchId2, mapId2);

            assertThat(repository.findMapIdByMatchId(matchId1)).contains(mapId1);
            assertThat(repository.findMapIdByMatchId(matchId2)).contains(mapId2);
        }
    }

    @Nested
    @DisplayName("findMapIdByMatchId")
    class FindMapIdByMatchId {

        @Test
        @DisplayName("should return empty when no assignment exists")
        void shouldReturnEmptyWhenNoAssignmentExists() {
            long matchId = 1L;

            Optional<Long> result = repository.findMapIdByMatchId(matchId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return map ID when assignment exists")
        void shouldReturnMapIdWhenAssignmentExists() {
            long matchId = 1L;
            long mapId = 100L;
            repository.assignMapToMatch(matchId, mapId);

            Optional<Long> result = repository.findMapIdByMatchId(matchId);

            assertThat(result).contains(mapId);
        }
    }

    @Nested
    @DisplayName("hasAssignedMap")
    class HasAssignedGridMap {

        @Test
        @DisplayName("should return false when no assignment exists")
        void shouldReturnFalseWhenNoAssignmentExists() {
            long matchId = 1L;

            boolean result = repository.hasAssignedMap(matchId);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return true when assignment exists")
        void shouldReturnTrueWhenAssignmentExists() {
            long matchId = 1L;
            long mapId = 100L;
            repository.assignMapToMatch(matchId, mapId);

            boolean result = repository.hasAssignedMap(matchId);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("removeMapAssignment")
    class RemoveMapAssignment {

        @Test
        @DisplayName("should remove existing assignment")
        void shouldRemoveExistingAssignment() {
            long matchId = 1L;
            long mapId = 100L;
            repository.assignMapToMatch(matchId, mapId);

            repository.removeMapAssignment(matchId);

            assertThat(repository.findMapIdByMatchId(matchId)).isEmpty();
        }

        @Test
        @DisplayName("should not throw when no assignment exists")
        void shouldNotThrowWhenNoAssignmentExists() {
            long matchId = 1L;

            repository.removeMapAssignment(matchId);

            assertThat(repository.findMapIdByMatchId(matchId)).isEmpty();
        }

        @Test
        @DisplayName("should not affect other matches")
        void shouldNotAffectOtherMatches() {
            long matchId1 = 1L;
            long matchId2 = 2L;
            long mapId1 = 100L;
            long mapId2 = 200L;
            repository.assignMapToMatch(matchId1, mapId1);
            repository.assignMapToMatch(matchId2, mapId2);

            repository.removeMapAssignment(matchId1);

            assertThat(repository.findMapIdByMatchId(matchId1)).isEmpty();
            assertThat(repository.findMapIdByMatchId(matchId2)).contains(mapId2);
        }
    }
}
