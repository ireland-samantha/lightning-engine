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

import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.core.match.Player;
import ca.samanthaireland.lightning.engine.core.match.PlayerRepository;
import ca.samanthaireland.lightning.engine.core.match.PlayerService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory implementation of {@link PlayerService}.
 *
 * <p>This implementation provides business logic for player operations,
 * delegating persistence to {@link PlayerRepository}.
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>Player must not be null when creating</li>
 *   <li>Player must exist before deletion</li>
 * </ul>
 *
 * <p>SOLID compliance:
 * <ul>
 *   <li>SRP: Business logic only, persistence delegated to repository</li>
 *   <li>DIP: Depends on PlayerRepository abstraction</li>
 * </ul>
 */
@Slf4j
public class InMemoryPlayerService implements PlayerService {

    private final PlayerRepository playerRepository;

    public InMemoryPlayerService(PlayerRepository playerRepository) {
        this.playerRepository = Objects.requireNonNull(playerRepository, "playerRepository must not be null");
    }

    @Override
    public Player createPlayer(Player player) {
        Objects.requireNonNull(player, "player must not be null");
        log.info("Creating player with id: {}", player.id());
        return playerRepository.save(player);
    }

    @Override
    public void deletePlayer(long playerId) {
        log.info("Deleting player: {}", playerId);
        if (!playerRepository.existsById(playerId)) {
            throw new EntityNotFoundException(String.format("Player %d not found.", playerId));
        }
        playerRepository.deleteById(playerId);
    }

    @Override
    public Optional<Player> getPlayer(long playerId) {
        return playerRepository.findById(playerId);
    }

    @Override
    public Player getPlayerOrThrow(long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Player %d not found.", playerId)));
    }

    @Override
    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    @Override
    public boolean playerExists(long playerId) {
        return playerRepository.existsById(playerId);
    }
}
