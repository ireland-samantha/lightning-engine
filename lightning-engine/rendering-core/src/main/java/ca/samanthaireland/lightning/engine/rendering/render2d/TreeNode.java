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


package ca.samanthaireland.lightning.engine.rendering.render2d;

import java.util.List;

/**
 * Interface for tree node data in a TreeView.
 */
public interface TreeNode {

    /**
     * Get the node label.
     */
    String getLabel();

    /**
     * Set the node label.
     */
    void setLabel(String label);

    /**
     * Get the child nodes.
     */
    List<TreeNode> getChildren();

    /**
     * Add a child node.
     */
    void addChild(TreeNode child);

    /**
     * Remove a child node.
     */
    void removeChild(TreeNode child);

    /**
     * Clear all children.
     */
    void clearChildren();

    /**
     * Check if the node is expanded.
     */
    boolean isExpanded();

    /**
     * Set the expanded state.
     */
    void setExpanded(boolean expanded);

    /**
     * Check if the node has children.
     */
    default boolean hasChildren() {
        return !getChildren().isEmpty();
    }

    /**
     * Get user data associated with this node.
     */
    Object getUserData();

    /**
     * Set user data associated with this node.
     */
    void setUserData(Object userData);
}
