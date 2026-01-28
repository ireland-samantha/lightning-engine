package ca.samanthaireland.lightning.engine.ext.modules.domain.service;

import ca.samanthaireland.lightning.engine.ext.modules.domain.Health;
import ca.samanthaireland.lightning.engine.ext.modules.domain.repository.HealthRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link HealthService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HealthService")
class HealthServiceTest {

    @Mock
    private HealthRepository healthRepository;

    private HealthService healthService;

    @BeforeEach
    void setUp() {
        healthService = new HealthService(healthRepository);
    }

    @Nested
    @DisplayName("attachHealth")
    class AttachHealth {

        @Test
        @DisplayName("should save health with specified values")
        void shouldSaveHealthWithSpecifiedValues() {
            long entityId = 42L;

            healthService.attachHealth(entityId, 100f, 75f);

            ArgumentCaptor<Health> healthCaptor = ArgumentCaptor.forClass(Health.class);
            verify(healthRepository).save(eq(entityId), healthCaptor.capture());

            Health saved = healthCaptor.getValue();
            assertThat(saved.maxHP()).isEqualTo(100f);
            assertThat(saved.currentHP()).isEqualTo(75f);
            assertThat(saved.isDead()).isFalse();
            assertThat(saved.invulnerable()).isFalse();
        }

        @Test
        @DisplayName("should throw for invalid maxHP")
        void shouldThrowForInvalidMaxHP() {
            assertThatThrownBy(() -> healthService.attachHealth(42L, 0, 50f))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(healthRepository, never()).save(anyLong(), any());
        }

