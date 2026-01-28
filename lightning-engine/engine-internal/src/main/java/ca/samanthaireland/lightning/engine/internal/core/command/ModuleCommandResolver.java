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
import ca.samanthaireland.lightning.engine.internal.ext.module.ModuleManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Resolves commands by name from modules managed by a {@link ModuleManager}.
 *
 * <p>This implementation caches commands for performance and provides
 * methods to invalidate the cache when modules are added or removed.
 */
@Slf4j
public class ModuleCommandResolver implements CommandResolver {

    private final ModuleResolver moduleResolver;
    private final Map<String, EngineCommand> commandCache = new ConcurrentHashMap<>();
    private final AtomicBoolean cachePopulated = new AtomicBoolean(false);

    public ModuleCommandResolver(ModuleResolver moduleResolver) {
        this.moduleResolver = moduleResolver;
    }

    @Override
    public EngineCommand resolveByName(String name) {
        if (name == null) {
            return null;
        }

        ensureCachePopulated();
        return commandCache.get(name);
    }

    @Override
    public List<EngineCommand> getAll() {
        ensureCachePopulated();
        return commandCache.values().stream().toList();
    }

    public Map<String, Class<?>> schema(Long commandId) {
        ensureCachePopulated();
        return Optional.ofNullable(commandCache.get(commandId))
                .map(v->v.schema())
                .orElse(Map.of());
    }

    /**
     * Invalidate the command cache.
     *
     * <p>This should be called when modules are added or removed
     * to ensure the cache reflects the current state.
     */
    public void invalidateCache() {
        commandCache.clear();
        cachePopulated.set(false);
        log.debug("Command cache invalidated");
    }

    /**
     * Get all cached command names.
     *
     * @return list of command names
     */
    public List<String> getAvailableCommandNames() {
        ensureCachePopulated();
        return List.copyOf(commandCache.keySet());
    }

    /**
     * Check if a command with the given name is available.
     *
     * @param name the command name
     * @return true if the command exists
     */
    public boolean hasCommand(String name) {
        ensureCachePopulated();
        return commandCache.containsKey(name);
    }

    private void ensureCachePopulated() {
        if (cachePopulated.compareAndSet(false, true)) {
            populateCache();
        }
    }

    private void populateCache() {
        log.debug("Populating command cache from modules");

        List<EngineModule> modules = moduleResolver.resolveAllModules();
        for (EngineModule module : modules) {
            List<EngineCommand> commands = module.createCommands();
            if (commands != null) {
                for (EngineCommand command : commands) {
                    String commandName = command.getName();
                    if (commandName != null) {
                        if (commandCache.containsKey(commandName)) {
                            log.warn("Duplicate command name '{}' found. Overwriting.", commandName);
                        }
                        commandCache.put(commandName, command);
                        log.debug("Cached command: {}", commandName);
                    }
                }
            }
        }

        log.info("Command cache populated with {} commands", commandCache.size());
    }
}
