package ca.samanthaireland.lightning.engine.ext.modules.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Entity} domain entity.
 */
@DisplayName("Entity")
class EntityTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create entity with all fields")
        void shouldCreateEntityWithAllFields() {
            Entity entity = new Entity(1L, 100L, 42L);

            assertThat(entity.id()).isEqualTo(1L);
            assertThat(entity.entityType()).isEqualTo(100L);
            assertThat(entity.playerId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("should handle zero values")
        void shouldHandleZeroValues() {
            Entity entity = new Entity(0L, 0L, 0L);

            assertThat(entity.id()).isEqualTo(0L);
            assertThat(entity.entityType()).isEqualTo(0L);
            assertThat(entity.playerId()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Entity.create factory method")
    class CreateFactory {

        @Test
        @DisplayName("should create entity with id 0")
        void shouldCreateEntityWithIdZero() {
            Entity entity = Entity.create(100L, 42L);

            assertThat(entity.id()).isEqualTo(0L);
            assertThat(entity.entityType()).isEqualTo(100L);
            assertThat(entity.playerId()).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            Entity entity1 = new Entity(1L, 100L, 42L);
            Entity entity2 = new Entity(1L, 100L, 42L);

            assertThat(entity1).isEqualTo(entity2);
        }

        @Test
        @DisplayName("should not be equal when id differs")
        void shouldNotBeEqualWhenIdDiffers() {
            Entity entity1 = new Entity(1L, 100L, 42L);
            Entity entity2 = new Entity(2L, 100L, 42L);

            assertThat(entity1).isNotEqualTo(entity2);
        }

        @Test
        @DisplayName("should not be equal when entityType differs")
        void shouldNotBeEqualWhenEntityTypeDiffers() {
            Entity entity1 = new Entity(1L, 100L, 42L);
            Entity entity2 = new Entity(1L, 200L, 42L);

            assertThat(entity1).isNotEqualTo(entity2);
        }

        @Test
        @DisplayName("should not be equal when playerId differs")
        void shouldNotBeEqualWhenPlayerIdDiffers() {
            Entity entity1 = new Entity(1L, 100L, 42L);
            Entity entity2 = new Entity(1L, 100L, 99L);

            assertThat(entity1).isNotEqualTo(entity2);
        }
    }
}
