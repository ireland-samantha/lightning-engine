/*
 * Copyright (c) 2026 Samantha Ireland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package ca.samanthaireland.lightning.engine.internal.core.impl;

import ca.samanthaireland.lightning.engine.internal.core.match.InMemoryPlayerRepository;
import ca.samanthaireland.lightning.engine.core.match.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link InMemoryPlayerRepository}.
 *
 * <p>Tests verify Repository pattern compliance with pure CRUD operations.
 */
@DisplayName("InMemoryPlayerRepository")
class InMemoryPlayerRepositoryTest {

    private InMemoryPlayerRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPlayerRepository();
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should store player")
        void shouldStorePlayer() {
            Player player = new Player(1L);

            Player result = repository.save(player);

            assertThat(result).isEqualTo(player);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should overwrite existing player with same id")
        void shouldOverwriteExistingPlayer() {
            Player player1 = new Player(1L);
            Player player2 = new Player(1L);

            repository.save(player1);
            Player result = repository.save(player2);

            assertThat(result).isEqualTo(player2);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should reject null player")
        void shouldRejectNullPlayer() {
            assertThatThrownBy(() -> repository.save(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("player must not be null");
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        @DisplayName("should remove player")
        void shouldRemovePlayer() {
            Player player = new Player(1L);
            repository.save(player);

            repository.deleteById(1L);

            assertThat(repository.findById(1L)).isEmpty();
            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("should not throw when deleting non-existent player")
        void shouldNotThrowWhenDeletingNonExistent() {
            repository.deleteById(999L);

            assertThat(repository.count()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return player when found")
        void shouldReturnPlayerWhenFound() {
            Player player = new Player(1L);
            repository.save(player);

            Optional<Player> result = repository.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(player);
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            Optional<Player> result = repository.findById(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return all players")
        void shouldReturnAllPlayers() {
            repository.save(new Player(1L));
            repository.save(new Player(2L));
            repository.save(new Player(3L));

            List<Player> result = repository.findAll();

            assertThat(result).hasSize(3);
            assertThat(result).extracting(Player::id).containsExactlyInAnyOrder(1L, 2L, 3L);
        }

        @Test
        @DisplayName("should return empty list when no players")
        void shouldReturnEmptyListWhenNoPlayers() {
            List<Player> result = repository.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsById")
    class ExistsById {

        @Test
        @DisplayName("should return true when player exists")
        void shouldReturnTrueWhenPlayerExists() {
            repository.save(new Player(1L));

            assertThat(repository.existsById(1L)).isTrue();
        }

        @Test
        @DisplayName("should return false when player does not exist")
        void shouldReturnFalseWhenPlayerDoesNotExist() {
            assertThat(repository.existsById(999L)).isFalse();
        }
    }

    @Nested
    @DisplayName("count")
    class Count {

        @Test
        @DisplayName("should return correct count")
        void shouldReturnCorrectCount() {
            assertThat(repository.count()).isEqualTo(0);

            repository.save(new Player(1L));
            assertThat(repository.count()).isEqualTo(1);

            repository.save(new Player(2L));
            assertThat(repository.count()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("should remove all players")
        void shouldRemoveAllPlayers() {
            repository.save(new Player(1L));
            repository.save(new Player(2L));
            repository.save(new Player(3L));

            repository.clear();

            assertThat(repository.count()).isEqualTo(0);
        }
    }
}
