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

import ca.samanthaireland.lightning.engine.internal.core.match.InMemoryMatchRepository;
import ca.samanthaireland.lightning.engine.core.match.Match;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link InMemoryMatchRepository}.
 *
 * <p>Tests verify Repository pattern compliance with pure CRUD operations.
 */
@DisplayName("InMemoryMatchRepository")
class InMemoryMatchRepositoryTest {

    private InMemoryMatchRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryMatchRepository();
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should store match")
        void shouldStoreMatch() {
            Match match = new Match(1L, List.of(), List.of());

            Match result = repository.save(match);

            assertThat(result).isEqualTo(match);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should overwrite existing match with same id")
        void shouldOverwriteExistingMatch() {
            Match match1 = new Match(1L, List.of(), List.of());
            Match match2 = new Match(1L, List.of("module1"), List.of());

            repository.save(match1);
            Match result = repository.save(match2);

            assertThat(result).isEqualTo(match2);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should reject null match")
        void shouldRejectNullMatch() {
            assertThatThrownBy(() -> repository.save(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("match must not be null");
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        @DisplayName("should remove match")
        void shouldRemoveMatch() {
            Match match = new Match(1L, List.of(), List.of());
            repository.save(match);

            repository.deleteById(1L);

            assertThat(repository.findById(1L)).isEmpty();
            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("should not throw when deleting non-existent match")
        void shouldNotThrowWhenDeletingNonExistent() {
            repository.deleteById(999L);

            assertThat(repository.count()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return match when found")
        void shouldReturnMatchWhenFound() {
            Match match = new Match(1L, List.of(), List.of());
            repository.save(match);

            Optional<Match> result = repository.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(match);
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            Optional<Match> result = repository.findById(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return all matches")
        void shouldReturnAllMatches() {
            repository.save(new Match(1L, List.of(), List.of()));
            repository.save(new Match(2L, List.of(), List.of()));
            repository.save(new Match(3L, List.of(), List.of()));

            List<Match> result = repository.findAll();

            assertThat(result).hasSize(3);
            assertThat(result).extracting(Match::id).containsExactlyInAnyOrder(1L, 2L, 3L);
        }

        @Test
        @DisplayName("should return empty list when no matches")
        void shouldReturnEmptyListWhenNoMatches() {
            List<Match> result = repository.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsById")
    class ExistsById {

        @Test
        @DisplayName("should return true when match exists")
        void shouldReturnTrueWhenMatchExists() {
            repository.save(new Match(1L, List.of(), List.of()));

            assertThat(repository.existsById(1L)).isTrue();
        }

        @Test
        @DisplayName("should return false when match does not exist")
        void shouldReturnFalseWhenMatchDoesNotExist() {
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

            repository.save(new Match(1L, List.of(), List.of()));
            assertThat(repository.count()).isEqualTo(1);

            repository.save(new Match(2L, List.of(), List.of()));
            assertThat(repository.count()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("should remove all matches")
        void shouldRemoveAllMatches() {
            repository.save(new Match(1L, List.of(), List.of()));
            repository.save(new Match(2L, List.of(), List.of()));
            repository.save(new Match(3L, List.of(), List.of()));

            repository.clear();

            assertThat(repository.count()).isEqualTo(0);
        }
    }
}
