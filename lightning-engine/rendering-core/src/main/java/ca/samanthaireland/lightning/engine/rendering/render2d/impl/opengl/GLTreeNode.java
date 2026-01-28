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


package ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl;

import ca.samanthaireland.lightning.engine.rendering.render2d.TreeNode;

import java.util.ArrayList;
import java.util.List;

/**
 * A node in a tree data structure for use with {@link GLTreeView}.
 * OpenGL implementation of {@link TreeNode}.
 *
 * <p>Each node has:
 * <ul>
 *   <li>A text label for display</li>
 *   <li>Optional user data for application-specific information</li>
 *   <li>An expanded/collapsed state</li>
 *   <li>A list of child nodes</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * TreeNode root = new GLTreeNode("Root");
 * root.setExpanded(true);
 *
 * TreeNode child1 = new GLTreeNode("Child 1", someData);
 * TreeNode child2 = new GLTreeNode("Child 2");
 *
 * root.addChild(child1);
 * root.addChild(child2);
 *
 * treeView.addRootNode(root);
 * }</pre>
 */
public class GLTreeNode implements TreeNode {

    private String text;
    private Object userData;
    private boolean expanded;
    private final List<TreeNode> children = new ArrayList<>();

    /**
     * Create a new tree node with the given text.
     *
     * @param text the text to display
     */
    public GLTreeNode(String text) {
        this.text = text;
        this.expanded = false;
    }

    /**
     * Create a new tree node with the given text and user data.
     *
     * @param text the text to display
     * @param userData optional user data to associate with this node
     */
    public GLTreeNode(String text, Object userData) {
        this.text = text;
        this.userData = userData;
        this.expanded = false;
    }

    /**
     * Get the display text.
     *
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * Set the display text.
     *
     * @param text the text to display
     */
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String getLabel() {
        return text;
    }

    @Override
    public void setLabel(String label) {
        this.text = label;
    }

    /**
     * Get the user data.
     *
     * @return the user data, or null if not set
     */
    public Object getUserData() {
        return userData;
    }

    /**
     * Set the user data.
     *
     * @param userData the user data to associate with this node
     */
    public void setUserData(Object userData) {
        this.userData = userData;
    }

    /**
     * Check if this node is expanded.
     *
     * @return true if expanded, false if collapsed
     */
    public boolean isExpanded() {
        return expanded;
    }

    /**
     * Set the expanded state.
     *
     * @param expanded true to expand, false to collapse
     */
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    /**
     * Get the list of child nodes.
     *
     * @return the list of children (modifiable)
     */
    @Override
    public List<TreeNode> getChildren() {
        return children;
    }

    /**
     * Add a child node.
     *
     * @param child the child node to add
     */
    @Override
    public void addChild(TreeNode child) {
        children.add(child);
    }

    /**
     * Remove a child node.
     *
     * @param child the child node to remove
     */
    @Override
    public void removeChild(TreeNode child) {
        children.remove(child);
    }

    /**
     * Remove all child nodes.
     */
    @Override
    public void clearChildren() {
        children.clear();
    }

    /**
     * Check if this node has any children.
     *
     * @return true if this node has children
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * Get the number of children.
     *
     * @return the child count
     */
    public int getChildCount() {
        return children.size();
    }
}
