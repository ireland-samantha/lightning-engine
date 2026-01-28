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

import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.ext.module.EngineModule;
import ca.samanthaireland.lightning.engine.ext.module.ModuleResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModuleCommandResolver")
class ModuleCommandResolverTest {

    @Mock
    private ModuleResolver moduleResolver;

    private ModuleCommandResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ModuleCommandResolver(moduleResolver);
    }

    @Nested
    @DisplayName("resolveByName")
    class ResolveByName {

        @Test
        @DisplayName("should return null for null name without populating cache")
        void shouldReturnNullForNullNameWithoutPopulatingCache() {
            EngineCommand result = resolver.resolveByName(null);

            assertThat(result).isNull();
            verify(moduleResolver, times(0)).resolveAllModules();
        }

        @Test
        @DisplayName("should return command when found")
        void shouldReturnCommandWhenFound() {
            EngineCommand mockCommand = createMockCommand("testCmd");
            EngineModule mockModule = createMockModule(List.of(mockCommand));
            when(moduleResolver.resolveAllModules()).thenReturn(List.of(mockModule));

            EngineCommand result = resolver.resolveByName("testCmd");

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("testCmd");
        }

        @Test
        @DisplayName("should return null when command not found")
        void shouldReturnNullWhenCommandNotFound() {
            when(moduleResolver.resolveAllModules()).thenReturn(List.of());

            EngineCommand result = resolver.resolveByName("nonexistent");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should use cached command on subsequent calls")
        void shouldUseCachedCommandOnSubsequentCalls() {
            EngineCommand mockCommand = createMockCommand("testCmd");
            EngineModule mockModule = createMockModule(List.of(mockCommand));
            when(moduleResolver.resolveAllModules()).thenReturn(List.of(mockModule));

            resolver.resolveByName("testCmd");
            resolver.resolveByName("testCmd");
            resolver.resolveByName("testCmd");

            verify(moduleResolver, times(1)).resolveAllModules();
        }

        @Test
        @DisplayName("should handle module returning null commands")
        void shouldHandleModuleReturningNullCommands() {
            EngineModule mockModule = mock(EngineModule.class);
            when(mockModule.createCommands()).thenReturn(null);
            when(moduleResolver.resolveAllModules()).thenReturn(List.of(mockModule));

            EngineCommand result = resolver.resolveByName("anyCmd");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should handle command with null name")
        void shouldHandleCommandWithNullName() {
            EngineCommand mockCommand = mock(EngineCommand.class);
            when(mockCommand.getName()).thenReturn(null);
            EngineModule mockModule = createMockModule(List.of(mockCommand));
            when(moduleResolver.resolveAllModules()).thenReturn(List.of(mockModule));

            EngineCommand result = resolver.resolveByName("anyCmd");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("should return all commands")
        void shouldReturnAllCommands() {
            EngineCommand cmd1 = createMockCommand("cmd1");
            EngineCommand cmd2 = createMockCommand("cmd2");
            EngineModule mockModule = createMockModule(List.of(cmd1, cmd2));
            when(moduleResolver.resolveAllModules()).thenReturn(List.of(mockModule));

            List<EngineCommand> result = resolver.getAll();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no commands")
        void shouldReturnEmptyListWhenNoCommands() {
            when(moduleResolver.resolveAllModules()).thenReturn(List.of());

            List<EngineCommand> result = resolver.getAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAvailableCommandNames")
    class GetAvailableCommandNames {

        @Test
        @DisplayName("should return all command names")
        void shouldReturnAllCommandNames() {
            EngineCommand cmd1 = createMockCommand("spawn");
            EngineCommand cmd2 = createMockCommand("move");
            EngineModule mockModule = createMockModule(List.of(cmd1, cmd2));
            when(moduleResolver.resolveAllModules()).thenReturn(List.of(mockModule));

            List<String> result = resolver.getAvailableCommandNames();

            assertThat(result).containsExactlyInAnyOrder("spawn", "move");
        }

        @Test
        @DisplayName("should return empty list when no commands")
        void shouldReturnEmptyListWhenNoCommands() {
            when(moduleResolver.resolveAllModules()).thenReturn(List.of());

            List<String> result = resolver.getAvailableCommandNames();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasCommand")
    class HasCommand {

        @Test
        @DisplayName("should return true when command exists")
        void shouldReturnTrueWhenCommandExists() {
            EngineCommand mockCommand = createMockCommand("testCmd");
            EngineModule mockModule = createMockModule(List.of(mockCommand));
            when(moduleResolver.resolveAllModules()).thenReturn(List.of(mockModule));

            boolean result = resolver.hasCommand("testCmd");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when command does not exist")
        void shouldReturnFalseWhenCommandDoesNotExist() {
            when(moduleResolver.resolveAllModules()).thenReturn(List.of());

            boolean result = resolver.hasCommand("nonexistent");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("invalidateCache")
    class InvalidateCache {

        @Test
        @DisplayName("should clear cache and repopulate on next access")
        void shouldClearCacheAndRepopulateOnNextAccess() {
            EngineCommand mockCommand = createMockCommand("cmd");
            EngineModule mockModule = createMockModule(List.of(mockCommand));
            when(moduleResolver.resolveAllModules()).thenReturn(List.of(mockModule));

            resolver.resolveByName("cmd");
            resolver.invalidateCache();
            resolver.resolveByName("cmd");

            verify(moduleResolver, times(2)).resolveAllModules();
        }

        @Test
        @DisplayName("should allow multiple invalidations")
        void shouldAllowMultipleInvalidations() {
            when(moduleResolver.resolveAllModules()).thenReturn(List.of());

            resolver.invalidateCache();
            resolver.invalidateCache();
            resolver.invalidateCache();

            assertThat(resolver.getAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("schema")
    class Schema {

        @Test
        @DisplayName("should return empty map for nonexistent command")
        void shouldReturnEmptyMapForNonexistentCommand() {
            when(moduleResolver.resolveAllModules()).thenReturn(List.of());

            Map<String, Class<?>> result = resolver.schema(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("duplicate commands")
    class DuplicateCommands {

        @Test
        @DisplayName("should overwrite command when duplicate name found")
        void shouldOverwriteCommandWhenDuplicateNameFound() {
            EngineCommand cmd1 = createMockCommand("duplicate");
            EngineCommand cmd2 = createMockCommand("duplicate");
            EngineModule module1 = createMockModule(List.of(cmd1));
            EngineModule module2 = createMockModule(List.of(cmd2));
            when(moduleResolver.resolveAllModules()).thenReturn(List.of(module1, module2));

            EngineCommand result = resolver.resolveByName("duplicate");

            assertThat(result).isNotNull();
            assertThat(result).isSameAs(cmd2);
        }
    }

    @Nested
    @DisplayName("multiple modules")
    class MultipleModules {

        @Test
        @DisplayName("should aggregate commands from all modules")
        void shouldAggregateCommandsFromAllModules() {
            EngineCommand cmd1 = createMockCommand("cmd1");
            EngineCommand cmd2 = createMockCommand("cmd2");
            EngineCommand cmd3 = createMockCommand("cmd3");
            EngineModule module1 = createMockModule(List.of(cmd1));
            EngineModule module2 = createMockModule(List.of(cmd2, cmd3));
            when(moduleResolver.resolveAllModules()).thenReturn(List.of(module1, module2));

            List<EngineCommand> result = resolver.getAll();

            assertThat(result).hasSize(3);
        }
    }

    private static EngineCommand createMockCommand(String name) {
        EngineCommand command = mock(EngineCommand.class);
        when(command.getName()).thenReturn(name);
        return command;
    }

    private static EngineModule createMockModule(List<EngineCommand> commands) {
        EngineModule module = mock(EngineModule.class);
        when(module.createCommands()).thenReturn(commands);
        return module;
    }
}
