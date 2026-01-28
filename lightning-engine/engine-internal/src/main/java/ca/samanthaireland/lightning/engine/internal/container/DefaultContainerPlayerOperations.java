/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.internal.container;

import ca.samanthaireland.lightning.engine.core.container.ContainerPlayerOperations;
import ca.samanthaireland.lightning.engine.core.container.ExecutionContainer;
import ca.samanthaireland.lightning.engine.core.match.Player;
import ca.samanthaireland.lightning.engine.core.match.PlayerRepository;
import ca.samanthaireland.lightning.engine.internal.core.match.InMemoryPlayerRepository;
import ca.samanthaireland.lightning.engine.internal.core.match.InMemoryPlayerService;

import java.util.List;
import java.util.Optional;

/**
 * Default implementation of ContainerPlayerOperations.
 *
 * <p>Each container has its own player store, providing complete
 * isolation between containers.</p>
 */
public final class DefaultContainerPlayerOperations implements ContainerPlayerOperations {

    private final ExecutionContainer container;
    private final InMemoryPlayerService playerService;

    public DefaultContainerPlayerOperations(ExecutionContainer container) {
        this.container = container;
        // Each container gets its own player repository for isolation
        PlayerRepository playerRepository = new InMemoryPlayerRepository();
        this.playerService = new InMemoryPlayerService(playerRepository);
    }

    @Override
    public Player create() {
        long playerId = System.currentTimeMillis() + (int) (Math.random() * 1000);
        return create(playerId);
    }

    @Override
    public Player create(long playerId) {
        return playerService.createPlayer(new Player(playerId));
    }

    @Override
    public Optional<Player> get(long playerId) {
        return playerService.getPlayer(playerId);
    }

    @Override
    public List<Player> all() {
        return playerService.getAllPlayers();
    }

    @Override
    public boolean delete(long playerId) {
        if (!playerService.playerExists(playerId)) {
            return false;
        }
        playerService.deletePlayer(playerId);
        return true;
    }

    @Override
    public boolean has(long playerId) {
        return playerService.playerExists(playerId);
    }

    @Override
    public int count() {
        return playerService.getAllPlayers().size();
    }
}
