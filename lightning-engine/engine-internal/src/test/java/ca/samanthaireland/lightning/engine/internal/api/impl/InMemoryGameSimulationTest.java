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


package ca.samanthaireland.lightning.engine.internal.api.impl;

import ca.samanthaireland.lightning.engine.internal.GameLoop;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.CommandPayload;
import ca.samanthaireland.lightning.engine.core.command.CommandQueue;
import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.core.match.Match;
import ca.samanthaireland.lightning.engine.core.match.MatchService;
import ca.samanthaireland.lightning.engine.core.match.Player;
import ca.samanthaireland.lightning.engine.core.match.PlayerService;
import ca.samanthaireland.lightning.engine.core.session.PlayerSession;
import ca.samanthaireland.lightning.engine.core.session.PlayerSessionService;
import ca.samanthaireland.lightning.engine.core.session.SessionStatus;
import ca.samanthaireland.lightning.engine.internal.core.match.InMemoryGameSimulation;
import ca.samanthaireland.lightning.engine.internal.core.command.CommandResolver;
import ca.samanthaireland.lightning.engine.internal.core.snapshot.SnapshotProvider;
import ca.samanthaireland.lightning.engine.internal.ext.module.ModuleManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link InMemoryGameSimulation}.
 *
 * <p>Tests verify that GameSimulation correctly delegates to services.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InMemoryGameSimulation")
class InMemoryGameSimulationTest {

    @Mock
    private MatchService matchService;

    @Mock
    private PlayerService playerService;

    @Mock
    private PlayerSessionService sessionService;

    @Mock
    private ModuleManager moduleManager;

    @Mock
    private CommandResolver commandResolver;

    @Mock
    private CommandQueue commandQueue;

    @Mock
    private SnapshotProvider snapshotProvider;

    @Mock
    private GameLoop gameLoop;

    private InMemoryGameSimulation simulation;

    @BeforeEach
    void setUp() {
        simulation = new InMemoryGameSimulation(
                matchService, playerService, sessionService,
                moduleManager, commandResolver, commandQueue,
                snapshotProvider, gameLoop);
    }

    @Nested
    @DisplayName("tick")
    class Tick {

        @Test
        @DisplayName("should return zero initially")
        void shouldReturnZeroInitially() {
            assertThat(simulation.getCurrentTick()).isEqualTo(0);
        }

