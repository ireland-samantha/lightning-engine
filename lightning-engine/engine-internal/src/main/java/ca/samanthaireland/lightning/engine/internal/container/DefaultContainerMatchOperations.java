/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */
package ca.samanthaireland.lightning.engine.internal.container;

import ca.samanthaireland.lightning.engine.core.container.ContainerMatchOperations;
import ca.samanthaireland.lightning.engine.core.match.Match;

import java.util.List;
import java.util.Optional;

/**
 * Default implementation of ContainerMatchOperations.
 * Works directly with InMemoryExecutionContainer for match management.
 */
public final class DefaultContainerMatchOperations implements ContainerMatchOperations {

    private final InMemoryExecutionContainer container;

    public DefaultContainerMatchOperations(InMemoryExecutionContainer container) {
        this.container = container;
    }

    @Override
    public Match create(Match match) {
        return container.createMatchInternal(match);
    }

    @Override
    public Optional<Match> get(long matchId) {
        return container.getMatchInternal(matchId);
    }

    @Override
    public List<Match> all() {
        return container.getAllMatchesInternal();
    }

    @Override
    public ContainerMatchOperations delete(long matchId) {
        container.deleteMatchInternal(matchId);
        return this;
    }
}
