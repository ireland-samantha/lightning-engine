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

import ca.samanthaireland.lightning.engine.core.match.Player;
import ca.samanthaireland.lightning.engine.core.match.PlayerRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link PlayerRepository}.
 *
 * <p>This implementation stores players in a thread-safe ConcurrentHashMap,
 * keyed by their ID. Provides pure CRUD operations without business logic.
 *
 * <p>Thread-safe: All operations use ConcurrentHashMap for thread safety.
 */
@Slf4j
public class InMemoryPlayerRepository implements PlayerRepository {

    private final Map<Long, Player> players = new ConcurrentHashMap<>();

    @Override
    public Player save(Player player) {
        Objects.requireNonNull(player, "player must not be null");
        log.debug("Saving player: {}", player.id());
        players.put(player.id(), player);
        return player;
    }

    @Override
    public void deleteById(long id) {
        log.debug("Deleting player: {}", id);
        players.remove(id);
    }

    @Override
    public Optional<Player> findById(long id) {
        Player value = players.get(id);
        log.debug("Find player by id {}: {}", id, value != null ? "found" : "not found");
        return Optional.ofNullable(value);
    }

    @Override
    public List<Player> findAll() {
        return new ArrayList<>(players.values());
    }

    @Override
    public boolean existsById(long id) {
        return players.containsKey(id);
    }

    @Override
    public long count() {
        return players.size();
    }

    /**
     * Clear all players from the repository.
     * For testing purposes.
     */
    public void clear() {
        players.clear();
    }
}
