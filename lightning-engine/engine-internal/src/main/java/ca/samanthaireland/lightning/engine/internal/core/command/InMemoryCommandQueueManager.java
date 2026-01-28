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


package ca.samanthaireland.lightning.engine.internal.core.command;

import ca.samanthaireland.lightning.engine.core.command.CommandExecutionException;
import ca.samanthaireland.lightning.engine.core.command.CommandPayload;
import ca.samanthaireland.lightning.engine.core.command.CommandQueue;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.internal.CommandExecutionMetrics;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread-safe in-memory implementation of {@link CommandQueue} and {@link CommandQueueExecutor}.
 *
 * <p>Commands are enqueued when scheduled and dequeued when executed.
 * Errors during execution are captured and can be retrieved via {@link #getErrors()}.
 */
@Slf4j
public class InMemoryCommandQueueManager implements CommandQueue, CommandQueueExecutor {

    private final ConcurrentLinkedQueue<ScheduledCommand> commandQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<CommandExecutionException> errorQueue = new ConcurrentLinkedQueue<>();

    // Per-command execution metrics for the last tick
    private volatile List<CommandExecutionMetrics> lastTickCommandMetrics = Collections.emptyList();

    @Override
    public void enqueue(EngineCommand command, CommandPayload payload) {
        if (command == null) {
            log.warn("Attempted to schedule null command");
            return;
        }
        commandQueue.add(new ScheduledCommand(command, payload));
        log.debug("Scheduled command: {}", command.getName());
    }

    @Override
    public void executeCommands(int amount) {
        log.trace("Execute {} commands.", amount);
        if (amount <= 0) {
            lastTickCommandMetrics = Collections.emptyList();
            return;
        }

        List<CommandExecutionMetrics> metrics = new ArrayList<>();
        int executed = 0;
        while (executed < amount) {
            ScheduledCommand scheduled = commandQueue.poll();
            if (scheduled == null) {
                break;
            }

            long startTime = System.nanoTime();
            boolean success = false;
            try {
                scheduled.command().executeCommand(scheduled.payload());
                log.debug("Executed command: {}", scheduled.command().getName());
                success = true;
                executed++;
            } catch (Exception e) {
                log.error("Failed to execute command: {}", scheduled.command().getName(), e);
                errorQueue.add(new CommandExecutionException(
                        scheduled.command().getName(),
                        scheduled.payload(),
                        e
                ));
            }
            long duration = System.nanoTime() - startTime;
            metrics.add(new CommandExecutionMetrics(scheduled.command().getName(), duration, success));
        }

        lastTickCommandMetrics = metrics;
        log.debug("Executed {} commands", executed);
    }

    @Override
    public List<CommandExecutionException> getErrors() {
        List<CommandExecutionException> errors = new ArrayList<>();
        CommandExecutionException error;
        while ((error = errorQueue.poll()) != null) {
            errors.add(error);
        }
        return errors;
    }

    /**
     * Get the number of commands currently in the queue.
     *
     * @return the queue size
     */
    public int getQueueSize() {
        return commandQueue.size();
    }

    /**
     * Get the number of errors currently in the error queue.
     *
     * @return the error queue size
     */
    public int getErrorQueueSize() {
        return errorQueue.size();
    }

    /**
     * Clear all pending commands from the queue.
     */
    public void clear() {
        commandQueue.clear();
        log.debug("Command queue cleared");
    }

    /**
     * Clear all errors from the error queue.
     */
    public void clearErrors() {
        errorQueue.clear();
        log.debug("Error queue cleared");
    }

    /**
     * Get the command execution metrics from the last tick.
     *
     * @return list of command execution metrics
     */
    public List<CommandExecutionMetrics> getLastTickCommandMetrics() {
        return lastTickCommandMetrics;
    }

    /**
     * A scheduled command with its payload.
     */
    private record ScheduledCommand(EngineCommand command, CommandPayload payload) {
    }
}
