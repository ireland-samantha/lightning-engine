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
import java.util.function.Consumer;

/**
 * Interface for tree view components.
 */
public interface TreeView extends WindowComponent {

    /**
     * Add a root node.
     */
    void addRootNode(TreeNode node);

    /**
     * Get all root nodes.
     */
    List<TreeNode> getRootNodes();

    /**
     * Clear all nodes.
     */
    void clearNodes();

    /**
     * Expand all nodes.
     */
    void expandAll();

    /**
     * Collapse all nodes.
     */
    void collapseAll();

    /**
     * Get the selected node.
     */
    TreeNode getSelectedNode();

    /**
     * Set the selection handler.
     */
    void setOnSelect(Consumer<TreeNode> onSelect);

    /**
     * Set the item height.
     */
    void setItemHeight(int height);

    /**
     * Set the indent width for child nodes.
     */
    void setIndentWidth(int width);
}
