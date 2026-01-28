package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.GridMap;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.MapMatchRepository;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.MapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MapService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MapService")
class GridMapServiceTest {

    @Mock
    private MapRepository mapRepository;

    @Mock
    private MapMatchRepository mapMatchRepository;

    private MapService mapService;

    @BeforeEach
    void setUp() {
        mapService = new MapService(mapRepository, mapMatchRepository);
    }

    @Nested
    @DisplayName("createMap")
    class CreateGridMap {

        @Test
        @DisplayName("should create map with specified dimensions")
        void shouldCreateMapWithSpecifiedDimensions() {
            long matchId = 1L;
            GridMap savedGridMap = new GridMap(100L, 20, 15, 3);
            when(mapRepository.save(eq(matchId), any(GridMap.class))).thenReturn(savedGridMap);

            GridMap result = mapService.createMap(matchId, 20, 15, 3);

            assertThat(result).isEqualTo(savedGridMap);
            verify(mapRepository).save(eq(matchId), any(GridMap.class));
        }

        @Test
        @DisplayName("should throw for invalid width")
        void shouldThrowForInvalidWidth() {
            assertThatThrownBy(() -> mapService.createMap(1L, 0, 10, 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw for invalid height")
        void shouldThrowForInvalidHeight() {
            assertThatThrownBy(() -> mapService.createMap(1L, 10, -5, 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw for invalid depth")
        void shouldThrowForInvalidDepth() {
            assertThatThrownBy(() -> mapService.createMap(1L, 10, 10, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return map when found")
        void shouldReturnMapWhenFound() {
            long mapId = 100L;
            GridMap gridMap = new GridMap(mapId, 10, 10, 1);
            when(mapRepository.findById(mapId)).thenReturn(Optional.of(gridMap));

            GridMap result = mapService.findById(mapId);

            assertThat(result).isEqualTo(gridMap);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when not found")
        void shouldThrowEntityNotFoundExceptionWhenNotFound() {
            long mapId = 100L;
            when(mapRepository.findById(mapId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mapService.findById(mapId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("assignMapToMatch")
    class AssignGridMapToMatch {

        @Test
        @DisplayName("should assign map to match when map exists")
        void shouldAssignMapToMatchWhenMapExists() {
            long matchId = 1L;
            long mapId = 100L;
            when(mapRepository.exists(mapId)).thenReturn(true);

            mapService.assignMapToMatch(matchId, mapId);

            verify(mapMatchRepository).assignMapToMatch(matchId, mapId);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when map does not exist")
        void shouldThrowEntityNotFoundExceptionWhenMapDoesNotExist() {
            long matchId = 1L;
            long mapId = 100L;
            when(mapRepository.exists(mapId)).thenReturn(false);

            assertThatThrownBy(() -> mapService.assignMapToMatch(matchId, mapId))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(mapMatchRepository, never()).assignMapToMatch(matchId, mapId);
        }
    }

    @Nested
    @DisplayName("findMapByMatchId")
    class FindGridMapByMatchId {

        @Test
        @DisplayName("should return map when match has assigned map")
        void shouldReturnMapWhenMatchHasAssignedMap() {
            long matchId = 1L;
            long mapId = 100L;
            GridMap gridMap = new GridMap(mapId, 10, 10, 1);
            when(mapMatchRepository.findMapIdByMatchId(matchId)).thenReturn(Optional.of(mapId));
            when(mapRepository.findById(mapId)).thenReturn(Optional.of(gridMap));

            Optional<GridMap> result = mapService.findMapByMatchId(matchId);

            assertThat(result).contains(gridMap);
        }

        @Test
        @DisplayName("should return empty when match has no assigned map")
        void shouldReturnEmptyWhenMatchHasNoAssignedMap() {
            long matchId = 1L;
            when(mapMatchRepository.findMapIdByMatchId(matchId)).thenReturn(Optional.empty());

            Optional<GridMap> result = mapService.findMapByMatchId(matchId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when assigned map no longer exists")
        void shouldReturnEmptyWhenAssignedMapNoLongerExists() {
            long matchId = 1L;
            long mapId = 100L;
            when(mapMatchRepository.findMapIdByMatchId(matchId)).thenReturn(Optional.of(mapId));
            when(mapRepository.findById(mapId)).thenReturn(Optional.empty());

            Optional<GridMap> result = mapService.findMapByMatchId(matchId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasAssignedMap")
    class HasAssignedGridMap {

        @Test
        @DisplayName("should return true when match has assigned map")
        void shouldReturnTrueWhenMatchHasAssignedMap() {
            long matchId = 1L;
            when(mapMatchRepository.hasAssignedMap(matchId)).thenReturn(true);

            boolean result = mapService.hasAssignedMap(matchId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when match has no assigned map")
        void shouldReturnFalseWhenMatchHasNoAssignedMap() {
            long matchId = 1L;
            when(mapMatchRepository.hasAssignedMap(matchId)).thenReturn(false);

            boolean result = mapService.hasAssignedMap(matchId);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("removeMapAssignment")
    class RemoveMapAssignment {

        @Test
        @DisplayName("should delegate to repository")
        void shouldDelegateToRepository() {
            long matchId = 1L;

            mapService.removeMapAssignment(matchId);

            verify(mapMatchRepository).removeMapAssignment(matchId);
        }
    }
}
