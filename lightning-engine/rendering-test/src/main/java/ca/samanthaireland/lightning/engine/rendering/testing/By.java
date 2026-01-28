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


package ca.samanthaireland.lightning.engine.rendering.testing;

import ca.samanthaireland.lightning.engine.rendering.render2d.Panel;
import ca.samanthaireland.lightning.engine.rendering.render2d.WindowComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Locator strategies for finding GUI components.
 * Mirrors Selenium's By class pattern.
 */
public abstract class By {

    /**
     * Find a single component matching this locator.
     * @param registry the component registry to search
     * @return the matching component, or null if not found
     */
    public abstract WindowComponent find(ComponentRegistry registry);

    /**
     * Find all components matching this locator.
     * @param registry the component registry to search
     * @return list of matching components (may be empty)
     */
    public abstract List<WindowComponent> findAll(ComponentRegistry registry);

    /**
     * Get a description of this locator for error messages.
     * @return human-readable description
     */
    public abstract String describe();

    @Override
    public String toString() {
        return describe();
    }

    // ========== Factory Methods ==========

    /**
     * Locate by component ID.
     * @param id the component ID
     * @return the locator
     */
    public static By id(String id) {
        return new ById(id);
    }

    /**
     * Locate by exact text content (for Button, Label, TextField).
     * @param text the exact text to match
     * @return the locator
     */
    public static By text(String text) {
        return new ByText(text, false);
    }

    /**
     * Locate by partial text content.
     * @param text the text substring to match
     * @return the locator
     */
    public static By textContaining(String text) {
        return new ByText(text, true);
    }

    /**
     * Locate by component type.
     * @param type the component class
     * @return the locator
     */
    public static By type(Class<? extends WindowComponent> type) {
        return new ByType(type);
    }

    /**
     * Locate a Panel by its title.
     * @param title the panel title
     * @return the locator
     */
    public static By title(String title) {
        return new ByTitle(title);
    }

    /**
     * Combine multiple locators with AND logic.
     * @param locators the locators to combine
     * @return the combined locator
     */
    public static By and(By... locators) {
        return new ByAnd(locators);
    }

    /**
     * Combine multiple locators with OR logic.
     * @param locators the locators to combine
     * @return the combined locator
     */
    public static By or(By... locators) {
        return new ByOr(locators);
    }

    /**
     * Scope this locator to search within a parent.
     * @param parent the parent locator
     * @return a scoped locator
     */
    public By within(By parent) {
        return new ByWithin(parent, this);
    }

    // ========== Locator Implementations ==========

    private static class ById extends By {
        private final String id;

        ById(String id) {
            this.id = Objects.requireNonNull(id, "id cannot be null");
        }

        @Override
        public WindowComponent find(ComponentRegistry registry) {
            return registry.findById(id);
        }

        @Override
        public List<WindowComponent> findAll(ComponentRegistry registry) {
            WindowComponent component = find(registry);
            return component != null ? List.of(component) : List.of();
        }

        @Override
        public String describe() {
            return "By.id(\"" + id + "\")";
        }
    }

    private static class ByText extends By {
        private final String text;
        private final boolean partial;

        ByText(String text, boolean partial) {
            this.text = Objects.requireNonNull(text, "text cannot be null");
            this.partial = partial;
        }

        @Override
        public WindowComponent find(ComponentRegistry registry) {
            if (partial) {
                List<WindowComponent> matches = registry.findByTextContaining(text);
                return matches.isEmpty() ? null : matches.get(0);
            } else {
                return registry.findByText(text);
            }
        }

        @Override
        public List<WindowComponent> findAll(ComponentRegistry registry) {
            if (partial) {
                return registry.findByTextContaining(text);
            } else {
                return registry.findAllByText(text);
            }
        }

        @Override
        public String describe() {
            return partial
                ? "By.textContaining(\"" + text + "\")"
                : "By.text(\"" + text + "\")";
        }
    }

    private static class ByType extends By {
        private final Class<? extends WindowComponent> type;

        ByType(Class<? extends WindowComponent> type) {
            this.type = Objects.requireNonNull(type, "type cannot be null");
        }

