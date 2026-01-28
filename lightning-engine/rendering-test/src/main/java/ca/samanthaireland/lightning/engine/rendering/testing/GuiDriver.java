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
import ca.samanthaireland.lightning.engine.rendering.render2d.Window;
import ca.samanthaireland.lightning.engine.rendering.render2d.WindowComponent;

import java.io.Closeable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Main entry point for GUI test automation.
 * Mirrors Selenium's WebDriver pattern.
 *
 * <p>Works with the Window interface, supporting both:
 * <ul>
 *   <li>Headless testing (HeadlessWindow) - no OpenGL required</li>
 *   <li>Live testing (GLWindow) - with actual OpenGL rendering</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Headless Testing (No OpenGL)</h3>
 * <pre>{@code
 * HeadlessWindow window = new HeadlessWindow();
 * window.addComponent(myPanel);
 *
 * GuiDriver driver = GuiDriver.connect(window);
 * driver.findElement(By.text("Save")).click();
 * }</pre>
 */
public class GuiDriver implements Closeable {

    private final Window window;
    private final ComponentRegistry registry;
    private final String backendUrl;
    private boolean closed = false;

    /**
     * Create a GuiDriver connected to a window.
     * @param window the window to drive
     * @param registry the component registry
     * @param backendUrl the backend URL (null for headless)
     */
    private GuiDriver(Window window, ComponentRegistry registry, String backendUrl) {
        this.window = window;
        this.registry = registry;
        this.backendUrl = backendUrl;
    }

    // ========== Factory Methods ==========

    /**
     * Connect to an existing window for headless testing.
     * This is the primary method for unit testing without OpenGL.
     *
     * @param window the window to connect to
     * @return a new GuiDriver
     */
    public static GuiDriver connect(Window window) {
        ComponentRegistry registry = new ComponentRegistry();
        // Register all components from the window
        for (WindowComponent component : window.getComponents()) {
            registry.registerTree(component);
        }
        return new GuiDriver(window, registry, null);
    }

    /**
     * Connect to an existing window with a pre-populated registry.
     *
     * @param window the window to connect to
     * @param registry the component registry
     * @return a new GuiDriver
     */
    public static GuiDriver connect(Window window, ComponentRegistry registry) {
        return new GuiDriver(window, registry, null);
    }

    // ========== Element Location ==========

    /**
     * Find a single element matching the locator.
     *
     * @param locator the locator to use
     * @return the found element
     * @throws NoSuchElementException if not found
     */
    public GuiElement findElement(By locator) {
        // Re-register components in case new ones were added
        refreshRegistry();

        WindowComponent component = locator.find(registry);
        if (component == null) {
            throw new NoSuchElementException("Element not found: " + locator.describe());
        }
        return new GuiElement(component, this);
    }

    /**
     * Find all elements matching the locator.
     *
     * @param locator the locator to use
     * @return list of found elements (may be empty)
     */
    public List<GuiElement> findElements(By locator) {
        refreshRegistry();

        List<GuiElement> elements = new ArrayList<>();
        for (WindowComponent component : locator.findAll(registry)) {
            elements.add(new GuiElement(component, this));
        }
        return elements;
    }

    /**
     * Check if an element exists.
     *
     * @param locator the locator to use
     * @return true if at least one element matches
     */
    public boolean hasElement(By locator) {
        refreshRegistry();
        return locator.find(registry) != null;
    }

    // ========== Input Simulation ==========

    /**
     * Simulate a mouse click at the given coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public void click(int x, int y) {
        // Dispatch to components in reverse order (top-most first)
        List<WindowComponent> components = window.getComponents();
        for (int i = components.size() - 1; i >= 0; i--) {
            WindowComponent component = components.get(i);
            if (component.onMouseClick(x, y, 0, 1)) { // Press
                component.onMouseClick(x, y, 0, 0);   // Release
                return;
            }
        }
    }

    /**
     * Type text by sending character input events.
     *
     * @param text the text to type
     */
    public void type(String text) {
        for (char c : text.toCharArray()) {
            typeChar(c);
        }
    }

    /**
     * Type a single character.
     *
     * @param c the character to type
     */
    public void typeChar(char c) {
        List<WindowComponent> components = window.getComponents();
        for (int i = components.size() - 1; i >= 0; i--) {
            if (components.get(i).onCharInput(c)) {
                return;
            }
        }
    }

