package ca.samanthaireland.lightning.engine.ext.modules.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Health}.
 */
@DisplayName("Health")
class HealthTest {

    @Nested
    @DisplayName("constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("should throw for zero maxHP")
        void shouldThrowForZeroMaxHP() {
            assertThatThrownBy(() -> new Health(50f, 0, 0, false, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Max HP must be positive");
        }

        @Test
        @DisplayName("should throw for negative maxHP")
        void shouldThrowForNegativeMaxHP() {
            assertThatThrownBy(() -> new Health(50f, -10f, 0, false, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Max HP must be positive");
        }

        @Test
        @DisplayName("should throw for negative currentHP")
        void shouldThrowForNegativeCurrentHP() {
            assertThatThrownBy(() -> new Health(-10f, 100f, 0, false, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Current HP cannot be negative");
        }

        @Test
        @DisplayName("should allow valid values")
        void shouldAllowValidValues() {
            Health health = new Health(50f, 100f, 5f, false, true);

            assertThat(health.currentHP()).isEqualTo(50f);
            assertThat(health.maxHP()).isEqualTo(100f);
            assertThat(health.damageTaken()).isEqualTo(5f);
            assertThat(health.isDead()).isFalse();
            assertThat(health.invulnerable()).isTrue();
        }
    }

    @Nested
    @DisplayName("create factory methods")
    class CreateFactoryMethods {

        @Test
        @DisplayName("should create with maxHP and currentHP")
        void shouldCreateWithMaxHPAndCurrentHP() {
            Health health = Health.create(100f, 75f);

            assertThat(health.maxHP()).isEqualTo(100f);
            assertThat(health.currentHP()).isEqualTo(75f);
            assertThat(health.damageTaken()).isEqualTo(0);
            assertThat(health.isDead()).isFalse();
            assertThat(health.invulnerable()).isFalse();
        }

        @Test
        @DisplayName("should create with full HP when only maxHP provided")
        void shouldCreateWithFullHPWhenOnlyMaxHPProvided() {
            Health health = Health.create(100f);

            assertThat(health.maxHP()).isEqualTo(100f);
            assertThat(health.currentHP()).isEqualTo(100f);
        }
    }

    @Nested
    @DisplayName("withCurrentHP")
    class WithCurrentHP {

        @Test
        @DisplayName("should update currentHP")
        void shouldUpdateCurrentHP() {
            Health health = Health.create(100f);

            Health updated = health.withCurrentHP(75f);

            assertThat(updated.currentHP()).isEqualTo(75f);
            assertThat(health.currentHP()).isEqualTo(100f); // Original unchanged
        }

        @Test
        @DisplayName("should clamp to maxHP when exceeding")
        void shouldClampToMaxHPWhenExceeding() {
            Health health = Health.create(100f);

            Health updated = health.withCurrentHP(150f);

            assertThat(updated.currentHP()).isEqualTo(100f);
        }

        @Test
        @DisplayName("should clamp to 0 when negative")
        void shouldClampTo0WhenNegative() {
            Health health = Health.create(100f);

            Health updated = health.withCurrentHP(-50f);

            assertThat(updated.currentHP()).isEqualTo(0);
        }

        @Test
        @DisplayName("should set isDead when HP reaches 0")
        void shouldSetIsDeadWhenHPReaches0() {
            Health health = Health.create(100f);

            Health updated = health.withCurrentHP(0);

            assertThat(updated.isDead()).isTrue();
        }
    }

    @Nested
    @DisplayName("withDamage")
    class WithDamage {

        @Test
        @DisplayName("should add damage to queue")
        void shouldAddDamageToQueue() {
            Health health = Health.create(100f);

            Health updated = health.withDamage(25f);

            assertThat(updated.damageTaken()).isEqualTo(25f);
        }

        @Test
        @DisplayName("should accumulate damage")
        void shouldAccumulateDamage() {
            Health health = new Health(100f, 100f, 10f, false, false);

            Health updated = health.withDamage(15f);

            assertThat(updated.damageTaken()).isEqualTo(25f);
        }

        @Test
        @DisplayName("should ignore zero damage")
        void shouldIgnoreZeroDamage() {
            Health health = Health.create(100f);

            Health updated = health.withDamage(0);

            assertThat(updated).isSameAs(health);
        }

        @Test
        @DisplayName("should ignore negative damage")
        void shouldIgnoreNegativeDamage() {
            Health health = Health.create(100f);

            Health updated = health.withDamage(-10f);

            assertThat(updated).isSameAs(health);
        }
    }

    @Nested
    @DisplayName("withHealing")
    class WithHealing {

        @Test
        @DisplayName("should increase currentHP")
        void shouldIncreaseCurrentHP() {
            Health health = new Health(50f, 100f, 0, false, false);

            Health updated = health.withHealing(25f);

            assertThat(updated.currentHP()).isEqualTo(75f);
        }

        @Test
        @DisplayName("should cap at maxHP")
        void shouldCapAtMaxHP() {
            Health health = new Health(90f, 100f, 0, false, false);

            Health updated = health.withHealing(50f);

            assertThat(updated.currentHP()).isEqualTo(100f);
        }

        @Test
        @DisplayName("should not heal dead entities")
        void shouldNotHealDeadEntities() {
            Health health = new Health(0, 100f, 0, true, false);

            Health updated = health.withHealing(50f);

            assertThat(updated).isSameAs(health);
        }

        @Test
        @DisplayName("should ignore zero healing")
        void shouldIgnoreZeroHealing() {
            Health health = Health.create(100f, 50f);

            Health updated = health.withHealing(0);

            assertThat(updated).isSameAs(health);
        }
    }

    @Nested
    @DisplayName("withInvulnerable")
    class WithInvulnerable {

        @Test
        @DisplayName("should set invulnerable true")
        void shouldSetInvulnerableTrue() {
            Health health = Health.create(100f);

            Health updated = health.withInvulnerable(true);

            assertThat(updated.invulnerable()).isTrue();
        }

        @Test
        @DisplayName("should set invulnerable false")
        void shouldSetInvulnerableFalse() {
            Health health = new Health(100f, 100f, 0, false, true);

            Health updated = health.withInvulnerable(false);

            assertThat(updated.invulnerable()).isFalse();
        }
    }

    @Nested
    @DisplayName("processDamage")
    class ProcessDamage {

        @Test
        @DisplayName("should apply damage and reduce HP")
        void shouldApplyDamageAndReduceHP() {
            Health health = new Health(100f, 100f, 30f, false, false);

            Health processed = health.processDamage();

            assertThat(processed.currentHP()).isEqualTo(70f);
            assertThat(processed.damageTaken()).isEqualTo(0);
        }

        @Test
        @DisplayName("should set isDead when HP reaches 0")
        void shouldSetIsDeadWhenHPReaches0() {
            Health health = new Health(50f, 100f, 100f, false, false);

            Health processed = health.processDamage();

            assertThat(processed.currentHP()).isEqualTo(0);
            assertThat(processed.isDead()).isTrue();
        }

        @Test
        @DisplayName("should skip damage when invulnerable")
        void shouldSkipDamageWhenInvulnerable() {
            Health health = new Health(100f, 100f, 50f, false, true);

            Health processed = health.processDamage();

            assertThat(processed.currentHP()).isEqualTo(100f);
            assertThat(processed.damageTaken()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return cleared state when already dead")
        void shouldReturnClearedStateWhenAlreadyDead() {
            Health health = new Health(0, 100f, 10f, true, false);

            Health processed = health.processDamage();

            assertThat(processed.damageTaken()).isEqualTo(0);
            assertThat(processed.isDead()).isTrue();
        }

        @Test
        @DisplayName("should clear damage when no damage taken")
        void shouldClearDamageWhenNoDamageTaken() {
            Health health = new Health(100f, 100f, 0, false, false);

            Health processed = health.processDamage();

            assertThat(processed.currentHP()).isEqualTo(100f);
            assertThat(processed.damageTaken()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("hasHealth")
    class HasHealth {

        @Test
        @DisplayName("should always return true")
        void shouldAlwaysReturnTrue() {
            Health health = Health.create(100f);

            assertThat(health.hasHealth()).isTrue();
        }
    }
}
