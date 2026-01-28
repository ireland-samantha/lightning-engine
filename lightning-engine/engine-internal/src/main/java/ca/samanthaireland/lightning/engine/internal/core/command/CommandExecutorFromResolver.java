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

import ca.samanthaireland.lightning.engine.core.command.CommandExecutor;
import ca.samanthaireland.lightning.engine.core.command.CommandPayload;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import lombok.extern.slf4j.Slf4j;

/**
 * CommandExecutor implementation that uses a CommandResolver to find commands.
 *
 * <p>This allows commands to be executed by name without requiring the command
 * map to be pre-built at construction time. Commands are resolved on-demand
 * from the loaded modules.
 */
@Slf4j
public class CommandExecutorFromResolver implements CommandExecutor {

    private final CommandResolver commandResolver;

    public CommandExecutorFromResolver(CommandResolver commandResolver) {
        this.commandResolver = commandResolver;
    }

    @Override
    public void executeCommand(String commandName, CommandPayload payload) {
        EngineCommand command = commandResolver.resolveByName(commandName);
        if (command == null) {
            log.warn("Command not found: {}", commandName);
            return;
        }

        try {
            command.executeCommand(payload);
        } catch (Exception e) {
            log.error("Error executing command '{}': {}", commandName, e.getMessage(), e);
        }
    }
}
