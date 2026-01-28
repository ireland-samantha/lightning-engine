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

package ca.samanthaireland.lightning.engine.internal.core.session;

import ca.samanthaireland.lightning.engine.core.exception.ConflictException;
import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.core.match.MatchService;
import ca.samanthaireland.lightning.engine.core.match.PlayerService;
import ca.samanthaireland.lightning.engine.core.session.PlayerSession;
import ca.samanthaireland.lightning.engine.core.session.PlayerSessionRepository;
import ca.samanthaireland.lightning.engine.core.session.SessionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultPlayerSessionService")
class DefaultPlayerSessionServiceTest {

    @Mock
    private PlayerSessionRepository sessionRepository;

    @Mock
    private PlayerService playerService;

    @Mock
    private MatchService matchService;

    private DefaultPlayerSessionService service;

    private static final long PLAYER_ID = 1L;
    private static final long MATCH_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new DefaultPlayerSessionService(sessionRepository, playerService, matchService);
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("should throw when sessionRepository is null")
        void shouldThrowWhenSessionRepositoryIsNull() {
            assertThatThrownBy(() -> new DefaultPlayerSessionService(null, playerService, matchService))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("sessionRepository");
        }

        @Test
        @DisplayName("should throw when playerService is null")
        void shouldThrowWhenPlayerServiceIsNull() {
            assertThatThrownBy(() -> new DefaultPlayerSessionService(sessionRepository, null, matchService))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("playerService");
        }