    /**
     * Simulate a key press.
     *
     * @param keyCode the key code (from KeyCodes)
     */
    public void pressKey(int keyCode) {
        List<WindowComponent> components = window.getComponents();
        for (int i = components.size() - 1; i >= 0; i--) {
            if (components.get(i).onKeyPress(keyCode, 1)) { // Press
                components.get(i).onKeyPress(keyCode, 0);   // Release
                return;
            }
        }
    }

    /**
     * Simulate mouse movement.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public void moveMouse(int x, int y) {
        List<WindowComponent> components = window.getComponents();
        for (int i = components.size() - 1; i >= 0; i--) {
            components.get(i).onMouseMove(x, y);
        }
    }

    /**
     * Simulate mouse scroll.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param scrollX horizontal scroll amount
     * @param scrollY vertical scroll amount
     */
    public void scroll(int x, int y, double scrollX, double scrollY) {
        List<WindowComponent> components = window.getComponents();
        for (int i = components.size() - 1; i >= 0; i--) {
            if (components.get(i).onMouseScroll(x, y, scrollX, scrollY)) {
                return;
            }
        }
    }

    // ========== Actions Builder ==========

    /**
     * Create an Actions builder for complex interactions.
     *
     * @return a new Actions builder
     */
    public Actions actions() {
        return new Actions(this);
    }

    // ========== Wait Support ==========

    /**
     * Create a Wait with default timeout (10 seconds).
     *
     * @return a new Wait
     */
    public Wait waitFor() {
        return new Wait(this);
    }

    /**
     * Create a Wait with custom timeout.
     *
     * @param timeout the timeout duration
     * @return a new Wait
     */
    public Wait waitFor(Duration timeout) {
        return new Wait(this).timeout(timeout);
    }

    // ========== Window Control ==========

    /**
     * Get the underlying window.
     *
     * @return the window
     */
    public Window getWindow() {
        return window;
    }

    /**
     * Get the component registry.
     *
     * @return the registry
     */
    public ComponentRegistry getRegistry() {
        return registry;
    }

    /**
     * Get the backend URL (if connected to live backend).
     *
     * @return the backend URL, or null for headless
     */
    public String getBackendUrl() {
        return backendUrl;
    }

    /**
     * Check if the driver is connected to a live backend.
     *
     * @return true if connected to backend
     */
    public boolean isLiveBackend() {
        return backendUrl != null;
    }

    /**
     * Check if the window is still open.
     *
     * @return true if open
     */
    public boolean isOpen() {
        return !closed;
    }

    /**
     * Close the driver and window.
     */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            window.stop();
        }
    }

    // ========== Debugging ==========

    /**
     * Dump the component tree for debugging.
     *
     * @return a string representation of all components
     */
    public String dumpComponentTree() {
        StringBuilder sb = new StringBuilder();
        sb.append("Component Tree:\n");
        for (WindowComponent component : window.getComponents()) {
            dumpComponent(sb, component, 0);
        }
        return sb.toString();
    }

    private void dumpComponent(StringBuilder sb, WindowComponent component, int indent) {
        String prefix = "  ".repeat(indent);
        String id = component.getId() != null ? "#" + component.getId() : "";
        String text = ComponentRegistry.getComponentText(component);
        String textStr = text != null ? " \"" + text + "\"" : "";

        sb.append(prefix)
            .append("- ")
            .append(component.getClass().getSimpleName())
            .append(id)
            .append(textStr)
            .append(" @(")
            .append(component.getX()).append(",").append(component.getY())
            .append(" ")
            .append(component.getWidth()).append("x").append(component.getHeight())
            .append(")\n");

        if (component instanceof Panel panel) {
            for (WindowComponent child : panel.getChildren()) {
                dumpComponent(sb, child, indent + 1);
            }
        } else {
            // Handle custom panels with visualPanel field
            Panel visualPanel = findVisualPanel(component);
            if (visualPanel != null) {
                for (WindowComponent child : visualPanel.getChildren()) {
                    dumpComponent(sb, child, indent + 1);
                }
            }
        }
    }

    private Panel findVisualPanel(WindowComponent component) {
        try {
            var field = component.getClass().getDeclaredField("visualPanel");
            field.setAccessible(true);
            Object value = field.get(component);
            if (value instanceof Panel panel) {
                return panel;
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // No visualPanel field or can't access - that's fine
        }
        return null;
    }

    /**
     * Refresh the registry with current window components.
     */
    public void refreshRegistry() {
        registry.clear();
        for (WindowComponent component : window.getComponents()) {
            registry.registerTree(component);
        }
    }
}
