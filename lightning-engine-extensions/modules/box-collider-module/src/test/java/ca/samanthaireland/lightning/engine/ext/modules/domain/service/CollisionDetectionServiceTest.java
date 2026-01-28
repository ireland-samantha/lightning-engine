package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.ext.modules.domain.AABB;
import ca.samanthaireland.lightning.engine.ext.modules.domain.BoxCollider;
import ca.samanthaireland.lightning.engine.ext.modules.domain.CollisionInfo;
import ca.samanthaireland.lightning.engine.ext.modules.domain.CollisionPair;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.BoxColliderRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CollisionDetectionService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CollisionDetectionService")
class CollisionDetectionServiceTest {

    @Mock
    private BoxColliderRepository repository;

    private CollisionDetectionService service;

    @BeforeEach
    void setUp() {
        service = new CollisionDetectionService(repository);
    }

    @Nested
    @DisplayName("detectCollisions")
    class DetectCollisions {

        @Test
        @DisplayName("should return empty list when no colliders exist")
        void shouldReturnEmptyListWhenNoCollidersExist() {
            when(repository.findAllColliderEntities()).thenReturn(Set.of());

            List<CollisionPair> result = service.detectCollisions(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should detect collision between overlapping entities")
        void shouldDetectCollisionBetweenOverlappingEntities() {
            long entityA = 1L;
            long entityB = 2L;

            when(repository.findAllColliderEntities()).thenReturn(Set.of(entityA, entityB));

            // Entity A at position (0, 0) with 10x10 box
            BoxCollider colliderA = new BoxCollider(entityA, 10f, 10f, 1f, 0, 0, 0, 1, -1, false);
            // Entity B at position (5, 0) with 10x10 box - overlapping with A
            BoxCollider colliderB = new BoxCollider(entityB, 10f, 10f, 1f, 0, 0, 0, 1, -1, false);

            when(repository.findByEntityId(entityA)).thenReturn(Optional.of(colliderA));
            when(repository.findByEntityId(entityB)).thenReturn(Optional.of(colliderB));

            when(repository.getPositionX(entityA)).thenReturn(0f);
            when(repository.getPositionY(entityA)).thenReturn(0f);
            when(repository.getPositionX(entityB)).thenReturn(5f);
            when(repository.getPositionY(entityB)).thenReturn(0f);

            List<CollisionPair> result = service.detectCollisions(1L);

            assertThat(result).hasSize(1);
            verify(repository).updateCollisionState(eq(entityA), eq(true), anyInt(), eq(entityB),
                    anyFloat(), anyFloat(), anyFloat());
            verify(repository).updateCollisionState(eq(entityB), eq(true), anyInt(), eq(entityA),
                    anyFloat(), anyFloat(), anyFloat());
        }

        @Test
        @DisplayName("should not detect collision between non-overlapping entities")
        void shouldNotDetectCollisionBetweenNonOverlappingEntities() {
            long entityA = 1L;
            long entityB = 2L;

            when(repository.findAllColliderEntities()).thenReturn(Set.of(entityA, entityB));

            // Entity A at position (0, 0) with 10x10 box
            BoxCollider colliderA = new BoxCollider(entityA, 10f, 10f, 1f, 0, 0, 0, 1, -1, false);
            // Entity B at position (100, 0) - far away from A
            BoxCollider colliderB = new BoxCollider(entityB, 10f, 10f, 1f, 0, 0, 0, 1, -1, false);

            when(repository.findByEntityId(entityA)).thenReturn(Optional.of(colliderA));
            when(repository.findByEntityId(entityB)).thenReturn(Optional.of(colliderB));

            when(repository.getPositionX(entityA)).thenReturn(0f);
            when(repository.getPositionY(entityA)).thenReturn(0f);
            when(repository.getPositionX(entityB)).thenReturn(100f);
            when(repository.getPositionY(entityB)).thenReturn(0f);

            List<CollisionPair> result = service.detectCollisions(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should respect layer/mask filtering")
        void shouldRespectLayerMaskFiltering() {
            long entityA = 1L;
            long entityB = 2L;

            when(repository.findAllColliderEntities()).thenReturn(Set.of(entityA, entityB));

            // Entity A on layer 1, mask 1 (can only hit layer 1)
            BoxCollider colliderA = new BoxCollider(entityA, 10f, 10f, 1f, 0, 0, 0, 1, 1, false);
            // Entity B on layer 2, mask 2 (can only hit layer 2)
            BoxCollider colliderB = new BoxCollider(entityB, 10f, 10f, 1f, 0, 0, 0, 2, 2, false);

            when(repository.findByEntityId(entityA)).thenReturn(Optional.of(colliderA));
            when(repository.findByEntityId(entityB)).thenReturn(Optional.of(colliderB));

            // Overlapping positions (lenient because layer filtering returns early)
            lenient().when(repository.getPositionX(entityA)).thenReturn(0f);
            lenient().when(repository.getPositionY(entityA)).thenReturn(0f);
            lenient().when(repository.getPositionX(entityB)).thenReturn(0f);
            lenient().when(repository.getPositionY(entityB)).thenReturn(0f);

            List<CollisionPair> result = service.detectCollisions(1L);

            // No collision because layers don't match masks
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should reset collision state before detection")
        void shouldResetCollisionStateBeforeDetection() {
            long entityA = 1L;
            when(repository.findAllColliderEntities()).thenReturn(Set.of(entityA));

            service.detectCollisions(1L);

            verify(repository).resetCollisionState(entityA);
        }
    }

    @Nested
    @DisplayName("canCollide")
    class CanCollide {

        @Test
        @DisplayName("should return true when layers and masks match")
        void shouldReturnTrueWhenLayersAndMasksMatch() {
            long entityA = 1L;
            long entityB = 2L;

            BoxCollider colliderA = new BoxCollider(entityA, 10f, 10f, 1f, 0, 0, 0, 1, -1, false);
            BoxCollider colliderB = new BoxCollider(entityB, 10f, 10f, 1f, 0, 0, 0, 1, -1, false);

            when(repository.findByEntityId(entityA)).thenReturn(Optional.of(colliderA));
            when(repository.findByEntityId(entityB)).thenReturn(Optional.of(colliderB));

            boolean result = service.canCollide(entityA, entityB);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when entity not found")
        void shouldReturnFalseWhenEntityNotFound() {
            long entityA = 1L;
            long entityB = 2L;

            when(repository.findByEntityId(entityA)).thenReturn(Optional.empty());

            boolean result = service.canCollide(entityA, entityB);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getAABB")
    class GetAABB {

        @Test
        @DisplayName("should calculate AABB from collider and position")
        void shouldCalculateAABBFromColliderAndPosition() {
            long entityId = 1L;
            BoxCollider collider = new BoxCollider(entityId, 10f, 20f, 1f, 2f, 3f, 0, 1, -1, false);

            when(repository.findByEntityId(entityId)).thenReturn(Optional.of(collider));
            when(repository.getPositionX(entityId)).thenReturn(100f);
            when(repository.getPositionY(entityId)).thenReturn(200f);

            AABB result = service.getAABB(entityId);

            // Center: (100 + 2, 200 + 3) = (102, 203)
            // Half extents: (5, 10)
            // Min: (97, 193), Max: (107, 213)
            assertThat(result.minX()).isEqualTo(97f);
            assertThat(result.minY()).isEqualTo(193f);
            assertThat(result.maxX()).isEqualTo(107f);
            assertThat(result.maxY()).isEqualTo(213f);
        }

        @Test
        @DisplayName("should return zero AABB when collider not found")
        void shouldReturnZeroAABBWhenColliderNotFound() {
            long entityId = 1L;
            when(repository.findByEntityId(entityId)).thenReturn(Optional.empty());

            AABB result = service.getAABB(entityId);

            assertThat(result.minX()).isEqualTo(0f);
            assertThat(result.minY()).isEqualTo(0f);
            assertThat(result.maxX()).isEqualTo(0f);
            assertThat(result.maxY()).isEqualTo(0f);
        }
    }

    @Nested
    @DisplayName("calculateCollisionInfo")
    class CalculateCollisionInfo {

        @Test
        @DisplayName("should calculate correct normal for horizontal collision")
        void shouldCalculateCorrectNormalForHorizontalCollision() {
            // A is to the left of B
            AABB a = new AABB(0, 0, 10, 10);
            AABB b = new AABB(8, 0, 18, 10);

            CollisionInfo info = service.calculateCollisionInfo(a, b);

            // Overlap on X is 2, overlap on Y is 10
            // Minimum is X, so normal points in X direction
            assertThat(info.normalX()).isEqualTo(1f);
            assertThat(info.normalY()).isEqualTo(0f);
            assertThat(info.penetrationDepth()).isEqualTo(2f);
        }

        @Test
        @DisplayName("should calculate correct normal for vertical collision")
        void shouldCalculateCorrectNormalForVerticalCollision() {
            // A is below B
            AABB a = new AABB(0, 0, 10, 10);
            AABB b = new AABB(0, 8, 10, 18);

            CollisionInfo info = service.calculateCollisionInfo(a, b);

            // Overlap on X is 10, overlap on Y is 2
            // Minimum is Y, so normal points in Y direction
            assertThat(info.normalX()).isEqualTo(0f);
            assertThat(info.normalY()).isEqualTo(1f);
            assertThat(info.penetrationDepth()).isEqualTo(2f);
        }
    }
}
