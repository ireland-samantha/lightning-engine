package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.ext.modules.domain.Projectile;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.ProjectileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ProjectileService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectileService")
class ProjectileServiceTest {

    @Mock
    private ProjectileRepository projectileRepository;

    private ProjectileService projectileService;

    @BeforeEach
    void setUp() {
        projectileService = new ProjectileService(projectileRepository);
    }

    @Nested
    @DisplayName("spawnProjectile")
    class SpawnProjectile {

        @Test
        @DisplayName("should save projectile with correct parameters")
        void shouldSaveProjectileWithCorrectParameters() {
            long matchId = 1L;
            Projectile savedProjectile = new Projectile(
                    100L, 5L, 10f, 20f, 0.6f, 0.8f, 15f, 25f, 100f, 0f, 2f, 2f, 1f, false
            );
            when(projectileRepository.save(eq(matchId), any(Projectile.class))).thenReturn(savedProjectile);

            Projectile result = projectileService.spawnProjectile(
                    matchId, 5L, 10f, 20f, 3f, 4f, 15f, 25f, 100f, 2f, 1f
            );

            assertThat(result).isEqualTo(savedProjectile);
            verify(projectileRepository).save(eq(matchId), any(Projectile.class));
        }

        @Test
        @DisplayName("should normalize direction vector")
        void shouldNormalizeDirectionVector() {
            when(projectileRepository.save(anyLong(), any(Projectile.class)))
                    .thenAnswer(inv -> ((Projectile) inv.getArgument(1)).withId(100L));

            projectileService.spawnProjectile(
                    1L, 5L, 10f, 20f, 3f, 4f, 15f, 25f, 100f, 0f, 1f
            );

            ArgumentCaptor<Projectile> captor = ArgumentCaptor.forClass(Projectile.class);
            verify(projectileRepository).save(anyLong(), captor.capture());

            Projectile captured = captor.getValue();
            assertThat(captured.directionX()).isCloseTo(0.6f, within(0.001f));
            assertThat(captured.directionY()).isCloseTo(0.8f, within(0.001f));
        }
    }

    @Nested
    @DisplayName("destroyProjectile")
    class DestroyProjectile {

        @Test
        @DisplayName("should mark projectile for destruction")
        void shouldMarkProjectileForDestruction() {
            long projectileId = 100L;

            projectileService.destroyProjectile(projectileId);

            verify(projectileRepository).markPendingDestroy(projectileId);
        }

        @Test
        @DisplayName("should not mark when entityId is 0")
        void shouldNotMarkWhenEntityIdIsZero() {
            projectileService.destroyProjectile(0L);

            verify(projectileRepository, never()).markPendingDestroy(anyLong());
        }
    }

    @Nested
    @DisplayName("processMovement")
    class ProcessMovement {

        @Test
        @DisplayName("should update position for active projectiles")
        void shouldUpdatePositionForActiveProjectiles() {
            long entityId = 100L;
            Projectile projectile = new Projectile(
                    entityId, 1L, 10f, 20f, 1f, 0f, 5f, 25f, 100f, 0f, 0f, 0f, 1f, false
            );
            when(projectileRepository.findAllIds()).thenReturn(Set.of(entityId));
            when(projectileRepository.findById(entityId)).thenReturn(Optional.of(projectile));

            projectileService.processMovement();

            ArgumentCaptor<Projectile> captor = ArgumentCaptor.forClass(Projectile.class);
            verify(projectileRepository).update(captor.capture());

            Projectile updated = captor.getValue();
            assertThat(updated.positionX()).isEqualTo(15f);
            assertThat(updated.positionY()).isEqualTo(20f);
        }

        @Test
        @DisplayName("should skip projectiles pending destruction")
        void shouldSkipProjectilesPendingDestruction() {
            long entityId = 100L;
            Projectile projectile = new Projectile(
                    entityId, 1L, 10f, 20f, 1f, 0f, 5f, 25f, 100f, 0f, 0f, 0f, 1f, true
            );
            when(projectileRepository.findAllIds()).thenReturn(Set.of(entityId));
            when(projectileRepository.findById(entityId)).thenReturn(Optional.of(projectile));

            projectileService.processMovement();

            verify(projectileRepository, never()).update(any());
        }

        @Test
        @DisplayName("should skip projectiles with zero speed")
        void shouldSkipProjectilesWithZeroSpeed() {
            long entityId = 100L;
            Projectile projectile = new Projectile(
                    entityId, 1L, 10f, 20f, 1f, 0f, 0f, 25f, 100f, 0f, 0f, 0f, 1f, false
            );
            when(projectileRepository.findAllIds()).thenReturn(Set.of(entityId));
            when(projectileRepository.findById(entityId)).thenReturn(Optional.of(projectile));

            projectileService.processMovement();

            verify(projectileRepository, never()).update(any());
        }
    }

