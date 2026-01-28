package ca.samanthaireland.lightning.engine.ext.modules.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link Projectile}.
 */
@DisplayName("Projectile")
class ProjectileTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create projectile with normalized direction")
        void shouldCreateProjectileWithNormalizedDirection() {
            Projectile projectile = Projectile.create(
                    1L, 10f, 20f, 3f, 4f, 15f, 25f, 100f, 2f, 1f
            );

            // Direction (3, 4) normalized = (0.6, 0.8)
            assertThat(projectile.directionX()).isCloseTo(0.6f, within(0.001f));
            assertThat(projectile.directionY()).isCloseTo(0.8f, within(0.001f));
        }

        @Test
        @DisplayName("should set ticksAlive to 0")
        void shouldSetTicksAliveToZero() {
            Projectile projectile = Projectile.create(
                    1L, 10f, 20f, 1f, 0f, 15f, 25f, 100f, 2f, 1f
            );

            assertThat(projectile.ticksAlive()).isEqualTo(0f);
        }

        @Test
        @DisplayName("should set hitsRemaining to pierceCount")
        void shouldSetHitsRemainingToPierceCount() {
            Projectile projectile = Projectile.create(
                    1L, 10f, 20f, 1f, 0f, 15f, 25f, 100f, 5f, 1f
            );

            assertThat(projectile.hitsRemaining()).isEqualTo(5f);
        }

        @Test
        @DisplayName("should throw for negative speed")
        void shouldThrowForNegativeSpeed() {
            assertThatThrownBy(() -> Projectile.create(
                    1L, 10f, 20f, 1f, 0f, -5f, 25f, 100f, 0f, 1f
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("speed");
        }

        @Test
        @DisplayName("should throw for negative damage")
        void shouldThrowForNegativeDamage() {
            assertThatThrownBy(() -> Projectile.create(
                    1L, 10f, 20f, 1f, 0f, 15f, -25f, 100f, 0f, 1f
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("damage");
        }
    }

    @Nested
    @DisplayName("hasExpired")
    class HasExpired {

        @Test
        @DisplayName("should return true when ticksAlive reaches lifetime")
        void shouldReturnTrueWhenTicksAliveReachesLifetime() {
            Projectile projectile = new Projectile(
                    1L, 1L, 10f, 20f, 1f, 0f, 15f, 25f, 100f, 100f, 0f, 0f, 1f, false
            );

            assertThat(projectile.hasExpired()).isTrue();
        }

        @Test
        @DisplayName("should return false when ticksAlive is less than lifetime")
        void shouldReturnFalseWhenTicksAliveIsLessThanLifetime() {
            Projectile projectile = new Projectile(
                    1L, 1L, 10f, 20f, 1f, 0f, 15f, 25f, 100f, 50f, 0f, 0f, 1f, false
            );

            assertThat(projectile.hasExpired()).isFalse();
        }

        @Test
        @DisplayName("should return false when lifetime is 0")
        void shouldReturnFalseWhenLifetimeIsZero() {
            Projectile projectile = new Projectile(
                    1L, 1L, 10f, 20f, 1f, 0f, 15f, 25f, 0f, 1000f, 0f, 0f, 1f, false
            );

            assertThat(projectile.hasExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("withMovement")
    class WithMovement {

        @Test
        @DisplayName("should update position based on direction and speed")
        void shouldUpdatePositionBasedOnDirectionAndSpeed() {
            Projectile projectile = new Projectile(
                    1L, 1L, 10f, 20f, 1f, 0f, 5f, 25f, 100f, 0f, 0f, 0f, 1f, false
            );

            Projectile moved = projectile.withMovement();

            assertThat(moved.positionX()).isEqualTo(15f);
            assertThat(moved.positionY()).isEqualTo(20f);
        }

        @Test
        @DisplayName("should preserve other fields")
        void shouldPreserveOtherFields() {
            Projectile projectile = new Projectile(
                    1L, 2L, 10f, 20f, 1f, 0f, 5f, 25f, 100f, 50f, 3f, 2f, 1f, false
            );

            Projectile moved = projectile.withMovement();

            assertThat(moved.id()).isEqualTo(1L);
            assertThat(moved.ownerEntityId()).isEqualTo(2L);
            assertThat(moved.speed()).isEqualTo(5f);
            assertThat(moved.damage()).isEqualTo(25f);
            assertThat(moved.ticksAlive()).isEqualTo(50f);
        }
    }

    @Nested
    @DisplayName("withTick")
    class WithTick {

        @Test
        @DisplayName("should increment ticksAlive by 1")
        void shouldIncrementTicksAliveByOne() {
            Projectile projectile = new Projectile(
                    1L, 1L, 10f, 20f, 1f, 0f, 5f, 25f, 100f, 50f, 0f, 0f, 1f, false
            );

            Projectile ticked = projectile.withTick();

            assertThat(ticked.ticksAlive()).isEqualTo(51f);
        }
    }

    @Nested
    @DisplayName("withPendingDestroy")
    class WithPendingDestroy {

        @Test
        @DisplayName("should set pendingDestroy to true")
        void shouldSetPendingDestroyToTrue() {
            Projectile projectile = new Projectile(
                    1L, 1L, 10f, 20f, 1f, 0f, 5f, 25f, 100f, 50f, 0f, 0f, 1f, false
            );

            Projectile marked = projectile.withPendingDestroy();

            assertThat(marked.pendingDestroy()).isTrue();
        }
    }

    @Nested
    @DisplayName("withId")
    class WithId {

        @Test
        @DisplayName("should set new ID")
        void shouldSetNewId() {
            Projectile projectile = Projectile.create(
                    1L, 10f, 20f, 1f, 0f, 15f, 25f, 100f, 0f, 1f
            );

            Projectile withId = projectile.withId(42L);

            assertThat(withId.id()).isEqualTo(42L);
        }
    }
}
