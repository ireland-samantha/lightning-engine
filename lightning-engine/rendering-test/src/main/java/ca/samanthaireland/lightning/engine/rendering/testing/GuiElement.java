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
import ca.samanthaireland.lightning.engine.rendering.render2d.TextField;
import ca.samanthaireland.lightning.engine.rendering.render2d.TreeNode;
import ca.samanthaireland.lightning.engine.rendering.render2d.TreeView;
import ca.samanthaireland.lightning.engine.rendering.render2d.WindowComponent;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around a WindowComponent providing interaction methods.
 * Mirrors Selenium's WebElement pattern.
 */
public class GuiElement {

    private final WindowComponent component;
    private final GuiDriver driver;

    /**
     * Create a new GuiElement wrapping a component.
     * @param component the wrapped component
     * @param driver the driver for input simulation
     */
    public GuiElement(WindowComponent component, GuiDriver driver) {
        this.component = component;
        this.driver = driver;
    }

    /**
     * Get the underlying WindowComponent.
     * @return the wrapped component
     */
    public WindowComponent getComponent() {
        return component;
    }

    // ========== Information Methods ==========

    /**
     * Get the component ID.
     * @return the ID, or null if not set
     */
    public String getId() {
        return component.getId();
    }

    /**
     * Get the text content of this element.
     * Works for Button, Label, TextField.
     * @return the text, or null if not a text-bearing component
     */
    public String getText() {
        return ComponentRegistry.getComponentText(component);
    }

    /**
     * Check if the element is visible.
     * @return true if visible
     */
    public boolean isVisible() {
        return component.isVisible();
    }

    /**
     * Check if the element is enabled.
     * Currently all components are enabled; this method exists for API compatibility.
     * @return true (always)
     */
    public boolean isEnabled() {
        return true;
    }

    /**
     * Get the bounds of this element.
     * @return the bounding rectangle
     */
    public Rectangle getBounds() {
        return new Rectangle(
            component.getX(),
            component.getY(),
            component.getWidth(),
            component.getHeight()
        );
    }

    /**
     * Get the X position.
     * @return x coordinate
     */
    public int getX() {
        return component.getX();
    }

    /**
     * Get the Y position.
     * @return y coordinate
     */
    public int getY() {
        return component.getY();
    }

    /**
     * Get the width.
     * @return width in pixels
     */
    public int getWidth() {
        return component.getWidth();
    }

    /**
     * Get the height.
     * @return height in pixels
     */
    public int getHeight() {
        return component.getHeight();
    }

    // ========== Interaction Methods ==========

    /**
     * Click on this element (at its center).
     */
    public void click() {
        int centerX = component.getX() + component.getWidth() / 2;
        int centerY = component.getY() + component.getHeight() / 2;
        driver.click(centerX, centerY);
    }

    /**
     * Double-click on this element.
     */
    public void doubleClick() {
        int centerX = component.getX() + component.getWidth() / 2;
        int centerY = component.getY() + component.getHeight() / 2;
        driver.click(centerX, centerY);
        driver.click(centerX, centerY);
    }

    /**
     * Type text into this element (for TextField).
     * @param text the text to type
     */
    public void type(String text) {
        // First click to focus
        click();
        // Then type each character
        for (char c : text.toCharArray()) {
            component.onCharInput(c);
        }
    }

    /**
     * Clear the text content (for TextField).
     */
    public void clear() {
        if (component instanceof TextField textField) {
            textField.setText("");
        }
    }

    /**
     * Send a key press to this element.
     * @param keyCode the key code (from KeyCodes)
     */
    public void sendKey(int keyCode) {
        component.onKeyPress(keyCode, 1); // Press
        component.onKeyPress(keyCode, 0); // Release
    }

    // ========== TreeView/TreeNode Methods ==========

    /**
     * Get children of this element (for Panel, TreeView, or TreeNode).
     * @return list of child elements
     */
    public List<GuiElement> getChildren() {
        List<GuiElement> children = new ArrayList<>();

        if (component instanceof Panel panel) {
            for (WindowComponent child : panel.getChildren()) {
                children.add(new GuiElement(child, driver));
            }
        } else if (component instanceof TreeView treeView) {
            for (TreeNode node : treeView.getRootNodes()) {
                children.add(new TreeNodeElement(node, driver));
            }
        }

        return children;
    }

    /**
     * Get a specific child by index.
     * @param index the child index
     * @return the child element
     */
    public GuiElement getChild(int index) {
        List<GuiElement> children = getChildren();
        if (index >= 0 && index < children.size()) {
            return children.get(index);
        }
        return null;
    }

