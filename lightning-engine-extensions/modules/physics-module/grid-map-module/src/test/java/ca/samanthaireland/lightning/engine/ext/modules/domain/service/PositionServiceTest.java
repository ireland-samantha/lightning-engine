package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.GridMap;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Position;
import ca.samanthaireland.lightning.engine.ext.modules.domain.PositionOutOfBoundsException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.MapRepository;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.PositionRepository;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PositionService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PositionService")
class PositionServiceTest {

    @Mock
    private MapRepository mapRepository;

    @Mock
    private PositionRepository positionRepository;

    private PositionService positionService;

    @BeforeEach
    void setUp() {
        positionService = new PositionService(mapRepository, positionRepository);
    }

    @Nested
    @DisplayName("setPosition")
    class SetPosition {

        private final long entityId = 42L;
        private final long mapId = 100L;
        private final GridMap gridMap = new GridMap(mapId, 10, 10, 5);

        @Test
        @DisplayName("should save position when within bounds")
        void shouldSavePositionWhenWithinBounds() {
            when(mapRepository.findById(mapId)).thenReturn(Optional.of(gridMap));
            Position position = new Position(5, 5, 2);

            positionService.setPosition(entityId, mapId, position);

            verify(positionRepository).save(entityId, position);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when map not found")
        void shouldThrowEntityNotFoundExceptionWhenMapNotFound() {
            when(mapRepository.findById(mapId)).thenReturn(Optional.empty());
            Position position = new Position(5, 5, 2);

            assertThatThrownBy(() -> positionService.setPosition(entityId, mapId, position))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(positionRepository, never()).save(anyLong(), any());
        }

        @Test
        @DisplayName("should throw PositionOutOfBoundsException when x out of bounds")
        void shouldThrowPositionOutOfBoundsExceptionWhenXOutOfBounds() {
            when(mapRepository.findById(mapId)).thenReturn(Optional.of(gridMap));
            Position position = new Position(15, 5, 0);

            assertThatThrownBy(() -> positionService.setPosition(entityId, mapId, position))
                    .isInstanceOf(PositionOutOfBoundsException.class);

            verify(positionRepository, never()).save(anyLong(), any());
        }

        @Test
        @DisplayName("should throw PositionOutOfBoundsException when y out of bounds")
        void shouldThrowPositionOutOfBoundsExceptionWhenYOutOfBounds() {
            when(mapRepository.findById(mapId)).thenReturn(Optional.of(gridMap));
            Position position = new Position(5, 15, 0);

            assertThatThrownBy(() -> positionService.setPosition(entityId, mapId, position))
                    .isInstanceOf(PositionOutOfBoundsException.class);

            verify(positionRepository, never()).save(anyLong(), any());
        }

        @Test
        @DisplayName("should throw PositionOutOfBoundsException when z out of bounds")
        void shouldThrowPositionOutOfBoundsExceptionWhenZOutOfBounds() {
            when(mapRepository.findById(mapId)).thenReturn(Optional.of(gridMap));
            Position position = new Position(5, 5, 10);

            assertThatThrownBy(() -> positionService.setPosition(entityId, mapId, position))
                    .isInstanceOf(PositionOutOfBoundsException.class);

            verify(positionRepository, never()).save(anyLong(), any());
        }

        @Test
        @DisplayName("should throw PositionOutOfBoundsException when position has negative coordinates")
        void shouldThrowPositionOutOfBoundsExceptionWhenPositionHasNegativeCoordinates() {
            when(mapRepository.findById(mapId)).thenReturn(Optional.of(gridMap));
            Position position = new Position(-1, 5, 0);

            assertThatThrownBy(() -> positionService.setPosition(entityId, mapId, position))
                    .isInstanceOf(PositionOutOfBoundsException.class);

            verify(positionRepository, never()).save(anyLong(), any());
        }

        @Test
        @DisplayName("should allow position at origin")
        void shouldAllowPositionAtOrigin() {
            when(mapRepository.findById(mapId)).thenReturn(Optional.of(gridMap));
            Position position = new Position(0, 0, 0);

            positionService.setPosition(entityId, mapId, position);

            verify(positionRepository).save(entityId, position);
        }

        @Test
        @DisplayName("should allow position at max bounds")
        void shouldAllowPositionAtMaxBounds() {
            when(mapRepository.findById(mapId)).thenReturn(Optional.of(gridMap));
            Position position = new Position(9, 9, 4);

            positionService.setPosition(entityId, mapId, position);

            verify(positionRepository).save(entityId, position);
        }
    }

    @Nested
    @DisplayName("getPosition")
    class GetPosition {

        @Test
        @DisplayName("should return position when entity has one")
        void shouldReturnPositionWhenEntityHasOne() {
            long entityId = 42L;
            Position position = new Position(5, 5, 2);
            when(positionRepository.findByEntityId(entityId)).thenReturn(Optional.of(position));

            Optional<Position> result = positionService.getPosition(entityId);

            assertThat(result).contains(position);
        }

        @Test
        @DisplayName("should return empty when entity has no position")
        void shouldReturnEmptyWhenEntityHasNoPosition() {
            long entityId = 42L;
            when(positionRepository.findByEntityId(entityId)).thenReturn(Optional.empty());

            Optional<Position> result = positionService.getPosition(entityId);

            assertThat(result).isEmpty();
        }
    }
}
