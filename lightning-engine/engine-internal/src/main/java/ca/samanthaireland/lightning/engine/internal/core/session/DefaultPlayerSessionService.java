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
import ca.samanthaireland.lightning.engine.core.session.PlayerSessionService;
import ca.samanthaireland.lightning.engine.core.session.SessionStatus;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link PlayerSessionService}.
 *
 * <p>This implementation provides business logic for player session operations,
 * delegating persistence to {@link PlayerSessionRepository}.
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>Player and match must exist before creating a session</li>
 *   <li>Player cannot have multiple active sessions in the same match</li>
 *   <li>Only DISCONNECTED sessions can be reconnected</li>
 *   <li>Stale sessions expire after configurable timeout</li>
 * </ul>
 *
 * <p>SOLID compliance:
 * <ul>
 *   <li>SRP: Business logic only, persistence delegated to repository</li>
 *   <li>DIP: Depends on repository and service abstractions</li>
 * </ul>
 */
@Slf4j
public class DefaultPlayerSessionService implements PlayerSessionService {

    private final PlayerSessionRepository sessionRepository;
    private final PlayerService playerService;
    private final MatchService matchService;

    public DefaultPlayerSessionService(
            PlayerSessionRepository sessionRepository,
            PlayerService playerService,
            MatchService matchService) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository,
            "sessionRepository must not be null");
        this.playerService = Objects.requireNonNull(playerService,
            "playerService must not be null");
        this.matchService = Objects.requireNonNull(matchService,
            "matchService must not be null");
    }

    @Override
    public PlayerSession createSession(long playerId, long matchId) {
        log.info("Creating session for player {} in match {}", playerId, matchId);

        // Validate player exists
        if (!playerService.playerExists(playerId)) {
            throw new EntityNotFoundException(
                String.format("Player %d not found.", playerId));
        }

        // Validate match exists
        if (!matchService.matchExists(matchId)) {
            throw new EntityNotFoundException(
                String.format("Match %d not found.", matchId));
        }

        return createSessionInternal(playerId, matchId);
    }

    @Override
    public PlayerSession createSessionForContainer(long playerId, long matchId) {
        log.info("Creating container-scoped session for player {} in match {}", playerId, matchId);

        // Validate player exists (match validation skipped - already done by container)
        if (!playerService.playerExists(playerId)) {
            throw new EntityNotFoundException(
                String.format("Player %d not found.", playerId));
        }

        return createSessionInternal(playerId, matchId);
    }

    private PlayerSession createSessionInternal(long playerId, long matchId) {
        // Check for existing session
        Optional<PlayerSession> existing = sessionRepository
            .findByPlayerAndMatch(playerId, matchId);

        if (existing.isPresent()) {
            PlayerSession session = existing.get();

            // If already active, throw conflict
            if (session.status() == SessionStatus.ACTIVE) {
                throw new ConflictException(
                    String.format("Player %d already has an active session in match %d.",
                        playerId, matchId));
            }

            // If disconnected, reactivate the session
            if (session.status() == SessionStatus.DISCONNECTED) {
                log.info("Reactivating disconnected session {} for player {} in match {}",
                    session.id(), playerId, matchId);
                return sessionRepository.save(session.withStatus(SessionStatus.ACTIVE));
            }

            // For EXPIRED or ABANDONED, create a new session
            log.info("Replacing {} session for player {} in match {}",
                session.status(), playerId, matchId);
            sessionRepository.deleteById(session.id());
        }

        // Create new session
        PlayerSession newSession = PlayerSession.create(playerId, matchId);
        return sessionRepository.save(newSession);
    }

    @Override
    public PlayerSession reconnect(long playerId, long matchId) {
        log.info("Player {} reconnecting to match {}", playerId, matchId);

        PlayerSession session = sessionRepository.findByPlayerAndMatch(playerId, matchId)
            .orElseThrow(() -> new EntityNotFoundException(
                String.format("No session found for player %d in match %d.", playerId, matchId)));

        if (!session.canReconnect()) {
            throw new IllegalStateException(
                String.format("Session cannot be reconnected. Current status: %s", session.status()));
        }

        PlayerSession reconnected = session.withStatus(SessionStatus.ACTIVE);
        log.info("Reconnected session {} for player {} in match {}",
            session.id(), playerId, matchId);

        return sessionRepository.save(reconnected);
    }

    @Override
    public void disconnect(long playerId, long matchId) {
        log.info("Disconnecting player {} from match {}", playerId, matchId);

        PlayerSession session = sessionRepository.findByPlayerAndMatch(playerId, matchId)
            .orElseThrow(() -> new EntityNotFoundException(
                String.format("No session found for player %d in match %d.", playerId, matchId)));

        if (session.status() != SessionStatus.ACTIVE) {
            log.warn("Session {} is not active (status={}), disconnect has no effect",
                session.id(), session.status());
            return;
        }

        PlayerSession disconnected = session.withStatus(SessionStatus.DISCONNECTED);
        sessionRepository.save(disconnected);

        log.info("Disconnected session {} for player {} in match {}",
            session.id(), playerId, matchId);
    }

    @Override
    public void abandon(long playerId, long matchId) {
        log.info("Player {} abandoning match {}", playerId, matchId);

        PlayerSession session = sessionRepository.findByPlayerAndMatch(playerId, matchId)
            .orElseThrow(() -> new EntityNotFoundException(
                String.format("No session found for player %d in match %d.", playerId, matchId)));

        PlayerSession abandoned = session.withStatus(SessionStatus.ABANDONED);
        sessionRepository.save(abandoned);

        log.info("Abandoned session {} for player {} in match {}",
            session.id(), playerId, matchId);
    }

    @Override
    public int expireStaleSessions(Duration timeout) {
        log.debug("Expiring stale sessions older than {}", timeout);

        List<PlayerSession> disconnected = sessionRepository.findByStatus(SessionStatus.DISCONNECTED);
        Instant cutoff = Instant.now().minus(timeout);
        int expiredCount = 0;

        for (PlayerSession session : disconnected) {
            if (session.disconnectedAt() != null && session.disconnectedAt().isBefore(cutoff)) {
                PlayerSession expired = session.withStatus(SessionStatus.EXPIRED);
                sessionRepository.save(expired);
                expiredCount++;
                log.debug("Expired session {} (disconnected at {})",
                    session.id(), session.disconnectedAt());
            }
        }

        if (expiredCount > 0) {
            log.info("Expired {} stale sessions", expiredCount);
        }

        return expiredCount;
    }

    @Override
    public Optional<PlayerSession> findActiveSession(long playerId, long matchId) {
        return sessionRepository.findByPlayerAndMatch(playerId, matchId)
            .filter(PlayerSession::isActive);
    }

    @Override
    public Optional<PlayerSession> findSession(long playerId, long matchId) {
        return sessionRepository.findByPlayerAndMatch(playerId, matchId);
    }

    @Override
    public List<PlayerSession> findMatchSessions(long matchId) {
        return sessionRepository.findByMatchId(matchId);
    }

    @Override
    public List<PlayerSession> findActiveMatchSessions(long matchId) {
        return sessionRepository.findByMatchIdAndStatus(matchId, SessionStatus.ACTIVE);
    }

    @Override
    public boolean canReconnect(long playerId, long matchId) {
        return sessionRepository.findByPlayerAndMatch(playerId, matchId)
            .map(PlayerSession::canReconnect)
            .orElse(false);
    }

    @Override
    public List<PlayerSession> findAllSessions() {
        return sessionRepository.findAll();
    }

    @Override
    public void recordActivity(long playerId, long matchId) {
        sessionRepository.findByPlayerAndMatch(playerId, matchId)
            .filter(PlayerSession::isActive)
            .ifPresent(session -> {
                sessionRepository.save(session.withActivity());
                log.debug("Recorded activity for session {}", session.id());
            });
    }
}
