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


package ca.samanthaireland.lightning.engine.core.command;

import java.util.List;

/**
 * Queue for scheduling commands to be executed.
 */
public interface CommandQueue {

    /**
     * Enqueue a command with its payload for execution.
     *
     * @param engineCommand the command to execute
     * @param payload the command payload
     */
    void enqueue(EngineCommand engineCommand, CommandPayload payload);

    /**
     * Get all errors that occurred during command execution since the last call.
     *
     * <p>Calling this method clears the error queue.
     *
     * @return list of command execution exceptions, never null
     */
    List<CommandExecutionException> getErrors();

    /**
     * Get the number of commands currently in the queue.
     *
     * @return the queue size
     */
    int getQueueSize();

    /**
     * Get the number of errors currently in the error queue.
     *
     * @return the error queue size
     */
    int getErrorQueueSize();

    /**
     * Clear all errors from the error queue.
     */
    void clearErrors();
}
