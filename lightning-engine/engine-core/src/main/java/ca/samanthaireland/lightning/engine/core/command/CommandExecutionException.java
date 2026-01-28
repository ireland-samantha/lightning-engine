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

/**
 * Exception that wraps errors occurring during command execution.
 *
 * <p>Captures the command name, payload, and the underlying cause for debugging.
 */
public class CommandExecutionException extends RuntimeException {

    private final String commandName;
    private final CommandPayload payload;

    public CommandExecutionException(String commandName, CommandPayload payload, Throwable cause) {
        super("Failed to execute command: " + commandName, cause);
        this.commandName = commandName;
        this.payload = payload;
    }

    /**
     * Get the name of the command that failed.
     *
     * @return the command name
     */
    public String getCommandName() {
        return commandName;
    }

    /**
     * Get the payload that was passed to the command.
     *
     * @return the command payload, may be null
     */
    public CommandPayload getPayload() {
        return payload;
    }
}
