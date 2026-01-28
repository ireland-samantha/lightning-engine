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

import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.internal.core.match.InMemoryPlayerService;
import ca.samanthaireland.lightning.engine.core.match.Player;
import ca.samanthaireland.lightning.engine.core.match.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link InMemoryPlayerService}.
 *
 * <p>Tests verify Service pattern compliance with business logic operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InMemoryPlayerService")
class InMemoryPlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    private InMemoryPlayerService service;

    @BeforeEach
    void setUp() {
        service = new InMemoryPlayerService(playerRepository);
    }

    @Test
    @DisplayName("constructor should reject null repository")
    void constructorShouldRejectNullRepository() {
        assertThatThrownBy(() -> new InMemoryPlayerService(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("playerRepository must not be null");
    }

    @Nested
    @DisplayName("createPlayer")
    class CreatePlayer {

        @Test
        @DisplayName("should delegate to repository")
        void shouldDelegateToRepository() {
            Player player = new Player(1L);
            when(playerRepository.save(player)).thenReturn(player);

            Player result = service.createPlayer(player);

            assertThat(result).isEqualTo(player);
            verify(playerRepository).save(player);
        }

        @Test
        @DisplayName("should reject null player")
        void shouldRejectNullPlayer() {
            assertThatThrownBy(() -> service.createPlayer(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("player must not be null");
        }
    }

    @Nested
    @DisplayName("deletePlayer")
    class DeletePlayer {

        @Test
        @DisplayName("should delete existing player")
        void shouldDeleteExistingPlayer() {
            when(playerRepository.existsById(1L)).thenReturn(true);

            service.deletePlayer(1L);

            verify(playerRepository).deleteById(1L);
        }

        @Test
        @DisplayName("should throw when player does not exist")
        void shouldThrowWhenPlayerDoesNotExist() {
            when(playerRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> service.deletePlayer(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Player 999 not found");
        }
    }

    @Nested
    @DisplayName("getPlayer")
    class GetPlayer {

        @Test
        @DisplayName("should return player when found")
        void shouldReturnPlayerWhenFound() {
            Player player = new Player(1L);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

            Optional<Player> result = service.getPlayer(1L);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(player);
            verify(playerRepository).findById(1L);
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            when(playerRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<Player> result = service.getPlayer(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getPlayerOrThrow")
    class GetPlayerOrThrow {

        @Test
        @DisplayName("should return player when found")
        void shouldReturnPlayerWhenFound() {
            Player player = new Player(1L);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

            Player result = service.getPlayerOrThrow(1L);

            assertThat(result).isEqualTo(player);
        }

        @Test
        @DisplayName("should throw when not found")
        void shouldThrowWhenNotFound() {
            when(playerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPlayerOrThrow(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Player 999 not found");
        }
    }

    @Nested
    @DisplayName("getAllPlayers")
    class GetAllPlayers {

        @Test
        @DisplayName("should delegate to repository")
        void shouldDelegateToRepository() {
            List<Player> players = List.of(new Player(1L), new Player(2L));
            when(playerRepository.findAll()).thenReturn(players);

            List<Player> result = service.getAllPlayers();

            assertThat(result).hasSize(2);
            verify(playerRepository).findAll();
        }
    }

    @Nested
    @DisplayName("playerExists")
    class PlayerExists {

        @Test
        @DisplayName("should return true when player exists")
        void shouldReturnTrueWhenPlayerExists() {
            when(playerRepository.existsById(1L)).thenReturn(true);

            assertThat(service.playerExists(1L)).isTrue();
        }

        @Test
        @DisplayName("should return false when player does not exist")
        void shouldReturnFalseWhenPlayerDoesNotExist() {
            when(playerRepository.existsById(999L)).thenReturn(false);

            assertThat(service.playerExists(999L)).isFalse();
        }
    }
}
