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

package ca.samanthaireland.lightning.engine.api.resource.adapter;

import java.io.IOException;
import java.util.Map;

/**
 * Adapter interface for simulation control operations.
 *
 * <p>Handles tick advancement, auto-play control, and command submission.
 */
public interface SimulationAdapter {

    /**
     * Advance the container's tick.
     *
     * @return the new tick value
     */
    long tick() throws IOException;

    /**
     * Get the container's current tick.
     *
     * @return the current tick value
     */
    long currentTick() throws IOException;

    /**
     * Start auto-advancing ticks at the specified interval.
     *
     * @param intervalMs the interval in milliseconds between ticks
     */
    void play(int intervalMs) throws IOException;

    /**
     * Stop auto-advancing ticks.
     */
    void stopAuto() throws IOException;

    /**
     * Submit a command in this container.
     *
     * @param commandName the command name
     * @param parameters the command parameters
     */
    void submitCommand(String commandName, Map<String, Object> parameters) throws IOException;
}
