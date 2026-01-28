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


package ca.samanthaireland.lightning.engine.quarkus.api.error;

import ca.samanthaireland.lightning.engine.core.command.CommandExecutionException;
import ca.samanthaireland.lightning.engine.core.command.CommandPayload;
import ca.samanthaireland.lightning.engine.core.command.CommandQueue;
import ca.samanthaireland.lightning.engine.core.error.ErrorBroadcaster;
import ca.samanthaireland.lightning.engine.core.error.GameError;
import ca.samanthaireland.lightning.engine.internal.TickListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/**
 * Tick listener that collects errors from the command queue and broadcasts them.
 *
 * <p>After each tick, this listener:
 * <ol>
 *   <li>Retrieves all command execution errors from the queue</li>
 *   <li>Converts them to GameError objects</li>
 *   <li>Publishes them via the ErrorBroadcaster</li>
 * </ol>
 */
public class ErrorCollectorTickListener implements TickListener {
    private static final Logger log = LoggerFactory.getLogger(ErrorCollectorTickListener.class);

    private final CommandQueue commandQueue;
    private final ErrorBroadcaster errorBroadcaster;

    public ErrorCollectorTickListener(CommandQueue commandQueue, ErrorBroadcaster errorBroadcaster) {
        this.commandQueue = commandQueue;
        this.errorBroadcaster = errorBroadcaster;
    }

    @Override
    public void onTickComplete(long tick) {
        List<CommandExecutionException> errors = commandQueue.getErrors();

        if (errors.isEmpty()) {
            return;
        }

        log.debug("Tick {}: collecting {} command errors", tick, errors.size());

        for (CommandExecutionException error : errors) {
            GameError gameError = convertToGameError(error);
            errorBroadcaster.publish(gameError);
        }
    }

    private GameError convertToGameError(CommandExecutionException error) {
        long matchId = extractMatchId(error.getPayload());
        long playerId = extractPlayerId(error.getPayload());

        return GameError.commandError(
                matchId,
                playerId,
                error.getCommandName(),
                error.getCause() != null ? error.getCause().getMessage() : "Command execution failed",
                getStackTrace(error.getCause())
        );
    }

    private long extractMatchId(CommandPayload payload) {
        if (payload != null && payload.getPayload() instanceof Map<?, ?> map) {
            Object matchId = map.get("matchId");
            if (matchId instanceof Number n) {
                return n.longValue();
            }
        }
        return 0;
    }

    private long extractPlayerId(CommandPayload payload) {
        if (payload != null && payload.getPayload() instanceof Map<?, ?> map) {
            Object playerId = map.get("playerId");
            if (playerId instanceof Number n) {
                return n.longValue();
            }
        }
        return 0;
    }

    private String getStackTrace(Throwable t) {
        if (t == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        // Limit stack trace length
        String trace = sw.toString();
        if (trace.length() > 2000) {
            trace = trace.substring(0, 2000) + "...[truncated]";
        }
        return trace;
    }
}
