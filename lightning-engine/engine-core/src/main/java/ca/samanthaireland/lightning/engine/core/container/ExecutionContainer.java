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


package ca.samanthaireland.lightning.engine.core.container;

import ca.samanthaireland.lightning.engine.core.command.CommandPayload;
import ca.samanthaireland.lightning.engine.core.match.Match;
import ca.samanthaireland.lightning.engine.ext.module.ModuleFactory;

import java.util.List;
import java.util.Optional;

/**
 * An execution container provides complete runtime isolation for a set of matches.
 * Each container has its own:
 * <ul>
 *     <li>Classloader - Module JARs are loaded in isolation</li>
 *     <li>Entity Component Store - Separate ECS data</li>
 *     <li>Game Loop - Independent tick execution</li>
 *     <li>Command Queue - Container-scoped command processing</li>
 * </ul>
 *
 * <p>Containers run completely independently and can have different tick rates.
 * One container can contain multiple matches that share the same module classes
 * and execute within the same tick loop.</p>
 *
 * Note:Do not add any non fluent methods to this class.
 */
public interface ExecutionContainer {

    // =========================================================================
    // IDENTITY
    // =========================================================================

    /**
     * Returns the unique identifier for this container.
     *
     * @return the container ID
     */
    long getId();

    /**
     * Returns the human-readable name for this container.
     *
     * @return the container name
     */
    String getName();

    /**
     * Returns the configuration used to create this container.
     *
     * @return the container configuration
     */
    ContainerConfig getConfig();

    /**
     * Returns the current lifecycle status of this container.
     *
     * @return the current status
     */
    ContainerStatus getStatus();

    // =========================================================================
    // FLUENT API
    // =========================================================================

    /**
     * Returns a fluent API for module operations.
     *
     * @return the module operations interface
     */
    ContainerModuleOperations modules();

    /**
     * Returns a fluent API for AI operations.
     *
     * @return the AI operations interface
     */
    ContainerAIOperations ai();

    /**
     * Returns a fluent API for resource operations.
     *
     * @return the resource operations interface
     */
    ContainerResourceOperations resources();

    /**
     * Returns a fluent API for lifecycle operations.
     *
     * <p>Example usage:
     * <pre>{@code
     * container.lifecycle()
     *     .start()
     *     .thenPlay(16);  // Start auto-advancing at 60 FPS
     * }</pre>
     *
     * @return the lifecycle operations interface
     */
    ContainerLifecycleOperations lifecycle();

    /**
     * Returns a fluent API for tick operations.
     *
     * <p>Example usage:
     * <pre>{@code
     * container.ticks()
     *     .advance()
     *     .advance()
     *     .play(16);
     * }</pre>
     *
     * @return the tick operations interface
     */
    ContainerTickOperations ticks();

    /**
     * Returns a fluent API for command operations.
     *
     * <p>Example usage:
     * <pre>{@code
     * container.commands()
     *     .named("SpawnEntity")
     *     .forMatch(1)
     *     .param("type", 100)
     *     .execute();
     * }</pre>
     *
     * @return the command operations interface
     */
    ContainerCommandOperations commands();

    /**
     * Returns a fluent API for match operations.
     *
     * <p>Example usage:
     * <pre>{@code
     * // Create a match
     * Match match = container.matches().create(matchConfig);
     *
     * // Get all matches
     * container.matches().all();
     * }</pre>
     *
     * @return the match operations interface
     */
    ContainerMatchOperations matches();

    /**
     * Returns a fluent API for snapshot operations.
     *
     * <p>Example usage:
     * <pre>{@code
     * // Get snapshot for a match
     * Snapshot snapshot = container.snapshots().forMatch(matchId);
     *
     * // Get player-scoped snapshot
     * Snapshot playerSnapshot = container.snapshots().forMatchAndPlayer(matchId, playerId);
     * }</pre>
     *
     * @return the snapshot operations interface, or null if container not started
     */
    ContainerSnapshotOperations snapshots();

    /**
     * Returns a fluent API for player operations.
     *
     * <p>Players are container-scoped - a player created in one container
     * is not visible to other containers.</p>
     *
     * <p>Example usage:
     * <pre>{@code
     * // Create a player
     * Player player = container.players().create();
     *
     * // Get all players in this container
     * List<Player> all = container.players().all();
     * }</pre>
     *
     * @return the player operations interface
     */
    ContainerPlayerOperations players();

    /**
     * Returns a fluent API for session operations.
     *
     * <p>Sessions are container-scoped - a session created in one container
     * is not visible to other containers.</p>
     *
     * <p>Example usage:
     * <pre>{@code
     * // Create a session (player joins match)
     * PlayerSession session = container.sessions().create(playerId, matchId);
     *
     * // Get all sessions for a match
     * List<PlayerSession> sessions = container.sessions().forMatch(matchId);
     *
     * // Disconnect a player
     * container.sessions().disconnect(playerId, matchId);
     * }</pre>
     *
     * @return the session operations interface
     */
    ContainerSessionOperations sessions();


    // Note for Claude: Do not add any non-fluent methods to this class.
    /**
     * Creates a fluent builder for match creation.
     *
     * <p>Example usage:
     * <pre>{@code
     * Match match = container.match()
     *     .withModules("EntityModule", "RigidBodyModule")
     *     .withAI("TickCounter")
     *     .create();
     * }</pre>
     *
     * @return a new match builder
     */
    default MatchBuilder match() {
        return new MatchBuilder(this);
    }

    List<CommandInfo> getAvailableCommands();

    /**
     * Command metadata record.
     */
    record CommandInfo(String name, String description, String module, List<ParameterInfo> parameters) {}

    /**
     * Command parameter metadata record.
     */
    record ParameterInfo(String name, String type, boolean required, String description) {}

    // =========================================================================
    // STATISTICS
    // =========================================================================

    /**
     * Returns container statistics including entity counts and memory usage.
     *
     * @return the container statistics
     */
    ContainerStats getStats();

    /**
     * Container statistics record.
     *
     * @param entityCount Current number of active entities
     * @param maxEntities Maximum entity capacity
     * @param usedMemoryBytes Estimated memory used by the container (ECS store)
     * @param maxMemoryBytes Maximum configured memory (0 = unlimited)
     * @param jvmMaxMemoryBytes JVM maximum heap size
     * @param jvmUsedMemoryBytes JVM currently used heap
     * @param matchCount Number of matches in the container
     * @param moduleCount Number of loaded modules
     */
    record ContainerStats(
            int entityCount,
            int maxEntities,
            long usedMemoryBytes,
            long maxMemoryBytes,
            long jvmMaxMemoryBytes,
            long jvmUsedMemoryBytes,
            int matchCount,
            int moduleCount
    ) {}
}