        @Test
        @DisplayName("should increment tick")
        void shouldIncrementTick() {
            assertThat(simulation.advanceTick()).isEqualTo(1);
            assertThat(simulation.advanceTick()).isEqualTo(2);
            assertThat(simulation.getCurrentTick()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("match operations")
    class MatchOperations {

        @Test
        @DisplayName("createMatch should delegate to service")
        void createMatchShouldDelegateToService() {
            Match match = new Match(1L, List.of(), List.of());
            when(matchService.createMatch(match)).thenReturn(match);

            simulation.createMatch(match);

            verify(matchService).createMatch(match);
        }

        @Test
        @DisplayName("deleteMatch should check existence and delete")
        void deleteMatchShouldCheckExistenceAndDelete() {
            when(matchService.matchExists(1L)).thenReturn(true);

            simulation.deleteMatch(1L);

            verify(matchService).matchExists(1L);
            verify(matchService).deleteMatch(1L);
        }

        @Test
        @DisplayName("deleteMatch non-existent should not call delete")
        void deleteMatchNonExistentShouldNotCallDelete() {
            when(matchService.matchExists(1L)).thenReturn(false);

            simulation.deleteMatch(1L);

            verify(matchService).matchExists(1L);
            verify(matchService, never()).deleteMatch(anyLong());
        }

        @Test
        @DisplayName("getMatch should delegate to service")
        void getMatchShouldDelegateToService() {
            Match match = new Match(1L, List.of(), List.of());
            when(matchService.getMatch(1L)).thenReturn(Optional.of(match));

            Optional<Match> result = simulation.getMatch(1L);

            assertThat(result).contains(match);
            verify(matchService).getMatch(1L);
        }

        @Test
        @DisplayName("getAllMatches should delegate to service")
        void getAllMatchesShouldDelegateToService() {
            List<Match> matches = List.of(new Match(1L, List.of(), List.of()), new Match(2L, List.of(), List.of()));
            when(matchService.getAllMatches()).thenReturn(matches);

            List<Match> result = simulation.getAllMatches();

            assertThat(result).isEqualTo(matches);
            verify(matchService).getAllMatches();
        }
    }

    @Nested
    @DisplayName("player operations")
    class PlayerOperations {

        @Test
        @DisplayName("createPlayer should delegate to service")
        void createPlayerShouldDelegateToService() {
            Player player = new Player(1L);
            when(playerService.createPlayer(player)).thenReturn(player);

            simulation.createPlayer(player);

            verify(playerService).createPlayer(player);
        }

        @Test
        @DisplayName("deletePlayer should check existence and delete")
        void deletePlayerShouldCheckExistenceAndDelete() {
            when(playerService.playerExists(1L)).thenReturn(true);

            simulation.deletePlayer(1L);

            verify(playerService).playerExists(1L);
            verify(playerService).deletePlayer(1L);
        }

        @Test
        @DisplayName("deletePlayer non-existent should not call delete")
        void deletePlayerNonExistentShouldNotCallDelete() {
            when(playerService.playerExists(1L)).thenReturn(false);

            simulation.deletePlayer(1L);

            verify(playerService).playerExists(1L);
            verify(playerService, never()).deletePlayer(anyLong());
        }

        @Test
        @DisplayName("getPlayer should delegate to service")
        void getPlayerShouldDelegateToService() {
            Player player = new Player(1L);
            when(playerService.getPlayer(1L)).thenReturn(Optional.of(player));

            Optional<Player> result = simulation.getPlayer(1L);

            assertThat(result).contains(player);
            verify(playerService).getPlayer(1L);
        }

        @Test
        @DisplayName("getAllPlayers should delegate to service")
        void getAllPlayersShouldDelegateToService() {
            List<Player> players = List.of(new Player(1L), new Player(2L));
            when(playerService.getAllPlayers()).thenReturn(players);

            List<Player> result = simulation.getAllPlayers();

            assertThat(result).isEqualTo(players);
            verify(playerService).getAllPlayers();
        }
    }

    @Nested
    @DisplayName("session operations")
    class SessionOperations {

        private PlayerSession createSession(long playerId, long matchId) {
            Instant now = Instant.now();
            return new PlayerSession(1L, playerId, matchId, SessionStatus.ACTIVE, now, now, null);
        }

        @Test
        @DisplayName("joinMatch should delegate to session service")
        void joinMatchShouldDelegateToSessionService() {
            PlayerSession session = createSession(1L, 100L);
            when(sessionService.createSession(1L, 100L)).thenReturn(session);

            PlayerSession result = simulation.joinMatch(1L, 100L);

            assertThat(result).isEqualTo(session);
            verify(sessionService).createSession(1L, 100L);
        }

        @Test
        @DisplayName("leaveMatch should check existence and abandon")
        void leaveMatchShouldCheckExistenceAndAbandon() {
            PlayerSession session = createSession(1L, 100L);
            when(sessionService.findSession(1L, 100L)).thenReturn(Optional.of(session));

            simulation.leaveMatch(1L, 100L);

            verify(sessionService).findSession(1L, 100L);
            verify(sessionService).abandon(1L, 100L);
        }

        @Test
        @DisplayName("leaveMatch non-existent should not call abandon")
        void leaveMatchNonExistentShouldNotCallAbandon() {
            when(sessionService.findSession(1L, 100L)).thenReturn(Optional.empty());

            simulation.leaveMatch(1L, 100L);

            verify(sessionService).findSession(1L, 100L);
            verify(sessionService, never()).abandon(anyLong(), anyLong());
        }

        @Test
        @DisplayName("getSession should delegate to service")
        void getSessionShouldDelegateToService() {
            PlayerSession session = createSession(1L, 100L);
            when(sessionService.findSession(1L, 100L)).thenReturn(Optional.of(session));

            Optional<PlayerSession> result = simulation.getSession(1L, 100L);

            assertThat(result).contains(session);
            verify(sessionService).findSession(1L, 100L);
        }

        @Test
        @DisplayName("getSessionsByMatch should delegate to service")
        void getSessionsByMatchShouldDelegateToService() {
            List<PlayerSession> sessions = List.of(
                    createSession(1L, 100L),
                    createSession(2L, 100L));
            when(sessionService.findMatchSessions(100L)).thenReturn(sessions);

            List<PlayerSession> result = simulation.getSessionsByMatch(100L);

            assertThat(result).isEqualTo(sessions);
            verify(sessionService).findMatchSessions(100L);
        }

        @Test
        @DisplayName("getSessionsByPlayer should filter all sessions")
        void getSessionsByPlayerShouldFilterAllSessions() {
            List<PlayerSession> allSessions = List.of(
                    createSession(1L, 100L),
                    createSession(1L, 200L),
                    createSession(2L, 100L));
            when(sessionService.findAllSessions()).thenReturn(allSessions);

            List<PlayerSession> result = simulation.getSessionsByPlayer(1L);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(s -> s.playerId() == 1L);
            verify(sessionService).findAllSessions();
        }
    }

    @Nested
    @DisplayName("module operations")
    class ModuleOperations {

        @Test
        @DisplayName("installModule should call module manager")
        void installModuleShouldCallModuleManager() throws Exception {
            Path jarPath = Paths.get("/path/to/module.jar");

            simulation.installModule("/path/to/module.jar");

            verify(moduleManager).installModule(jarPath);
        }

        @Test
        @DisplayName("installModule with null module manager should not throw")
        void installModuleWithNullModuleManagerShouldNotThrow() {
            InMemoryGameSimulation simWithoutModuleManager = new InMemoryGameSimulation(
                    matchService, playerService, sessionService, null, commandResolver, commandQueue,
                    snapshotProvider, gameLoop);

            simWithoutModuleManager.installModule("/path/to/module.jar");
            // Should not throw
        }
    }

    @Nested
    @DisplayName("command operations")
    class CommandOperations {

        @Test
        @DisplayName("enqueueCommand should resolve and schedule command")
        void enqueueCommandShouldResolveAndScheduleCommand() {
            EngineCommand command = mock(EngineCommand.class);
            CommandPayload payload = mock(CommandPayload.class);
            when(commandResolver.resolveByName("testCommand")).thenReturn(command);

            simulation.enqueueCommand("testCommand", payload);

            verify(commandResolver).resolveByName("testCommand");
            verify(commandQueue).enqueue(command, payload);
        }

        @Test
        @DisplayName("enqueueCommand with unknown command should throw")
        void enqueueCommandWithUnknownCommandShouldThrow() {
            when(commandResolver.resolveByName("unknownCommand")).thenReturn(null);

            assertThatThrownBy(() -> simulation.enqueueCommand("unknownCommand", mock(CommandPayload.class)))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(commandResolver).resolveByName("unknownCommand");
            verifyNoInteractions(commandQueue);
        }
    }
}