        @Test
        @DisplayName("should throw when matchService is null")
        void shouldThrowWhenMatchServiceIsNull() {
            assertThatThrownBy(() -> new DefaultPlayerSessionService(sessionRepository, playerService, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("matchService");
        }
    }

    @Nested
    @DisplayName("createSession")
    class CreateSession {

        @Test
        @DisplayName("should create new session when player and match exist")
        void shouldCreateNewSessionWhenPlayerAndMatchExist() {
            when(playerService.playerExists(PLAYER_ID)).thenReturn(true);
            when(matchService.matchExists(MATCH_ID)).thenReturn(true);
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.empty());
            PlayerSession newSession = PlayerSession.create(PLAYER_ID, MATCH_ID);
            when(sessionRepository.save(any(PlayerSession.class))).thenReturn(newSession);

            PlayerSession result = service.createSession(PLAYER_ID, MATCH_ID);

            assertThat(result.playerId()).isEqualTo(PLAYER_ID);
            assertThat(result.matchId()).isEqualTo(MATCH_ID);
            assertThat(result.status()).isEqualTo(SessionStatus.ACTIVE);
            verify(sessionRepository).save(any(PlayerSession.class));
        }

        @Test
        @DisplayName("should throw when player does not exist")
        void shouldThrowWhenPlayerDoesNotExist() {
            when(playerService.playerExists(PLAYER_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.createSession(PLAYER_ID, MATCH_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Player");

            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when match does not exist")
        void shouldThrowWhenMatchDoesNotExist() {
            when(playerService.playerExists(PLAYER_ID)).thenReturn(true);
            when(matchService.matchExists(MATCH_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.createSession(PLAYER_ID, MATCH_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Match");

            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when player already has active session")
        void shouldThrowWhenPlayerAlreadyHasActiveSession() {
            when(playerService.playerExists(PLAYER_ID)).thenReturn(true);
            when(matchService.matchExists(MATCH_ID)).thenReturn(true);
            PlayerSession activeSession = createSession(1L, PLAYER_ID, MATCH_ID, SessionStatus.ACTIVE);
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.of(activeSession));

            assertThatThrownBy(() -> service.createSession(PLAYER_ID, MATCH_ID))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("active session");
        }

        @Test
        @DisplayName("should reactivate disconnected session")
        void shouldReactivateDisconnectedSession() {
            when(playerService.playerExists(PLAYER_ID)).thenReturn(true);
            when(matchService.matchExists(MATCH_ID)).thenReturn(true);
            PlayerSession disconnected = createSession(1L, PLAYER_ID, MATCH_ID, SessionStatus.DISCONNECTED);
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.of(disconnected));
            PlayerSession reactivated = disconnected.withStatus(SessionStatus.ACTIVE);
            when(sessionRepository.save(any(PlayerSession.class))).thenReturn(reactivated);

            PlayerSession result = service.createSession(PLAYER_ID, MATCH_ID);

            assertThat(result.status()).isEqualTo(SessionStatus.ACTIVE);
        }

        @Test
        @DisplayName("should replace expired session with new one")
        void shouldReplaceExpiredSessionWithNewOne() {
            when(playerService.playerExists(PLAYER_ID)).thenReturn(true);
            when(matchService.matchExists(MATCH_ID)).thenReturn(true);
            PlayerSession expired = createSession(1L, PLAYER_ID, MATCH_ID, SessionStatus.EXPIRED);
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.of(expired));
            PlayerSession newSession = PlayerSession.create(PLAYER_ID, MATCH_ID);
            when(sessionRepository.save(any(PlayerSession.class))).thenReturn(newSession);

            PlayerSession result = service.createSession(PLAYER_ID, MATCH_ID);

            verify(sessionRepository).deleteById(1L);
            assertThat(result.status()).isEqualTo(SessionStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("createSessionForContainer")
    class CreateSessionForContainer {

        @Test
        @DisplayName("should create session without validating match")
        void shouldCreateSessionWithoutValidatingMatch() {
            when(playerService.playerExists(PLAYER_ID)).thenReturn(true);
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.empty());
            PlayerSession newSession = PlayerSession.create(PLAYER_ID, MATCH_ID);
            when(sessionRepository.save(any(PlayerSession.class))).thenReturn(newSession);

            PlayerSession result = service.createSessionForContainer(PLAYER_ID, MATCH_ID);

            assertThat(result.playerId()).isEqualTo(PLAYER_ID);
            verify(matchService, never()).matchExists(anyLong());
        }

        @Test
        @DisplayName("should throw when player does not exist")
        void shouldThrowWhenPlayerDoesNotExist() {
            when(playerService.playerExists(PLAYER_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.createSessionForContainer(PLAYER_ID, MATCH_ID))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("reconnect")
    class Reconnect {

        @Test
        @DisplayName("should reconnect disconnected session")
        void shouldReconnectDisconnectedSession() {
            PlayerSession disconnected = createSession(1L, PLAYER_ID, MATCH_ID, SessionStatus.DISCONNECTED);
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.of(disconnected));
            PlayerSession reconnected = disconnected.withStatus(SessionStatus.ACTIVE);
            when(sessionRepository.save(any(PlayerSession.class))).thenReturn(reconnected);

            PlayerSession result = service.reconnect(PLAYER_ID, MATCH_ID);

            assertThat(result.status()).isEqualTo(SessionStatus.ACTIVE);
            verify(sessionRepository).save(any(PlayerSession.class));
        }

        @Test
        @DisplayName("should throw when no session found")
        void shouldThrowWhenNoSessionFound() {
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.reconnect(PLAYER_ID, MATCH_ID))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when session cannot reconnect")
        void shouldThrowWhenSessionCannotReconnect() {
            PlayerSession expired = createSession(1L, PLAYER_ID, MATCH_ID, SessionStatus.EXPIRED);
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.of(expired));

            assertThatThrownBy(() -> service.reconnect(PLAYER_ID, MATCH_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("cannot be reconnected");
        }
    }

    @Nested
    @DisplayName("disconnect")
    class Disconnect {

        @Test
        @DisplayName("should disconnect active session")
        void shouldDisconnectActiveSession() {
            PlayerSession active = createSession(1L, PLAYER_ID, MATCH_ID, SessionStatus.ACTIVE);
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.of(active));

            service.disconnect(PLAYER_ID, MATCH_ID);

            verify(sessionRepository).save(any(PlayerSession.class));
        }

        @Test
        @DisplayName("should throw when no session found")
        void shouldThrowWhenNoSessionFound() {
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.disconnect(PLAYER_ID, MATCH_ID))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("should not save when session is not active")
        void shouldNotSaveWhenSessionIsNotActive() {
            PlayerSession disconnected = createSession(1L, PLAYER_ID, MATCH_ID, SessionStatus.DISCONNECTED);
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.of(disconnected));

            service.disconnect(PLAYER_ID, MATCH_ID);

            verify(sessionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("abandon")
    class Abandon {

        @Test
        @DisplayName("should abandon session")
        void shouldAbandonSession() {
            PlayerSession active = createSession(1L, PLAYER_ID, MATCH_ID, SessionStatus.ACTIVE);
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.of(active));

            service.abandon(PLAYER_ID, MATCH_ID);

            verify(sessionRepository).save(any(PlayerSession.class));
        }

        @Test
        @DisplayName("should throw when no session found")
        void shouldThrowWhenNoSessionFound() {
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.abandon(PLAYER_ID, MATCH_ID))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("expireStaleSessions")
    class ExpireStaleSessions {

        @Test
        @DisplayName("should expire sessions older than timeout")
        void shouldExpireSessionsOlderThanTimeout() {
            Instant oldDisconnect = Instant.now().minus(Duration.ofMinutes(10));
            PlayerSession stale = new PlayerSession(1L, PLAYER_ID, MATCH_ID, SessionStatus.DISCONNECTED,
                    Instant.now(), Instant.now(), oldDisconnect);
            when(sessionRepository.findByStatus(SessionStatus.DISCONNECTED)).thenReturn(List.of(stale));

            int expired = service.expireStaleSessions(Duration.ofMinutes(5));

            assertThat(expired).isEqualTo(1);
            verify(sessionRepository).save(any(PlayerSession.class));
        }

        @Test
        @DisplayName("should not expire sessions within timeout")
        void shouldNotExpireSessionsWithinTimeout() {
            Instant recentDisconnect = Instant.now().minus(Duration.ofMinutes(2));
            PlayerSession recent = new PlayerSession(1L, PLAYER_ID, MATCH_ID, SessionStatus.DISCONNECTED,
                    Instant.now(), Instant.now(), recentDisconnect);
            when(sessionRepository.findByStatus(SessionStatus.DISCONNECTED)).thenReturn(List.of(recent));

            int expired = service.expireStaleSessions(Duration.ofMinutes(5));

            assertThat(expired).isEqualTo(0);
            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should return 0 when no disconnected sessions")
        void shouldReturnZeroWhenNoDisconnectedSessions() {
            when(sessionRepository.findByStatus(SessionStatus.DISCONNECTED)).thenReturn(List.of());

            int expired = service.expireStaleSessions(Duration.ofMinutes(5));

            assertThat(expired).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("findActiveSession")
    class FindActiveSession {

        @Test
        @DisplayName("should return active session")
        void shouldReturnActiveSession() {
            PlayerSession active = createSession(1L, PLAYER_ID, MATCH_ID, SessionStatus.ACTIVE);
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.of(active));

            Optional<PlayerSession> result = service.findActiveSession(PLAYER_ID, MATCH_ID);

            assertThat(result).isPresent();
            assertThat(result.get().status()).isEqualTo(SessionStatus.ACTIVE);
        }

        @Test
        @DisplayName("should return empty for non-active session")
        void shouldReturnEmptyForNonActiveSession() {
            PlayerSession disconnected = createSession(1L, PLAYER_ID, MATCH_ID, SessionStatus.DISCONNECTED);
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.of(disconnected));

            Optional<PlayerSession> result = service.findActiveSession(PLAYER_ID, MATCH_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findSession")
    class FindSession {

        @Test
        @DisplayName("should return session regardless of status")
        void shouldReturnSessionRegardlessOfStatus() {
            PlayerSession disconnected = createSession(1L, PLAYER_ID, MATCH_ID, SessionStatus.DISCONNECTED);
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.of(disconnected));

            Optional<PlayerSession> result = service.findSession(PLAYER_ID, MATCH_ID);

            assertThat(result).isPresent();
        }
    }

    @Nested
    @DisplayName("findMatchSessions")
    class FindMatchSessions {

        @Test
        @DisplayName("should return all sessions for match")
        void shouldReturnAllSessionsForMatch() {
            List<PlayerSession> sessions = List.of(
                    createSession(1L, 1L, MATCH_ID, SessionStatus.ACTIVE),
                    createSession(2L, 2L, MATCH_ID, SessionStatus.DISCONNECTED)
            );
            when(sessionRepository.findByMatchId(MATCH_ID)).thenReturn(sessions);

            List<PlayerSession> result = service.findMatchSessions(MATCH_ID);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findActiveMatchSessions")
    class FindActiveMatchSessions {

        @Test
        @DisplayName("should return only active sessions for match")
        void shouldReturnOnlyActiveSessionsForMatch() {
            List<PlayerSession> activeSessions = List.of(
                    createSession(1L, 1L, MATCH_ID, SessionStatus.ACTIVE)
            );
            when(sessionRepository.findByMatchIdAndStatus(MATCH_ID, SessionStatus.ACTIVE)).thenReturn(activeSessions);

            List<PlayerSession> result = service.findActiveMatchSessions(MATCH_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).status()).isEqualTo(SessionStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("canReconnect")
    class CanReconnect {

        @Test
        @DisplayName("should return true for disconnected session")
        void shouldReturnTrueForDisconnectedSession() {
            PlayerSession disconnected = createSession(1L, PLAYER_ID, MATCH_ID, SessionStatus.DISCONNECTED);
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.of(disconnected));

            boolean result = service.canReconnect(PLAYER_ID, MATCH_ID);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for active session")
        void shouldReturnFalseForActiveSession() {
            PlayerSession active = createSession(1L, PLAYER_ID, MATCH_ID, SessionStatus.ACTIVE);
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.of(active));

            boolean result = service.canReconnect(PLAYER_ID, MATCH_ID);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when no session exists")
        void shouldReturnFalseWhenNoSessionExists() {
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.empty());

            boolean result = service.canReconnect(PLAYER_ID, MATCH_ID);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("findAllSessions")
    class FindAllSessions {

        @Test
        @DisplayName("should return all sessions")
        void shouldReturnAllSessions() {
            List<PlayerSession> sessions = List.of(
                    createSession(1L, 1L, 100L, SessionStatus.ACTIVE),
                    createSession(2L, 2L, 200L, SessionStatus.DISCONNECTED)
            );
            when(sessionRepository.findAll()).thenReturn(sessions);

            List<PlayerSession> result = service.findAllSessions();

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("recordActivity")
    class RecordActivity {

        @Test
        @DisplayName("should record activity for active session")
        void shouldRecordActivityForActiveSession() {
            PlayerSession active = createSession(1L, PLAYER_ID, MATCH_ID, SessionStatus.ACTIVE);
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.of(active));

            service.recordActivity(PLAYER_ID, MATCH_ID);

            verify(sessionRepository).save(any(PlayerSession.class));
        }

        @Test
        @DisplayName("should not record activity for non-active session")
        void shouldNotRecordActivityForNonActiveSession() {
            PlayerSession disconnected = createSession(1L, PLAYER_ID, MATCH_ID, SessionStatus.DISCONNECTED);
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.of(disconnected));

            service.recordActivity(PLAYER_ID, MATCH_ID);

            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not throw when no session exists")
        void shouldNotThrowWhenNoSessionExists() {
            when(sessionRepository.findByPlayerAndMatch(PLAYER_ID, MATCH_ID)).thenReturn(Optional.empty());

            service.recordActivity(PLAYER_ID, MATCH_ID);

            verify(sessionRepository, never()).save(any());
        }
    }

    private static PlayerSession createSession(long id, long playerId, long matchId, SessionStatus status) {
        return new PlayerSession(id, playerId, matchId, status, Instant.now(), Instant.now(), null);
    }
}