    /**
     * Select this item (for ListView).
     */
    public void select() {
        click();
    }

    /**
     * Expand this node (for TreeNode - use TreeNodeElement).
     */
    public void expand() {
        // Override in TreeNodeElement
    }

    /**
     * Collapse this node (for TreeNode - use TreeNodeElement).
     */
    public void collapse() {
        // Override in TreeNodeElement
    }

    // ========== Chained Element Search ==========

    /**
     * Find an element within this element's children.
     * @param locator the locator to use
     * @return the found element
     * @throws NoSuchElementException if not found
     */
    public GuiElement findElement(By locator) {
        if (component instanceof Panel panel) {
            ComponentRegistry scopedRegistry = new ComponentRegistry();
            for (WindowComponent child : panel.getChildren()) {
                scopedRegistry.registerTree(child);
            }
            WindowComponent found = locator.find(scopedRegistry);
            if (found != null) {
                return new GuiElement(found, driver);
            }
        }
        throw new NoSuchElementException("Element not found: " + locator.describe());
    }

    /**
     * Find all elements within this element matching the locator.
     * @param locator the locator to use
     * @return list of found elements
     */
    public List<GuiElement> findElements(By locator) {
        List<GuiElement> result = new ArrayList<>();
        if (component instanceof Panel panel) {
            ComponentRegistry scopedRegistry = new ComponentRegistry();
            for (WindowComponent child : panel.getChildren()) {
                scopedRegistry.registerTree(child);
            }
            for (WindowComponent found : locator.findAll(scopedRegistry)) {
                result.add(new GuiElement(found, driver));
            }
        }
        return result;
    }

    // ========== Fluent Assertions ==========

    /**
     * Assert that this element is visible.
     * @return this element for chaining
     * @throws AssertionError if not visible
     */
    public GuiElement shouldBeVisible() {
        if (!isVisible()) {
            throw new AssertionError("Element should be visible: " + describe());
        }
        return this;
    }

    /**
     * Assert that this element has the expected text.
     * @param expected the expected text
     * @return this element for chaining
     * @throws AssertionError if text doesn't match
     */
    public GuiElement shouldHaveText(String expected) {
        String actual = getText();
        if (!expected.equals(actual)) {
            throw new AssertionError(
                "Element text mismatch. Expected: \"" + expected + "\", Actual: \"" + actual + "\"");
        }
        return this;
    }

    /**
     * Assert that this element's text contains the expected substring.
     * @param expected the expected substring
     * @return this element for chaining
     * @throws AssertionError if text doesn't contain substring
     */
    public GuiElement shouldContainText(String expected) {
        String actual = getText();
        if (actual == null || !actual.contains(expected)) {
            throw new AssertionError(
                "Element text should contain: \"" + expected + "\", Actual: \"" + actual + "\"");
        }
        return this;
    }

    /**
     * Get a description of this element for error messages.
     * @return human-readable description
     */
    public String describe() {
        String id = getId();
        String text = getText();
        String type = component.getClass().getSimpleName();

        if (id != null) {
            return type + "#" + id;
        } else if (text != null) {
            return type + "(\"" + text + "\")";
        } else {
            return type + "@(" + getX() + "," + getY() + ")";
        }
    }

    @Override
    public String toString() {
        return describe();
    }

    // ========== TreeNode Element ==========

    /**
     * Specialized element for TreeNode.
     */
    public static class TreeNodeElement extends GuiElement {
        private final TreeNode node;
        private final GuiDriver treeDriver;

        public TreeNodeElement(TreeNode node, GuiDriver driver) {
            super(null, driver);
            this.node = node;
            this.treeDriver = driver;
        }

        @Override
        public WindowComponent getComponent() {
            return null; // TreeNode is not a WindowComponent
        }

        @Override
        public String getText() {
            return node.getLabel();
        }

        @Override
        public boolean isVisible() {
            return true;
        }

        @Override
        public List<GuiElement> getChildren() {
            List<GuiElement> children = new ArrayList<>();
            for (TreeNode child : node.getChildren()) {
                children.add(new TreeNodeElement(child, treeDriver));
            }
            return children;
        }

        @Override
        public void expand() {
            node.setExpanded(true);
        }

        @Override
        public void collapse() {
            node.setExpanded(false);
        }

        /**
         * Check if this node is expanded.
         * @return true if expanded
         */
        public boolean isExpanded() {
            return node.isExpanded();
        }

        /**
         * Get the underlying TreeNode.
         * @return the tree node
         */
        public TreeNode getNode() {
            return node;
        }

        @Override
        public String describe() {
            return "TreeNode(\"" + node.getLabel() + "\")";
        }
    }
}
