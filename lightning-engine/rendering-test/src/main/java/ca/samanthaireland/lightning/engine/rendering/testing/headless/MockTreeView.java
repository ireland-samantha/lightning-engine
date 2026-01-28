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


package ca.samanthaireland.lightning.engine.rendering.testing.headless;

import ca.samanthaireland.lightning.engine.rendering.render2d.AbstractWindowComponent;
import ca.samanthaireland.lightning.engine.rendering.render2d.InputConstants;
import ca.samanthaireland.lightning.engine.rendering.render2d.Renderer;
import ca.samanthaireland.lightning.engine.rendering.render2d.TreeNode;
import ca.samanthaireland.lightning.engine.rendering.render2d.TreeView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Mock TreeView implementation for headless testing.
 */
public class MockTreeView extends AbstractWindowComponent implements TreeView {

    private final List<TreeNode> rootNodes = new ArrayList<>();
    private TreeNode selectedNode;
    private int itemHeight = 24;
    private int indentWidth = 20;

    private Consumer<TreeNode> onSelect;

    public MockTreeView(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public void addRootNode(TreeNode node) {
        rootNodes.add(node);
    }

    @Override
    public List<TreeNode> getRootNodes() {
        return new ArrayList<>(rootNodes);
    }

    @Override
    public void clearNodes() {
        rootNodes.clear();
        selectedNode = null;
    }

    @Override
    public void expandAll() {
        for (TreeNode node : rootNodes) {
            expandRecursive(node);
        }
    }

    @Override
    public void collapseAll() {
        for (TreeNode node : rootNodes) {
            collapseRecursive(node);
        }
    }

    private void expandRecursive(TreeNode node) {
        node.setExpanded(true);
        for (TreeNode child : node.getChildren()) {
            expandRecursive(child);
        }
    }

    private void collapseRecursive(TreeNode node) {
        node.setExpanded(false);
        for (TreeNode child : node.getChildren()) {
            collapseRecursive(child);
        }
    }

    @Override
    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    /**
     * Select a node programmatically.
     */
    public void setSelectedNode(TreeNode node) {
        this.selectedNode = node;
        if (onSelect != null && node != null) {
            onSelect.accept(node);
        }
    }

    @Override
    public void setOnSelect(Consumer<TreeNode> onSelect) {
        this.onSelect = onSelect;
    }

    @Override
    public void setItemHeight(int height) {
        this.itemHeight = height;
    }

    @Override
    public void setIndentWidth(int width) {
        this.indentWidth = width;
    }

    public int getItemHeight() {
        return itemHeight;
    }

    public int getIndentWidth() {
        return indentWidth;
    }

    @Override
    public void render(Renderer renderer) {
        // No rendering in headless mode
    }

    @Override
    public boolean onMouseClick(int mx, int my, int button, int action) {
        if (!visible || !contains(mx, my)) {
            return false;
        }

        if (InputConstants.isLeftButton(button) && InputConstants.isPress(action)) {
            // Find clicked node based on position
            int localY = my - y;
            int clickedRow = localY / itemHeight;
            TreeNode clickedNode = findNodeByRow(clickedRow);

            if (clickedNode != null) {
                // Check if click is on expand/collapse arrow area
                int nodeDepth = getNodeDepth(clickedNode);
                int arrowX = x + nodeDepth * indentWidth;

                if (mx >= arrowX && mx < arrowX + indentWidth && clickedNode.hasChildren()) {
                    clickedNode.setExpanded(!clickedNode.isExpanded());
                } else {
                    selectedNode = clickedNode;
                    if (onSelect != null) {
                        onSelect.accept(clickedNode);
                    }
                }
                return true;
            }
        }

        return true;
    }

    private TreeNode findNodeByRow(int targetRow) {
        int[] currentRow = {0};
        for (TreeNode root : rootNodes) {
            TreeNode found = findNodeByRowRecursive(root, targetRow, currentRow);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private TreeNode findNodeByRowRecursive(TreeNode node, int targetRow, int[] currentRow) {
        if (currentRow[0] == targetRow) {
            return node;
        }
        currentRow[0]++;

        if (node.isExpanded()) {
            for (TreeNode child : node.getChildren()) {
                TreeNode found = findNodeByRowRecursive(child, targetRow, currentRow);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private int getNodeDepth(TreeNode targetNode) {
        for (TreeNode root : rootNodes) {
            int depth = getNodeDepthRecursive(root, targetNode, 0);
            if (depth >= 0) {
                return depth;
            }
        }
        return 0;
    }

    private int getNodeDepthRecursive(TreeNode current, TreeNode target, int depth) {
        if (current == target) {
            return depth;
        }
        for (TreeNode child : current.getChildren()) {
            int found = getNodeDepthRecursive(child, target, depth + 1);
            if (found >= 0) {
                return found;
            }
        }
        return -1;
    }

    /**
     * Get the total number of visible rows in the tree.
     */
    public int getVisibleRowCount() {
        int count = 0;
        for (TreeNode root : rootNodes) {
            count += countVisibleNodes(root);
        }
        return count;
    }

    private int countVisibleNodes(TreeNode node) {
        int count = 1;
        if (node.isExpanded()) {
            for (TreeNode child : node.getChildren()) {
                count += countVisibleNodes(child);
            }
        }
        return count;
    }
}
