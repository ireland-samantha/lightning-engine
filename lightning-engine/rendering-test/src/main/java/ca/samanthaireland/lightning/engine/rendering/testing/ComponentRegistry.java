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

import ca.samanthaireland.lightning.engine.rendering.render2d.Button;
import ca.samanthaireland.lightning.engine.rendering.render2d.Label;
import ca.samanthaireland.lightning.engine.rendering.render2d.Panel;
import ca.samanthaireland.lightning.engine.rendering.render2d.TextField;
import ca.samanthaireland.lightning.engine.rendering.render2d.WindowComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Registry for tracking GUI components by ID and other attributes.
 * Enables ID-based lookup across the component tree for test automation.
 */
public class ComponentRegistry {

    private final Map<String, WindowComponent> componentsById = new ConcurrentHashMap<>();
    private final List<WindowComponent> allComponents = new ArrayList<>();

    /**
     * Register a component with an ID.
     * @param id the unique identifier
     * @param component the component to register
     */
    public void register(String id, WindowComponent component) {
        if (id != null && !id.isEmpty()) {
            componentsById.put(id, component);
            component.setId(id);
        }
        if (!allComponents.contains(component)) {
            allComponents.add(component);
        }
    }

    /**
     * Register a component using its existing ID.
     * @param component the component to register
     */
    public void register(WindowComponent component) {
        String id = component.getId();
        if (id != null && !id.isEmpty()) {
            componentsById.put(id, component);
        }
        if (!allComponents.contains(component)) {
            allComponents.add(component);
        }
    }

    /**
     * Register a component and recursively register all children if it's a Panel.
     * Also handles components that wrap a Panel (e.g., custom panels with a visualPanel field).
     * @param component the component to register
     */
    public void registerTree(WindowComponent component) {
        register(component);
        if (component instanceof Panel panel) {
            for (WindowComponent child : panel.getChildren()) {
                registerTree(child);
            }
        } else {
            // Handle custom panels that wrap internal panels
            // Check for common panel field names: visualPanel, formPanel, etc.
            for (String panelFieldName : PANEL_FIELD_NAMES) {
                Panel childPanel = findPanelField(component, panelFieldName);
                if (childPanel != null) {
                    registerTree(childPanel);
                }
            }
        }
    }

    /** Common field names for nested panels in custom panel implementations */
    private static final String[] PANEL_FIELD_NAMES = {
        "visualPanel", "formPanel", "contentPanel", "mainPanel", "previewPanel"
    };

    /**
     * Try to find a panel field by name in the component via reflection.
     * This handles custom panel implementations that wrap a Panel.
     * Searches the entire class hierarchy (including superclasses).
     */
    private Panel findPanelField(WindowComponent component, String fieldName) {
        // Search through the class hierarchy
        Class<?> clazz = component.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                var field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(component);
                if (value instanceof Panel panel) {
                    return panel;
                }
                // Also handle AbstractWindowComponent subclasses that implement rendering
                if (value instanceof WindowComponent wc) {
                    // Check if this component has its own visualPanel
                    Panel nestedPanel = findPanelField(wc, "visualPanel");
                    if (nestedPanel != null) {
                        return nestedPanel;
                    }
                }
            } catch (NoSuchFieldException e) {
                // Field not in this class, try superclass
            } catch (IllegalAccessException e) {
                // Can't access - try superclass
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    /**
     * Unregister a component.
     * @param component the component to unregister
     */
    public void unregister(WindowComponent component) {
        String id = component.getId();
        if (id != null) {
            componentsById.remove(id);
        }
        allComponents.remove(component);
    }

    /**
     * Clear all registered components.
     */
    public void clear() {
        componentsById.clear();
        allComponents.clear();
    }

    /**
     * Find a component by its ID.
     * @param id the component ID
     * @return the component, or null if not found
     */
    public WindowComponent findById(String id) {
        return componentsById.get(id);
    }

    /**
     * Find all components of a specific type.
     * @param type the component type
     * @return list of matching components
     */
    public List<WindowComponent> findByType(Class<? extends WindowComponent> type) {
        return allComponents.stream()
            .filter(type::isInstance)
            .toList();
    }

    /**
     * Find the first component of a specific type.
     * @param type the component type
     * @return the first matching component, or null if not found
     */
    public WindowComponent findFirstByType(Class<? extends WindowComponent> type) {
        return allComponents.stream()
            .filter(type::isInstance)
            .findFirst()
            .orElse(null);
    }

    /**
     * Find a component by its text content (for Button, Label, etc.).
     * @param text the text to search for
     * @return the first matching component, or null if not found
     */
    public WindowComponent findByText(String text) {
        return allComponents.stream()
            .filter(c -> textMatches(c, text))
            .findFirst()
            .orElse(null);
    }

    /**
     * Find all components with exact text match.
     * @param text the exact text to match
     * @return list of matching components
     */
    public List<WindowComponent> findAllByText(String text) {
        return allComponents.stream()
            .filter(c -> textMatches(c, text))
            .toList();
    }

    /**
     * Find all components whose text contains the given substring.
     * @param text the text substring to search for
     * @return list of matching components
     */
    public List<WindowComponent> findByTextContaining(String text) {
        return allComponents.stream()
            .filter(c -> textContains(c, text))
            .toList();
    }

    /**
     * Find a Panel by its title.
     * Searches for panels with matching title by calling the panel's title field via reflection or instance check.
     * @param title the panel title
     * @return the first matching panel, or null if not found
     */
    public WindowComponent findByTitle(String title) {
        return allComponents.stream()
            .filter(c -> c instanceof Panel)
            .filter(c -> panelHasTitle(c, title))
            .findFirst()
            .orElse(null);
    }

    private boolean panelHasTitle(WindowComponent component, String title) {
        // Try to get title via reflection since Panel interface doesn't expose getTitle()
        try {
            var method = component.getClass().getMethod("getTitle");
            Object result = method.invoke(component);
            return title.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Find all components matching a predicate.
     * @param predicate the filter condition
     * @return list of matching components
     */
    public List<WindowComponent> findAll(Predicate<WindowComponent> predicate) {
        return allComponents.stream()
            .filter(predicate)
            .toList();
    }

    /**
     * Get all registered components.
     * @return list of all components
     */
    public List<WindowComponent> getAllComponents() {
        return new ArrayList<>(allComponents);
    }

    /**
     * Get the count of registered components.
     * @return the component count
     */
    public int size() {
        return allComponents.size();
    }

    private boolean textMatches(WindowComponent component, String text) {
        String componentText = getComponentText(component);
        return text.equals(componentText);
    }

    private boolean textContains(WindowComponent component, String text) {
        String componentText = getComponentText(component);
        return componentText != null && componentText.contains(text);
    }

    /**
     * Get the text content of a component if available.
     * @param component the component
     * @return the text, or null if not a text-bearing component
     */
    public static String getComponentText(WindowComponent component) {
        if (component instanceof Button button) {
            return button.getText();
        } else if (component instanceof Label label) {
            return label.getText();
        } else if (component instanceof TextField textField) {
            return textField.getText();
        }
        return null;
    }
}
