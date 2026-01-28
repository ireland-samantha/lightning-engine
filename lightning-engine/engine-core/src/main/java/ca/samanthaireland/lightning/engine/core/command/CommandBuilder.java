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

import java.util.Map;
import java.util.function.Consumer;

public interface CommandBuilder {
    static CommandBuilder newCommand() {
        return new DefaultCommandBuilder();
    }

    CommandBuilder withName(String name);
    CommandBuilder withSchema(Map<String, Class<?>> schema);
    CommandBuilder withExecution(Consumer<CommandPayload> execution);
    EngineCommand build();


    class DefaultCommandBuilder implements CommandBuilder {

        private String name;
        private Map<String, Class<?>> schema;
        private Consumer<CommandPayload> execution;

        @Override
        public CommandBuilder withName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public CommandBuilder withSchema(Map<String, Class<?>> schema) {
            this.schema = schema;
            return this;
        }

        @Override
        public CommandBuilder withExecution(Consumer<CommandPayload> execution) {
            this.execution = execution;
            return this;
        }

        @Override
        public EngineCommand build() {
            return new EngineCommand() {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public Map<String, Class<?>> schema() {
                    return schema;
                }

                @Override
                public void executeCommand(CommandPayload payload) {
                    execution.accept(payload);
                }
            };
        }
    }
}
