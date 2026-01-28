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

import ca.samanthaireland.lightning.engine.core.session.PlayerSession;
import ca.samanthaireland.lightning.engine.core.session.PlayerSessionRepository;
import ca.samanthaireland.lightning.engine.core.session.SessionStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of {@link PlayerSessionRepository}.
 *
 * <p>This implementation stores player sessions in a thread-safe ConcurrentHashMap,
 * using the session ID as the primary key. A secondary index by (playerId, matchId)
 * is maintained for efficient lookups.
 *
 * <p>Thread-safe: All operations use ConcurrentHashMap for thread safety.
 */
@Slf4j
public class InMemoryPlayerSessionRepository implements PlayerSessionRepository {

    private final Map<Long, PlayerSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<CompositeKey, Long> sessionIdByPlayerMatch = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    private record CompositeKey(long playerId, long matchId) {}

    @Override
    public PlayerSession save(PlayerSession session) {
        Objects.requireNonNull(session, "session must not be null");

        PlayerSession toSave = session;

        // Generate ID for new sessions
        if (session.id() <= 0) {
            long newId = idGenerator.getAndIncrement();
            toSave = new PlayerSession(
                newId,
                session.playerId(),
                session.matchId(),
                session.status(),
                session.connectedAt(),
                session.lastActivityAt(),
                session.disconnectedAt()
            );
            log.debug("Generated new session ID {} for player {} in match {}",
                newId, session.playerId(), session.matchId());
        }

        // Update primary index
        sessionsById.put(toSave.id(), toSave);

        // Update secondary index
        CompositeKey key = new CompositeKey(toSave.playerId(), toSave.matchId());
        sessionIdByPlayerMatch.put(key, toSave.id());

        log.debug("Saved session {}: player={}, match={}, status={}",
            toSave.id(), toSave.playerId(), toSave.matchId(), toSave.status());

        return toSave;
    }

    @Override
    public Optional<PlayerSession> findById(long id) {
        PlayerSession session = sessionsById.get(id);
        log.debug("Find session by id={}: {}", id, session != null ? "found" : "not found");
        return Optional.ofNullable(session);
    }

    @Override
    public Optional<PlayerSession> findByPlayerAndMatch(long playerId, long matchId) {
        CompositeKey key = new CompositeKey(playerId, matchId);
        Long sessionId = sessionIdByPlayerMatch.get(key);

        if (sessionId == null) {
            log.debug("Find session by player={}, match={}: not found", playerId, matchId);
            return Optional.empty();
        }

        PlayerSession session = sessionsById.get(sessionId);
        log.debug("Find session by player={}, match={}: {}",
            playerId, matchId, session != null ? "found" : "not found");
        return Optional.ofNullable(session);
    }

    @Override
    public List<PlayerSession> findByMatchId(long matchId) {
        List<PlayerSession> result = new ArrayList<>();
        for (PlayerSession session : sessionsById.values()) {
            if (session.matchId() == matchId) {
                result.add(session);
            }
        }
        log.debug("Find sessions by matchId={}: found {}", matchId, result.size());
        return result;
    }

    @Override
    public List<PlayerSession> findByPlayerId(long playerId) {
        List<PlayerSession> result = new ArrayList<>();
        for (PlayerSession session : sessionsById.values()) {
            if (session.playerId() == playerId) {
                result.add(session);
            }
        }
        log.debug("Find sessions by playerId={}: found {}", playerId, result.size());
        return result;
    }

    @Override
    public List<PlayerSession> findByStatus(SessionStatus status) {
        List<PlayerSession> result = new ArrayList<>();
        for (PlayerSession session : sessionsById.values()) {
            if (session.status() == status) {
                result.add(session);
            }
        }
        log.debug("Find sessions by status={}: found {}", status, result.size());
        return result;
    }

    @Override
    public List<PlayerSession> findByMatchIdAndStatus(long matchId, SessionStatus status) {
        List<PlayerSession> result = new ArrayList<>();
        for (PlayerSession session : sessionsById.values()) {
            if (session.matchId() == matchId && session.status() == status) {
                result.add(session);
            }
        }
        log.debug("Find sessions by matchId={}, status={}: found {}", matchId, status, result.size());
        return result;
    }

    @Override
    public void deleteById(long id) {
        PlayerSession session = sessionsById.remove(id);
        if (session != null) {
            CompositeKey key = new CompositeKey(session.playerId(), session.matchId());
            sessionIdByPlayerMatch.remove(key);
            log.debug("Deleted session {}", id);
        }
    }

    @Override
    public void deleteByMatchId(long matchId) {
        List<Long> toDelete = new ArrayList<>();

        for (PlayerSession session : sessionsById.values()) {
            if (session.matchId() == matchId) {
                toDelete.add(session.id());
            }
        }

        for (Long id : toDelete) {
            deleteById(id);
        }

        log.debug("Deleted {} sessions for matchId={}", toDelete.size(), matchId);
    }

    @Override
    public boolean existsByPlayerAndMatch(long playerId, long matchId) {
        CompositeKey key = new CompositeKey(playerId, matchId);
        return sessionIdByPlayerMatch.containsKey(key);
    }

    @Override
    public long count() {
        return sessionsById.size();
    }

    @Override
    public List<PlayerSession> findAll() {
        log.debug("Find all sessions: found {}", sessionsById.size());
        return new ArrayList<>(sessionsById.values());
    }

    /**
     * Clear all sessions from the repository.
     * For testing purposes.
     */
    public void clear() {
        sessionsById.clear();
        sessionIdByPlayerMatch.clear();
        log.debug("Cleared all sessions");
    }
}
