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

import ca.samanthaireland.lightning.engine.core.store.BaseComponent;
import ca.samanthaireland.lightning.engine.core.store.ComponentRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory implementation of {@link ComponentRegistry}.
 *
 * <p>Thread-safe using ConcurrentHashMap.
 */
@Slf4j
public class SimpleComponentRegistry implements ComponentRegistry {

    private final Map<String, BaseComponent> componentsByName = new ConcurrentHashMap<>();

    @Override
    public void register(BaseComponent component) {
        Objects.requireNonNull(component, "component must not be null");
        String name = component.getName();
        Objects.requireNonNull(name, "component name must not be null");

        BaseComponent existing = componentsByName.put(name, component);
        if (existing != null) {
            log.debug("Replaced component registration for '{}': {} -> {}",
                name, existing.getClass().getSimpleName(), component.getClass().getSimpleName());
        } else {
            log.debug("Registered component '{}'", name);
        }
    }

    @Override
    public Optional<BaseComponent> findByName(String name) {
        return Optional.ofNullable(componentsByName.get(name));
    }

    @Override
    public Set<String> getRegisteredNames() {
        return Set.copyOf(componentsByName.keySet());
    }

    @Override
    public void clear() {
        componentsByName.clear();
        log.debug("Cleared all component registrations");
    }

    /**
     * Get the number of registered components.
     *
     * @return the count of registered components
     */
    public int size() {
        return componentsByName.size();
    }
}
