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

import ca.samanthaireland.lightning.engine.core.GameSimulation;
import ca.samanthaireland.lightning.engine.core.command.CommandPayload;
import ca.samanthaireland.lightning.engine.core.command.CommandQueue;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.exception.EntityNotFoundException;
import ca.samanthaireland.lightning.engine.core.match.Match;
import ca.samanthaireland.lightning.engine.core.match.MatchService;
import ca.samanthaireland.lightning.engine.core.match.Player;
import ca.samanthaireland.lightning.engine.core.match.PlayerService;
import ca.samanthaireland.lightning.engine.core.session.PlayerSession;
import ca.samanthaireland.lightning.engine.core.session.PlayerSessionService;
import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;
import ca.samanthaireland.lightning.engine.internal.GameLoop;
import ca.samanthaireland.lightning.engine.internal.core.command.CommandResolver;
import ca.samanthaireland.lightning.engine.internal.core.snapshot.SnapshotProvider;
import ca.samanthaireland.lightning.engine.internal.ext.module.ModuleManager;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of {@link GameSimulation}.
 *
 * <p>This implementation delegates match, player, and player-match operations
 * to their respective services and manages the simulation tick counter.
 *
 * <p>SOLID compliance:
 * <ul>
 *   <li>SRP: Coordinates between services, doesn't implement business logic</li>
 *   <li>DIP: Depends on service abstractions, not implementations</li>
 * </ul>
 */
@Slf4j
public class InMemoryGameSimulation implements GameSimulation {

    private final MatchService matchService;
    private final PlayerService playerService;
    private final PlayerSessionService sessionService;
    private final ModuleManager moduleManager;
    private final CommandResolver commandResolver;
    private final CommandQueue commandQueue;
    private final AtomicLong currentTick = new AtomicLong(0);
    private final SnapshotProvider snapshotProvider;
    private final GameLoop gameLoop;

    // Auto-advance tick scheduling
    private final ScheduledExecutorService autoAdvanceExecutor;
    private final AtomicBoolean autoAdvancing = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> autoAdvanceTask;

    public InMemoryGameSimulation(
            MatchService matchService,
            PlayerService playerService,
            PlayerSessionService sessionService,
            ModuleManager moduleManager,
            CommandResolver commandResolver,
            CommandQueue commandQueue,
            SnapshotProvider snapshotProvider,
            GameLoop gameLoop) {
        this.matchService = Objects.requireNonNull(matchService, "matchService must not be null");
        this.playerService = Objects.requireNonNull(playerService, "playerService must not be null");
        this.sessionService = Objects.requireNonNull(sessionService, "sessionService must not be null");
        this.moduleManager = moduleManager;
        this.commandResolver = commandResolver;
        this.commandQueue = commandQueue;
        this.snapshotProvider = snapshotProvider;
        this.gameLoop = gameLoop;
        this.autoAdvanceExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "auto-advance-tick");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public long getCurrentTick() {
        return currentTick.get();
    }

    @Override
    public long advanceTick() {
        long tick = currentTick.incrementAndGet();
        gameLoop.advanceTick(tick);
        return tick;
    }

    @Override
    public void startAutoAdvance(long intervalMs) {
        if (autoAdvancing.compareAndSet(false, true)) {
            log.info("Starting auto-advance with interval {}ms", intervalMs);
            autoAdvanceTask = autoAdvanceExecutor.scheduleAtFixedRate(
                    this::advanceTick,
                    intervalMs,
                    intervalMs,
                    TimeUnit.MILLISECONDS
            );
        } else {
            log.debug("Auto-advance already running");
        }
    }

    @Override
    public void stopAutoAdvance() {
        if (autoAdvancing.compareAndSet(true, false)) {
            log.info("Stopping auto-advance at tick {}", currentTick.get());
            if (autoAdvanceTask != null) {
                autoAdvanceTask.cancel(false);
                autoAdvanceTask = null;
            }
        }
    }

    @Override
    public boolean isAutoAdvancing() {
        return autoAdvancing.get();
    }

    @Override
    public void installModule(Class<? extends ModuleFactory> factoryClass) {
        log.info("Installing module from class: {}", factoryClass.getName());
        if (moduleManager != null) {
            moduleManager.installModule(factoryClass);
        }
    }

    @Override
    public void installModule(String jarPath) {
        log.info("Installing module from: {}", jarPath);
        if (moduleManager != null) {
            try {
                moduleManager.installModule(Paths.get(jarPath));
            } catch (Exception e) {
                log.error("Failed to install module from: {}", jarPath, e);
            }
        }
    }

    @Override
    public void enqueueCommand(String commandName, CommandPayload payload) {
        Objects.requireNonNull(commandResolver, "commandResolver must not be null");
        Objects.requireNonNull(commandQueue, "commandQueue must not be null");

        EngineCommand command = commandResolver.resolveByName(commandName);
        if (command == null) {
            throw new EntityNotFoundException("Command not found: " + commandName);
        }

        log.debug("Enqueuing command: {}", commandName);
        commandQueue.enqueue(command, payload);
    }

    // Match operations

    @Override
    public Match createMatch(Match match) {
        log.debug("Creating match: {}", match.id());
        return matchService.createMatch(match);
    }

    @Override
    public Optional<Match> getMatch(long matchId) {
        return matchService.getMatch(matchId);
    }

    @Override
    public List<Match> getAllMatches() {
        return matchService.getAllMatches();
    }

    @Override
    public void deleteMatch(long matchId) {
        log.debug("Deleting match: {}", matchId);
        if (matchService.matchExists(matchId)) {
            matchService.deleteMatch(matchId);
        }
    }

    // Player operations

    @Override
    public void createPlayer(Player player) {
        log.debug("Creating player: {}", player.id());
        playerService.createPlayer(player);
    }

    @Override
    public Optional<Player> getPlayer(long playerId) {
        return playerService.getPlayer(playerId);
    }

    @Override
    public List<Player> getAllPlayers() {
        return playerService.getAllPlayers();
    }

    @Override
    public void deletePlayer(long playerId) {
        log.debug("Deleting player: {}", playerId);
        if (playerService.playerExists(playerId)) {
            playerService.deletePlayer(playerId);
        }
    }

    // Session operations (replaces PlayerMatch)

    @Override
    public PlayerSession joinMatch(long playerId, long matchId) {
        log.debug("Player {} joining match {}", playerId, matchId);
        return sessionService.createSession(playerId, matchId);
    }

    @Override
    public Optional<PlayerSession> getSession(long playerId, long matchId) {
        return sessionService.findSession(playerId, matchId);
    }

    @Override
    public List<PlayerSession> getSessionsByMatch(long matchId) {
        return sessionService.findMatchSessions(matchId);
    }

    @Override
    public List<PlayerSession> getSessionsByPlayer(long playerId) {
        return sessionService.findAllSessions().stream()
                .filter(s -> s.playerId() == playerId)
                .toList();
    }

    @Override
    public void leaveMatch(long playerId, long matchId) {
        log.debug("Player {} leaving match {}", playerId, matchId);
        sessionService.findSession(playerId, matchId)
                .ifPresent(session -> sessionService.abandon(playerId, matchId));
    }
}