        @Override
        public WindowComponent find(ComponentRegistry registry) {
            return registry.findFirstByType(type);
        }

        @Override
        public List<WindowComponent> findAll(ComponentRegistry registry) {
            return registry.findByType(type);
        }

        @Override
        public String describe() {
            return "By.type(" + type.getSimpleName() + ".class)";
        }
    }

    private static class ByTitle extends By {
        private final String title;

        ByTitle(String title) {
            this.title = Objects.requireNonNull(title, "title cannot be null");
        }

        @Override
        public WindowComponent find(ComponentRegistry registry) {
            return registry.findByTitle(title);
        }

        @Override
        public List<WindowComponent> findAll(ComponentRegistry registry) {
            WindowComponent component = find(registry);
            return component != null ? List.of(component) : List.of();
        }

        @Override
        public String describe() {
            return "By.title(\"" + title + "\")";
        }
    }

    private static class ByAnd extends By {
        private final By[] locators;

        ByAnd(By[] locators) {
            if (locators == null || locators.length < 2) {
                throw new IllegalArgumentException("At least 2 locators required for AND");
            }
            this.locators = locators;
        }

        @Override
        public WindowComponent find(ComponentRegistry registry) {
            List<WindowComponent> matches = findAll(registry);
            return matches.isEmpty() ? null : matches.get(0);
        }

        @Override
        public List<WindowComponent> findAll(ComponentRegistry registry) {
            List<WindowComponent> result = new ArrayList<>(locators[0].findAll(registry));
            for (int i = 1; i < locators.length && !result.isEmpty(); i++) {
                List<WindowComponent> next = locators[i].findAll(registry);
                result.retainAll(next);
            }
            return result;
        }

        @Override
        public String describe() {
            return "By.and(" + String.join(", ",
                Arrays.stream(locators).map(By::describe).toArray(String[]::new)) + ")";
        }
    }

    private static class ByOr extends By {
        private final By[] locators;

        ByOr(By[] locators) {
            if (locators == null || locators.length < 2) {
                throw new IllegalArgumentException("At least 2 locators required for OR");
            }
            this.locators = locators;
        }

        @Override
        public WindowComponent find(ComponentRegistry registry) {
            for (By locator : locators) {
                WindowComponent found = locator.find(registry);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }

        @Override
        public List<WindowComponent> findAll(ComponentRegistry registry) {
            List<WindowComponent> result = new ArrayList<>();
            for (By locator : locators) {
                for (WindowComponent component : locator.findAll(registry)) {
                    if (!result.contains(component)) {
                        result.add(component);
                    }
                }
            }
            return result;
        }

        @Override
        public String describe() {
            return "By.or(" + String.join(", ",
                Arrays.stream(locators).map(By::describe).toArray(String[]::new)) + ")";
        }
    }

    private static class ByWithin extends By {
        private final By parent;
        private final By child;

        ByWithin(By parent, By child) {
            this.parent = Objects.requireNonNull(parent, "parent cannot be null");
            this.child = Objects.requireNonNull(child, "child cannot be null");
        }

        @Override
        public WindowComponent find(ComponentRegistry registry) {
            WindowComponent parentComponent = parent.find(registry);
            if (parentComponent instanceof Panel panel) {
                ComponentRegistry scopedRegistry = new ComponentRegistry();
                for (WindowComponent c : panel.getChildren()) {
                    scopedRegistry.registerTree(c);
                }
                return child.find(scopedRegistry);
            }
            return null;
        }

        @Override
        public List<WindowComponent> findAll(ComponentRegistry registry) {
            WindowComponent parentComponent = parent.find(registry);
            if (parentComponent instanceof Panel panel) {
                ComponentRegistry scopedRegistry = new ComponentRegistry();
                for (WindowComponent c : panel.getChildren()) {
                    scopedRegistry.registerTree(c);
                }
                return child.findAll(scopedRegistry);
            }
            return List.of();
        }

        @Override
        public String describe() {
            return child.describe() + ".within(" + parent.describe() + ")";
        }
    }
}
