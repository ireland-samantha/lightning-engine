/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.internal.container;

import ca.samanthaireland.lightning.engine.core.container.ContainerSessionOperations;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import ca.samanthaireland.lightning.engine.core.exception.ConflictException;
import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.core.session.PlayerSession;
import ca.samanthaireland.lightning.engine.core.session.PlayerSessionRepository;
import ca.samanthaireland.lightning.engine.core.session.SessionStatus;
import ca.samanthaireland.lightning.engine.internal.core.session.InMemoryPlayerSessionRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of ContainerSessionOperations.
 *
 * <p>Each container has its own session store, providing complete
 * isolation between containers.</p>
 */
@Slf4j
public final class DefaultContainerSessionOperations implements ContainerSessionOperations {

    private final ExecutionContainer container;
    private final PlayerSessionRepository sessionRepository;

    public DefaultContainerSessionOperations(ExecutionContainer container) {
        this.container = container;
        // Each container gets its own session repository for isolation
        this.sessionRepository = new InMemoryPlayerSessionRepository();
    }

    @Override
    public PlayerSession create(long playerId, long matchId) {
        log.info("Creating session for player {} in match {} (container {})",
                playerId, matchId, container.getId());

        // Validate player exists in this container
        if (!container.players().has(playerId)) {
            throw new EntityNotFoundException(
                    String.format("Player %d not found in container %d.", playerId, container.getId()));
        }

        return createSessionInternal(playerId, matchId);
    }

    private PlayerSession createSessionInternal(long playerId, long matchId) {
        // Check for existing session
        Optional<PlayerSession> existing = sessionRepository.findByPlayerAndMatch(playerId, matchId);

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
        log.info("Player {} reconnecting to match {} (container {})", playerId, matchId, container.getId());

        PlayerSession session = sessionRepository.findByPlayerAndMatch(playerId, matchId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("No session found for player %d in match %d.", playerId, matchId)));

        if (!session.canReconnect()) {
            throw new IllegalStateException(
                    String.format("Session cannot be reconnected. Current status: %s", session.status()));
        }

        PlayerSession reconnected = session.withStatus(SessionStatus.ACTIVE);
        log.info("Reconnected session {} for player {} in match {}", session.id(), playerId, matchId);

        return sessionRepository.save(reconnected);
    }

    @Override
    public void disconnect(long playerId, long matchId) {
        log.info("Disconnecting player {} from match {} (container {})", playerId, matchId, container.getId());

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

        log.info("Disconnected session {} for player {} in match {}", session.id(), playerId, matchId);
    }

    @Override
    public void abandon(long playerId, long matchId) {
        log.info("Player {} abandoning match {} (container {})", playerId, matchId, container.getId());

        PlayerSession session = sessionRepository.findByPlayerAndMatch(playerId, matchId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("No session found for player %d in match %d.", playerId, matchId)));

        PlayerSession abandoned = session.withStatus(SessionStatus.ABANDONED);
        sessionRepository.save(abandoned);

        log.info("Abandoned session {} for player {} in match {}", session.id(), playerId, matchId);
    }

    @Override
    public int expireStale(Duration timeout) {
        log.debug("Expiring stale sessions older than {} (container {})", timeout, container.getId());

        List<PlayerSession> disconnected = sessionRepository.findByStatus(SessionStatus.DISCONNECTED);
        Instant cutoff = Instant.now().minus(timeout);
        int expiredCount = 0;

        for (PlayerSession session : disconnected) {
            if (session.disconnectedAt() != null && session.disconnectedAt().isBefore(cutoff)) {
                PlayerSession expired = session.withStatus(SessionStatus.EXPIRED);
                sessionRepository.save(expired);
                expiredCount++;
                log.debug("Expired session {} (disconnected at {})", session.id(), session.disconnectedAt());
            }
        }

        if (expiredCount > 0) {
            log.info("Expired {} stale sessions in container {}", expiredCount, container.getId());
        }

        return expiredCount;
    }

    @Override
    public Optional<PlayerSession> findActive(long playerId, long matchId) {
        return sessionRepository.findByPlayerAndMatch(playerId, matchId)
                .filter(PlayerSession::isActive);
    }

    @Override
    public Optional<PlayerSession> find(long playerId, long matchId) {
        return sessionRepository.findByPlayerAndMatch(playerId, matchId);
    }

    @Override
    public List<PlayerSession> forMatch(long matchId) {
        return sessionRepository.findByMatchId(matchId);
    }

    @Override
    public List<PlayerSession> activeForMatch(long matchId) {
        return sessionRepository.findByMatchIdAndStatus(matchId, SessionStatus.ACTIVE);
    }

    @Override
    public List<PlayerSession> all() {
        return sessionRepository.findAll();
    }

    @Override
    public boolean canReconnect(long playerId, long matchId) {
        return sessionRepository.findByPlayerAndMatch(playerId, matchId)
                .map(PlayerSession::canReconnect)
                .orElse(false);
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

    @Override
    public int count() {
        return (int) sessionRepository.count();
    }
}
