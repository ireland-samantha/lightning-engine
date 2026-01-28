package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.ext.modules.domain.MovementState;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Position;
import ca.samanthaireland.lightning.engine.ext.modules.domain.Velocity;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.MovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MovementService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MovementService")
class MovementServiceTest {

    @Mock
    private MovementRepository movementRepository;

    private MovementService movementService;

    @BeforeEach
    void setUp() {
        movementService = new MovementService(movementRepository);
    }

    @Nested
    @DisplayName("attachMovement")
    class AttachMovement {

        @Test
        @DisplayName("should save movement with position and velocity")
        void shouldSaveMovementWithPositionAndVelocity() {
            long entityId = 42L;
            Position position = Position.of(10, 20, 30);
            Velocity velocity = Velocity.of(1, 2, 3);

            movementService.attachMovement(entityId, position, velocity);

            verify(movementRepository).save(entityId, position, velocity);
        }

        @Test
        @DisplayName("should save movement with zero velocity")
        void shouldSaveMovementWithZeroVelocity() {
            long entityId = 42L;
            Position position = Position.of(100, 200, 300);
            Velocity velocity = Velocity.zero();

            movementService.attachMovement(entityId, position, velocity);

            verify(movementRepository).save(entityId, position, velocity);
        }
    }

    @Nested
    @DisplayName("getMovementState")
    class GetMovementState {

        @Test
        @DisplayName("should return movement state when entity exists")
        void shouldReturnMovementStateWhenEntityExists() {
            long entityId = 42L;
            MovementState state = MovementState.of(
                    entityId,
                    Position.of(10, 20, 30),
                    Velocity.of(1, 2, 3)
            );
            when(movementRepository.findById(entityId)).thenReturn(Optional.of(state));

            Optional<MovementState> result = movementService.getMovementState(entityId);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(state);
        }

        @Test
        @DisplayName("should return empty when entity does not exist")
        void shouldReturnEmptyWhenEntityDoesNotExist() {
            long entityId = 42L;
            when(movementRepository.findById(entityId)).thenReturn(Optional.empty());

            Optional<MovementState> result = movementService.getMovementState(entityId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("queueForDeletion")
    class QueueForDeletion {

        @Test
        @DisplayName("should add entity to pending deletions")
        void shouldAddEntityToPendingDeletions() {
            long entityId = 42L;

            movementService.queueForDeletion(entityId);

            assertThat(movementService.getPendingDeletions()).containsExactly(entityId);
        }

        @Test
        @DisplayName("should queue multiple entities")
        void shouldQueueMultipleEntities() {
            movementService.queueForDeletion(1L);
            movementService.queueForDeletion(2L);
            movementService.queueForDeletion(3L);

            assertThat(movementService.getPendingDeletions()).containsExactly(1L, 2L, 3L);
        }
    }

    @Nested
    @DisplayName("applyVelocities")
    class ApplyVelocities {

        @Test
        @DisplayName("should update position for all moveable entities")
        void shouldUpdatePositionForAllMoveableEntities() {
            long entityId1 = 1L;
            long entityId2 = 2L;

            MovementState state1 = MovementState.of(
                    entityId1,
                    Position.of(0, 0, 0),
                    Velocity.of(10, 20, 30)
            );
            MovementState state2 = MovementState.of(
                    entityId2,
                    Position.of(100, 100, 100),
                    Velocity.of(-5, -10, -15)
            );

            when(movementRepository.findAllMoveable()).thenReturn(Set.of(entityId1, entityId2));
            when(movementRepository.findById(entityId1)).thenReturn(Optional.of(state1));
            when(movementRepository.findById(entityId2)).thenReturn(Optional.of(state2));

            movementService.applyVelocities();

            verify(movementRepository).updatePosition(entityId1, Position.of(10, 20, 30));
            verify(movementRepository).updatePosition(entityId2, Position.of(95, 90, 85));
        }

        @Test
        @DisplayName("should do nothing when no moveable entities")
        void shouldDoNothingWhenNoMoveableEntities() {
            when(movementRepository.findAllMoveable()).thenReturn(Set.of());

            movementService.applyVelocities();

            verify(movementRepository, never()).updatePosition(anyLong(), any());
        }

        @Test
        @DisplayName("should skip entity if not found")
        void shouldSkipEntityIfNotFound() {
            long entityId = 42L;
            when(movementRepository.findAllMoveable()).thenReturn(Set.of(entityId));
            when(movementRepository.findById(entityId)).thenReturn(Optional.empty());

            movementService.applyVelocities();

            verify(movementRepository, never()).updatePosition(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("processDeletions")
    class ProcessDeletions {

        @Test
        @DisplayName("should delete all queued entities")
        void shouldDeleteAllQueuedEntities() {
            movementService.queueForDeletion(1L);
            movementService.queueForDeletion(2L);
            movementService.queueForDeletion(3L);

            movementService.processDeletions();

            verify(movementRepository).delete(1L);
            verify(movementRepository).delete(2L);
            verify(movementRepository).delete(3L);
        }

        @Test
        @DisplayName("should clear pending deletions after processing")
        void shouldClearPendingDeletionsAfterProcessing() {
            movementService.queueForDeletion(1L);
            movementService.queueForDeletion(2L);

            movementService.processDeletions();

            assertThat(movementService.getPendingDeletions()).isEmpty();
        }

        @Test
        @DisplayName("should do nothing when no pending deletions")
        void shouldDoNothingWhenNoPendingDeletions() {
            movementService.processDeletions();

            verify(movementRepository, never()).delete(anyLong());
        }
    }
}
