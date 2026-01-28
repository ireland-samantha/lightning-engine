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


package ca.samanthaireland.lightning.engine.internal.core.store;

import ca.samanthaireland.lightning.engine.core.store.PermissionComponent;
import ca.samanthaireland.lightning.engine.core.store.PermissionLevel;
import ca.samanthaireland.lightning.engine.core.store.PermissionRegistry;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple thread-safe implementation of {@link PermissionRegistry}.
 *
 * <p>This registry stores component permission levels, owner module names,
 * and component names in concurrent maps. Components that are not registered
 * default to {@link PermissionLevel#PRIVATE}.
 */
public class SimplePermissionRegistry implements PermissionRegistry {

    /**
     * Record to hold component registration info.
     */
    private record ComponentInfo(PermissionLevel level, String ownerModule, String componentName) {}

    private final Map<Long, ComponentInfo> componentInfoMap = new ConcurrentHashMap<>();

    @Override
    public void registerComponent(PermissionComponent component, String ownerModuleName) {
        componentInfoMap.put(
                component.getId(),
                new ComponentInfo(component.getPermissionLevel(), ownerModuleName, component.getName())
        );
    }

    @Override
    public void registerComponent(PermissionComponent component) {
        // When no owner is specified, use "Unknown" as placeholder
        registerComponent(component, "Unknown");
    }

    @Override
    public void registerComponents(Collection<PermissionComponent> components, String ownerModuleName) {
        for (PermissionComponent component : components) {
            registerComponent(component, ownerModuleName);
        }
    }

    @Override
    public void registerComponents(Collection<PermissionComponent> components) {
        for (PermissionComponent component : components) {
            registerComponent(component);
        }
    }

    @Override
    public PermissionLevel getPermissionLevel(long componentId) {
        ComponentInfo info = componentInfoMap.get(componentId);
        return info != null ? info.level() : PermissionLevel.PRIVATE;
    }

    @Override
    public Optional<String> getOwnerModuleName(long componentId) {
        ComponentInfo info = componentInfoMap.get(componentId);
        return info != null ? Optional.of(info.ownerModule()) : Optional.empty();
    }

    @Override
    public Optional<String> getComponentName(long componentId) {
        ComponentInfo info = componentInfoMap.get(componentId);
        return info != null ? Optional.of(info.componentName()) : Optional.empty();
    }

    @Override
    public void clear() {
        componentInfoMap.clear();
    }
}
