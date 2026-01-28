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


package ca.samanthaireland.lightning.engine.core.error;

import java.time.Instant;

/**
 * Represents an error that occurred during game execution.
 *
 * @param id unique error identifier
 * @param timestamp when the error occurred
 * @param matchId the match ID (0 if not match-specific)
 * @param playerId the player ID (0 if not player-specific)
 * @param type the error type (COMMAND, SYSTEM, GENERAL)
 * @param source the source of the error (command name, system class, etc.)
 * @param message the error message
 * @param details additional details (stack trace, etc.)
 */
public record GameError(
        String id,
        Instant timestamp,
        long matchId,
        long playerId,
        ErrorType type,
        String source,
        String message,
        String details
) {
    public enum ErrorType {
        COMMAND,
        SYSTEM,
        GENERAL
    }

    public static GameError commandError(long matchId, long playerId, String commandName, String message, String details) {
        return new GameError(
                generateId(),
                Instant.now(),
                matchId,
                playerId,
                ErrorType.COMMAND,
                commandName,
                message,
                details
        );
    }

    public static GameError systemError(long matchId, String systemName, String message, String details) {
        return new GameError(
                generateId(),
                Instant.now(),
                matchId,
                0,
                ErrorType.SYSTEM,
                systemName,
                message,
                details
        );
    }

    public static GameError generalError(String source, String message, String details) {
        return new GameError(
                generateId(),
                Instant.now(),
                0,
                0,
                ErrorType.GENERAL,
                source,
                message,
                details
        );
    }

    private static String generateId() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}