        @Test
        @DisplayName("should throw for negative currentHP")
        void shouldThrowForNegativeCurrentHP() {
            assertThatThrownBy(() -> healthService.attachHealth(42L, 100f, -10f))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(healthRepository, never()).save(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("damage")
    class Damage {

        private final long entityId = 42L;

        @Test
        @DisplayName("should queue damage when entity has health")
        void shouldQueueDamageWhenEntityHasHealth() {
            Health health = Health.create(100f);
            when(healthRepository.findByEntityId(entityId)).thenReturn(Optional.of(health));

            boolean result = healthService.damage(entityId, 25f);

            assertThat(result).isTrue();
            ArgumentCaptor<Health> healthCaptor = ArgumentCaptor.forClass(Health.class);
            verify(healthRepository).save(eq(entityId), healthCaptor.capture());
            assertThat(healthCaptor.getValue().damageTaken()).isEqualTo(25f);
        }

        @Test
        @DisplayName("should accumulate damage")
        void shouldAccumulateDamage() {
            Health health = new Health(100f, 100f, 10f, false, false);
            when(healthRepository.findByEntityId(entityId)).thenReturn(Optional.of(health));

            healthService.damage(entityId, 15f);

            ArgumentCaptor<Health> healthCaptor = ArgumentCaptor.forClass(Health.class);
            verify(healthRepository).save(eq(entityId), healthCaptor.capture());
            assertThat(healthCaptor.getValue().damageTaken()).isEqualTo(25f);
        }

        @Test
        @DisplayName("should return false when entity has no health")
        void shouldReturnFalseWhenEntityHasNoHealth() {
            when(healthRepository.findByEntityId(entityId)).thenReturn(Optional.empty());

            boolean result = healthService.damage(entityId, 25f);

            assertThat(result).isFalse();
            verify(healthRepository, never()).save(anyLong(), any());
        }

        @Test
        @DisplayName("should return false for zero amount")
        void shouldReturnFalseForZeroAmount() {
            boolean result = healthService.damage(entityId, 0);

            assertThat(result).isFalse();
            verify(healthRepository, never()).findByEntityId(anyLong());
        }

        @Test
        @DisplayName("should return false for negative amount")
        void shouldReturnFalseForNegativeAmount() {
            boolean result = healthService.damage(entityId, -10f);

            assertThat(result).isFalse();
            verify(healthRepository, never()).findByEntityId(anyLong());
        }
    }

    @Nested
    @DisplayName("heal")
    class Heal {

        private final long entityId = 42L;

        @Test
        @DisplayName("should heal when entity has health")
        void shouldHealWhenEntityHasHealth() {
            Health health = new Health(50f, 100f, 0, false, false);
            when(healthRepository.findByEntityId(entityId)).thenReturn(Optional.of(health));

            boolean result = healthService.heal(entityId, 25f);

            assertThat(result).isTrue();
            ArgumentCaptor<Health> healthCaptor = ArgumentCaptor.forClass(Health.class);
            verify(healthRepository).save(eq(entityId), healthCaptor.capture());
            assertThat(healthCaptor.getValue().currentHP()).isEqualTo(75f);
        }

        @Test
        @DisplayName("should cap healing at maxHP")
        void shouldCapHealingAtMaxHP() {
            Health health = new Health(90f, 100f, 0, false, false);
            when(healthRepository.findByEntityId(entityId)).thenReturn(Optional.of(health));

            healthService.heal(entityId, 50f);

            ArgumentCaptor<Health> healthCaptor = ArgumentCaptor.forClass(Health.class);
            verify(healthRepository).save(eq(entityId), healthCaptor.capture());
            assertThat(healthCaptor.getValue().currentHP()).isEqualTo(100f);
        }

        @Test
        @DisplayName("should return false when entity is dead")
        void shouldReturnFalseWhenEntityIsDead() {
            Health health = new Health(0, 100f, 0, true, false);
            when(healthRepository.findByEntityId(entityId)).thenReturn(Optional.of(health));

            boolean result = healthService.heal(entityId, 25f);

            assertThat(result).isFalse();
            verify(healthRepository, never()).save(anyLong(), any());
        }

        @Test
        @DisplayName("should return false when entity has no health")
        void shouldReturnFalseWhenEntityHasNoHealth() {
            when(healthRepository.findByEntityId(entityId)).thenReturn(Optional.empty());

            boolean result = healthService.heal(entityId, 25f);

            assertThat(result).isFalse();
            verify(healthRepository, never()).save(anyLong(), any());
        }

        @Test
        @DisplayName("should return false for zero amount")
        void shouldReturnFalseForZeroAmount() {
            boolean result = healthService.heal(entityId, 0);

            assertThat(result).isFalse();
            verify(healthRepository, never()).findByEntityId(anyLong());
        }
    }

    @Nested
    @DisplayName("setInvulnerable")
    class SetInvulnerable {

        private final long entityId = 42L;

        @Test
        @DisplayName("should set invulnerable true")
        void shouldSetInvulnerableTrue() {
            Health health = Health.create(100f);
            when(healthRepository.findByEntityId(entityId)).thenReturn(Optional.of(health));

            boolean result = healthService.setInvulnerable(entityId, true);

            assertThat(result).isTrue();
            ArgumentCaptor<Health> healthCaptor = ArgumentCaptor.forClass(Health.class);
            verify(healthRepository).save(eq(entityId), healthCaptor.capture());
            assertThat(healthCaptor.getValue().invulnerable()).isTrue();
        }

        @Test
        @DisplayName("should set invulnerable false")
        void shouldSetInvulnerableFalse() {
            Health health = new Health(100f, 100f, 0, false, true);
            when(healthRepository.findByEntityId(entityId)).thenReturn(Optional.of(health));

            boolean result = healthService.setInvulnerable(entityId, false);

            assertThat(result).isTrue();
            ArgumentCaptor<Health> healthCaptor = ArgumentCaptor.forClass(Health.class);
            verify(healthRepository).save(eq(entityId), healthCaptor.capture());
            assertThat(healthCaptor.getValue().invulnerable()).isFalse();
        }

        @Test
        @DisplayName("should return false when entity has no health")
        void shouldReturnFalseWhenEntityHasNoHealth() {
            when(healthRepository.findByEntityId(entityId)).thenReturn(Optional.empty());

            boolean result = healthService.setInvulnerable(entityId, true);

            assertThat(result).isFalse();
            verify(healthRepository, never()).save(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("processDamage")
    class ProcessDamage {

        @Test
        @DisplayName("should apply damage and reduce HP")
        void shouldApplyDamageAndReduceHP() {
            long entityId = 42L;
            Health health = new Health(100f, 100f, 30f, false, false);
            when(healthRepository.findAllEntityIds()).thenReturn(Set.of(entityId));
            when(healthRepository.findByEntityId(entityId)).thenReturn(Optional.of(health));

            healthService.processDamage();

            ArgumentCaptor<Health> healthCaptor = ArgumentCaptor.forClass(Health.class);
            verify(healthRepository).save(eq(entityId), healthCaptor.capture());

            Health processed = healthCaptor.getValue();
            assertThat(processed.currentHP()).isEqualTo(70f);
            assertThat(processed.damageTaken()).isEqualTo(0);
            assertThat(processed.isDead()).isFalse();
        }

        @Test
        @DisplayName("should set dead when HP reaches zero")
        void shouldSetDeadWhenHPReachesZero() {
            long entityId = 42L;
            Health health = new Health(50f, 100f, 75f, false, false);
            when(healthRepository.findAllEntityIds()).thenReturn(Set.of(entityId));
            when(healthRepository.findByEntityId(entityId)).thenReturn(Optional.of(health));

            healthService.processDamage();

            ArgumentCaptor<Health> healthCaptor = ArgumentCaptor.forClass(Health.class);
            verify(healthRepository).save(eq(entityId), healthCaptor.capture());

            Health processed = healthCaptor.getValue();
            assertThat(processed.currentHP()).isEqualTo(0);
            assertThat(processed.isDead()).isTrue();
        }

        @Test
        @DisplayName("should skip damage when invulnerable")
        void shouldSkipDamageWhenInvulnerable() {
            long entityId = 42L;
            Health health = new Health(100f, 100f, 50f, false, true);
            when(healthRepository.findAllEntityIds()).thenReturn(Set.of(entityId));
            when(healthRepository.findByEntityId(entityId)).thenReturn(Optional.of(health));

            healthService.processDamage();

            ArgumentCaptor<Health> healthCaptor = ArgumentCaptor.forClass(Health.class);
            verify(healthRepository).save(eq(entityId), healthCaptor.capture());

            Health processed = healthCaptor.getValue();
            assertThat(processed.currentHP()).isEqualTo(100f);
            assertThat(processed.damageTaken()).isEqualTo(0);
        }

        @Test
        @DisplayName("should skip already dead entities")
        void shouldSkipAlreadyDeadEntities() {
            long entityId = 42L;
            Health health = new Health(0, 100f, 0, true, false);
            when(healthRepository.findAllEntityIds()).thenReturn(Set.of(entityId));
            when(healthRepository.findByEntityId(entityId)).thenReturn(Optional.of(health));

            healthService.processDamage();

            verify(healthRepository, never()).save(anyLong(), any());
        }

        @Test
        @DisplayName("should process multiple entities")
        void shouldProcessMultipleEntities() {
            long entity1 = 1L;
            long entity2 = 2L;
            Health health1 = new Health(100f, 100f, 20f, false, false);
            Health health2 = new Health(50f, 100f, 10f, false, false);

            when(healthRepository.findAllEntityIds()).thenReturn(Set.of(entity1, entity2));
            when(healthRepository.findByEntityId(entity1)).thenReturn(Optional.of(health1));
            when(healthRepository.findByEntityId(entity2)).thenReturn(Optional.of(health2));

            healthService.processDamage();

            verify(healthRepository, times(2)).save(anyLong(), any());
        }

        @Test
        @DisplayName("should clear damage even when no damage was taken")
        void shouldClearDamageEvenWhenNoDamageWasTaken() {
            long entityId = 42L;
            Health health = new Health(100f, 100f, 0, false, false);
            when(healthRepository.findAllEntityIds()).thenReturn(Set.of(entityId));
            when(healthRepository.findByEntityId(entityId)).thenReturn(Optional.of(health));

            healthService.processDamage();

            ArgumentCaptor<Health> healthCaptor = ArgumentCaptor.forClass(Health.class);
            verify(healthRepository).save(eq(entityId), healthCaptor.capture());
            assertThat(healthCaptor.getValue().damageTaken()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getHealth")
    class GetHealth {

        @Test
        @DisplayName("should return health when entity has one")
        void shouldReturnHealthWhenEntityHasOne() {
            long entityId = 42L;
            Health health = Health.create(100f);
            when(healthRepository.findByEntityId(entityId)).thenReturn(Optional.of(health));

            Optional<Health> result = healthService.getHealth(entityId);

            assertThat(result).contains(health);
        }

        @Test
        @DisplayName("should return empty when entity has no health")
        void shouldReturnEmptyWhenEntityHasNoHealth() {
            long entityId = 42L;
            when(healthRepository.findByEntityId(entityId)).thenReturn(Optional.empty());

            Optional<Health> result = healthService.getHealth(entityId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasHealth")
    class HasHealth {

        @Test
        @DisplayName("should return true when entity has health")
        void shouldReturnTrueWhenEntityHasHealth() {
            long entityId = 42L;
            when(healthRepository.hasHealth(entityId)).thenReturn(true);

            boolean result = healthService.hasHealth(entityId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when entity has no health")
        void shouldReturnFalseWhenEntityHasNoHealth() {
            long entityId = 42L;
            when(healthRepository.hasHealth(entityId)).thenReturn(false);

            boolean result = healthService.hasHealth(entityId);

            assertThat(result).isFalse();
        }
    }
}
