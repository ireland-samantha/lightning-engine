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
import java.util.Map;

public interface EngineCommand {
    /**
     * Get the unique name of this command.
     *
     * @return the command name
     */
    String getName();

    /**
     * Get the description of this command.
     *
     * @return the command description, or null if not provided
     */
    default String getDescription() {
        return null;
    }

    /**
     * Get the name of the module that provides this command.
     *
     * @return the module name, or null if not associated with a module
     */
    default String getModuleName() {
        return null;
    }

    /**
     * Get the parameter metadata for this command.
     *
     * @return list of parameter info, or null if not provided
     */
    default List<ParameterInfo> getParameters() {
        return null;
    }

    /**
     * Parameter metadata record.
     */
    record ParameterInfo(String name, String type, boolean required, String description) {}

    Map<String, Class<?>> schema();

    /**
     * Execute the command with the given payload.
     *
     * @param payload the command payload
     */
    void executeCommand(CommandPayload payload);
}
