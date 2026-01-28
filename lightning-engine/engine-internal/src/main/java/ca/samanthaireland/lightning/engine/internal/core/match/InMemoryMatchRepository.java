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


package ca.samanthaireland.lightning.engine.internal.core.match;

import ca.samanthaireland.lightning.engine.core.match.Match;
import ca.samanthaireland.lightning.engine.core.match.MatchRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of {@link MatchRepository}.
 *
 * <p>This implementation stores matches in a thread-safe ConcurrentHashMap,
 * keyed by their ID. Provides pure CRUD operations without business logic.
 *
 * <p>Thread-safe: All operations use ConcurrentHashMap for thread safety.
 */
@Slf4j
public class InMemoryMatchRepository implements MatchRepository {

    private final Map<Long, Match> matches = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Match save(Match match) {
        Objects.requireNonNull(match, "match must not be null");
        // If id is 0 or negative, generate a new ID
        Match matchToSave = match;
        if (match.id() <= 0) {
            long newId = idGenerator.getAndIncrement();
            matchToSave = new Match(newId, match.enabledModules(), match.enabledAIs());
            log.debug("Generated new match ID: {}", newId);
        }
        log.debug("Saving match: {}", matchToSave.id());
        matches.put(matchToSave.id(), matchToSave);
        return matchToSave;
    }

    @Override
    public void deleteById(long id) {
        log.debug("Deleting match: {}", id);
        matches.remove(id);
    }

    @Override
    public Optional<Match> findById(long id) {
        Match value = matches.get(id);
        log.debug("Find match by id {}: {}", id, value != null ? "found" : "not found");
        return Optional.ofNullable(value);
    }

    @Override
    public List<Match> findAll() {
        return new ArrayList<>(matches.values());
    }

    @Override
    public boolean existsById(long id) {
        return matches.containsKey(id);
    }

    @Override
    public long count() {
        return matches.size();
    }

    /**
     * Clear all matches from the repository.
     * For testing purposes.
     */
    public void clear() {
        matches.clear();
    }
}