    @Nested
    @DisplayName("processLifetime")
    class ProcessLifetime {

        @Test
        @DisplayName("should increment ticksAlive")
        void shouldIncrementTicksAlive() {
            long entityId = 100L;
            Projectile projectile = new Projectile(
                    entityId, 1L, 10f, 20f, 1f, 0f, 5f, 25f, 100f, 50f, 0f, 0f, 1f, false
            );
            when(projectileRepository.findAllIds()).thenReturn(Set.of(entityId));
            when(projectileRepository.findById(entityId)).thenReturn(Optional.of(projectile));

            projectileService.processLifetime();

            ArgumentCaptor<Projectile> captor = ArgumentCaptor.forClass(Projectile.class);
            verify(projectileRepository).update(captor.capture());

            assertThat(captor.getValue().ticksAlive()).isEqualTo(51f);
        }

        @Test
        @DisplayName("should queue expired projectiles for destruction")
        void shouldQueueExpiredProjectilesForDestruction() {
            long entityId = 100L;
            Projectile projectile = new Projectile(
                    entityId, 1L, 10f, 20f, 1f, 0f, 5f, 25f, 100f, 99f, 0f, 0f, 1f, false
            );
            when(projectileRepository.findAllIds()).thenReturn(Set.of(entityId));
            when(projectileRepository.findById(entityId)).thenReturn(Optional.of(projectile));

            projectileService.processLifetime();

            assertThat(projectileService.getDestroyQueueSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("should not queue projectiles with no lifetime limit")
        void shouldNotQueueProjectilesWithNoLifetimeLimit() {
            long entityId = 100L;
            Projectile projectile = new Projectile(
                    entityId, 1L, 10f, 20f, 1f, 0f, 5f, 25f, 0f, 1000f, 0f, 0f, 1f, false
            );
            when(projectileRepository.findAllIds()).thenReturn(Set.of(entityId));
            when(projectileRepository.findById(entityId)).thenReturn(Optional.of(projectile));

            projectileService.processLifetime();

            assertThat(projectileService.getDestroyQueueSize()).isEqualTo(0);
        }

        @Test
        @DisplayName("should queue already pending projectiles")
        void shouldQueueAlreadyPendingProjectiles() {
            long entityId = 100L;
            Projectile projectile = new Projectile(
                    entityId, 1L, 10f, 20f, 1f, 0f, 5f, 25f, 100f, 50f, 0f, 0f, 1f, true
            );
            when(projectileRepository.findAllIds()).thenReturn(Set.of(entityId));
            when(projectileRepository.findById(entityId)).thenReturn(Optional.of(projectile));

            projectileService.processLifetime();

            assertThat(projectileService.getDestroyQueueSize()).isEqualTo(1);
            verify(projectileRepository, never()).update(any());
        }
    }

    @Nested
    @DisplayName("processCleanup")
    class ProcessCleanup {

        @Test
        @DisplayName("should delete queued projectiles")
        void shouldDeleteQueuedProjectiles() {
            long entityId = 100L;
            Projectile projectile = new Projectile(
                    entityId, 1L, 10f, 20f, 1f, 0f, 5f, 25f, 100f, 50f, 0f, 0f, 1f, true
            );
            when(projectileRepository.findAllIds()).thenReturn(Set.of(entityId));
            when(projectileRepository.findById(entityId)).thenReturn(Optional.of(projectile));

            // Queue for destruction via processLifetime
            projectileService.processLifetime();
            assertThat(projectileService.getDestroyQueueSize()).isEqualTo(1);

            // Clean up
            projectileService.processCleanup();

            verify(projectileRepository).delete(entityId);
            assertThat(projectileService.getDestroyQueueSize()).isEqualTo(0);
        }

        @Test
        @DisplayName("should do nothing when queue is empty")
        void shouldDoNothingWhenQueueIsEmpty() {
            projectileService.processCleanup();

            verify(projectileRepository, never()).delete(anyLong());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return projectile when found")
        void shouldReturnProjectileWhenFound() {
            long projectileId = 100L;
            Projectile projectile = new Projectile(
                    projectileId, 1L, 10f, 20f, 1f, 0f, 5f, 25f, 100f, 50f, 0f, 0f, 1f, false
            );
            when(projectileRepository.findById(projectileId)).thenReturn(Optional.of(projectile));

            Optional<Projectile> result = projectileService.findById(projectileId);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(projectile);
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            long projectileId = 100L;
            when(projectileRepository.findById(projectileId)).thenReturn(Optional.empty());

            Optional<Projectile> result = projectileService.findById(projectileId);

            assertThat(result).isEmpty();
        }
    }
}
