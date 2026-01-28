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


package ca.samanthaireland.lightning.engine.internal.ext.ai;

import ca.samanthaireland.game.domain.AI;
import ca.samanthaireland.game.backend.installation.AIFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Manages AI from external sources (e.g., JAR files).
 *
 * <p>Provides functionality to scan for AI, install new AI,
 * and resolve AI by name.
 */
public interface AIManager {

    /**
     * Scan for available AI.
     *
     * <p>This method scans the configured location for AI implementations
     * and registers them for later resolution.
     *
     * @throws IOException if the scan fails
     */
    void reloadInstalled() throws IOException;

    /**
     * Install an AI from a file.
     *
     * <p>This method installs the AI from the specified file,
     * making it available for resolution.
     *
     * @param aiFile the path to the AI JAR file to install
     * @throws IOException if the installation fails
     */
    void installAI(Path aiFile) throws IOException;

    /**
     * Install an AI from a factory class.
     *
     * <p>This method instantiates the factory class and registers the AI
     * for later resolution. Useful for programmatic registration
     * without requiring JAR files.
     *
     * @param aiFactory the AI factory class to install
     */
    void installAI(Class<? extends AIFactory> aiFactory);

    /**
     * Clear all caches and reset the scanned state.
     *
     * <p>Useful for hot-reloading AI.
     */
    void reset();

    /**
     * Get an AI factory by name.
     *
     * @param aiName the name of the AI to get
     * @return the factory, or null if not found
     */
    AIFactory getFactory(String aiName);

    /**
     * Create an AI instance for a specific match.
     *
     * <p>This creates a new instance of the AI bound to the specified match,
     * providing proper isolation between matches.
     *
     * @param aiName the name of the AI
     * @param matchId the match ID for isolation
     * @return the created AI instance, or null if not found
     */
    AI createForMatch(String aiName, long matchId);

    /**
     * Get all available AI names.
     *
     * @return list of available AI names
     */
    List<String> getAvailableAIs();

    /**
     * Check if an AI with the given name is available.
     *
     * @param aiName the AI name to check
     * @return true if the AI is available
     */
    boolean hasAI(String aiName);

    /**
     * Uninstall an AI by name.
     *
     * <p>This removes the AI from the cache. Note that this does not
     * delete the JAR file from disk.
     *
     * @param aiName the name of the AI to uninstall
     * @return true if the AI was found and uninstalled
     */
    boolean uninstallAI(String aiName);
}
