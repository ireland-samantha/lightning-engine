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

import ca.samanthaireland.lightning.engine.rendering.render2d.AbstractWindowComponent;
import ca.samanthaireland.lightning.engine.rendering.render2d.InputConstants;
import ca.samanthaireland.lightning.engine.rendering.render2d.Renderer;
import ca.samanthaireland.lightning.engine.rendering.render2d.TreeNode;
import ca.samanthaireland.lightning.engine.rendering.render2d.TreeView;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A tree view component for hierarchical data display.
 * OpenGL implementation of {@link TreeView}.
 */
@Slf4j
public class GLTreeView extends AbstractWindowComponent implements TreeView {

    private final List<TreeNode> rootNodes = new ArrayList<>();
    private TreeNode selectedNode;
    private TreeNode hoveredNode;
    private float scrollOffset = 0;
    private float itemHeight = 22.0f;
    private boolean debugLogged = false; // Only log once per tree update
    private float fontSize = 13.0f;
    private float indentWidth = 20.0f;
    private int fontId = -1;

    private float[] backgroundColor;
    private float[] selectedBackgroundColor;
    private float[] hoveredBackgroundColor;
    private float[] textColor;
    private float[] borderColor;
    private float[] scrollbarColor;
    private float[] scrollbarTrackColor;
    private float[] expandIconColor;

    private Consumer<TreeNode> onSelectionChanged;

    public GLTreeView(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.backgroundColor = GLColour.PANEL_BG;
        this.selectedBackgroundColor = GLColour.SELECTED;
        this.hoveredBackgroundColor = GLColour.withAlpha(GLColour.SELECTED, 0.3f);
        this.textColor = GLColour.TEXT_PRIMARY;
        this.borderColor = GLColour.BORDER;
        this.scrollbarColor = GLColour.SCROLLBAR;
        this.scrollbarTrackColor = GLColour.SCROLLBAR_TRACK;
        this.expandIconColor = GLColour.TEXT_SECONDARY;
    }

    public void setRootNodes(List<TreeNode> nodes) {
        this.rootNodes.clear();
        this.rootNodes.addAll(nodes);
        this.selectedNode = null;
        this.scrollOffset = 0;
    }

    @Override
    public void addRootNode(TreeNode node) {
        rootNodes.add(node);
    }

    @Override
    public void clearNodes() {
        rootNodes.clear();
        selectedNode = null;
        scrollOffset = 0;
        debugLogged = false; // Reset so we log again after tree rebuild
    }

    @Override
    public List<TreeNode> getRootNodes() {
        return rootNodes;
    }

    @Override
    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedNode(TreeNode node) {
        this.selectedNode = node;
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(node);
        }
    }

    @Override
    public void setOnSelect(Consumer<TreeNode> onSelect) {
        this.onSelectionChanged = onSelect;
    }

    public void setOnSelectionChanged(Consumer<TreeNode> onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
    }

    @Override
    public void setItemHeight(int itemHeight) {
        this.itemHeight = itemHeight;
    }

    public void setItemHeight(float itemHeight) {
        this.itemHeight = itemHeight;
    }

    @Override
    public void setIndentWidth(int width) {
        this.indentWidth = width;
    }

    @Override
    public void expandAll() {
        for (TreeNode node : rootNodes) {
            expandNodeRecursive(node);
        }
    }

    @Override
    public void collapseAll() {
        for (TreeNode node : rootNodes) {
            collapseNodeRecursive(node);
        }
    }

    private void expandNodeRecursive(TreeNode node) {
        node.setExpanded(true);
        for (TreeNode child : node.getChildren()) {
            expandNodeRecursive(child);
        }
    }

    private void collapseNodeRecursive(TreeNode node) {
        node.setExpanded(false);
        for (TreeNode child : node.getChildren()) {
            collapseNodeRecursive(child);
        }
    }

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    public void setFontId(int fontId) {
        this.fontId = fontId;
    }

    public void setBackgroundColor(float[] backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @Override
    public void render(Renderer renderer) {
        if (!visible) {
            return;
        }

        // Log tree structure once after rebuild
        if (!debugLogged && !rootNodes.isEmpty()) {
            debugLogged = true;
            log.info("TreeView rendering: {} root nodes, fontId={}, fontSize={}",
                rootNodes.size(), fontId, fontSize);
            logTreeStructure(rootNodes.get(0), 0);
        }

        // Draw background
        renderer.fillRect(x, y, width, height, backgroundColor);

        // Draw border
        renderer.strokeRect(x, y, width, height, borderColor, 1.0f);

        // Calculate total height and visible area
        float totalHeight = calculateTotalHeight();
        float maxScroll = Math.max(0, totalHeight - height);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        int scrollbarWidth = needsScrollbar(totalHeight) ? 10 : 0;
        int contentWidth = width - scrollbarWidth - 2;

        // Clip content area
        renderer.save();
        renderer.intersectClip(x + 1, y + 1, contentWidth, height - 2);

        // Setup font for rendering
        renderer.setFont(fontId, fontSize);

        // Render nodes
        float currentY = y - scrollOffset;
        for (TreeNode node : rootNodes) {
            currentY = renderNode(renderer, node, 0, currentY, contentWidth);
        }

        renderer.restore();

        // Draw scrollbar if needed
        if (needsScrollbar(totalHeight)) {
            float scrollbarX = x + width - scrollbarWidth - 1;
            float scrollbarHeight = height - 2;
            float thumbHeight = Math.max(20, (height / totalHeight) * scrollbarHeight);
            float thumbY = y + 1 + (scrollOffset / maxScroll) * (scrollbarHeight - thumbHeight);

            // Draw scrollbar track
            renderer.fillRect(scrollbarX, y + 1, scrollbarWidth, scrollbarHeight, scrollbarTrackColor);

            // Draw scrollbar thumb
            renderer.fillRoundedRect(scrollbarX + 2, thumbY, scrollbarWidth - 4, thumbHeight, 3, scrollbarColor);
        }
    }

    private float renderNode(Renderer renderer, TreeNode node, int depth, float currentY, int contentWidth) {
        if (currentY + itemHeight < y || currentY > y + height) {
            // Skip rendering nodes outside visible area
            currentY += itemHeight;
        } else {
            float indent = depth * indentWidth + 4;

            // Draw selection/hover background
            float[] bgColor = null;
            if (node == selectedNode) {
                bgColor = selectedBackgroundColor;
            } else if (node == hoveredNode) {
                bgColor = hoveredBackgroundColor;
            }

            if (bgColor != null) {
                renderer.fillRect(x + 1, currentY, contentWidth, itemHeight, bgColor);
            }

            // Draw expand/collapse icon if node has children
            if (!node.getChildren().isEmpty()) {
                float iconX = x + indent;
                float iconY = currentY + itemHeight / 2;

                if (node.isExpanded()) {
                    // Down arrow for expanded
                    renderer.fillTriangle(iconX, iconY - 3, iconX + 8, iconY - 3, iconX + 4, iconY + 3, expandIconColor);
                } else {
                    // Right arrow for collapsed
                    renderer.fillTriangle(iconX, iconY - 4, iconX + 6, iconY, iconX, iconY + 4, expandIconColor);
                }
            }

            // Draw text - use getLabel() for interface compatibility
            String label = node.getLabel();
            float textX = x + indent + (node.getChildren().isEmpty() ? 4 : 14);

            renderer.drawText(textX, currentY + itemHeight / 2, label != null ? label : "(null)", textColor,
                    Renderer.ALIGN_LEFT | Renderer.ALIGN_MIDDLE);

            currentY += itemHeight;
        }

        // Render children if expanded
        if (node.isExpanded()) {
            for (TreeNode child : node.getChildren()) {
                currentY = renderNode(renderer, child, depth + 1, currentY, contentWidth);
            }
        }

        return currentY;
    }

    private float calculateTotalHeight() {
        float total = 0;
        for (TreeNode node : rootNodes) {
            total += calculateNodeHeight(node);
        }
        return total;
    }

    private float calculateNodeHeight(TreeNode node) {
        float height = itemHeight;
        if (node.isExpanded()) {
            for (TreeNode child : node.getChildren()) {
                height += calculateNodeHeight(child);
            }
        }
        return height;
    }

    private boolean needsScrollbar(float totalHeight) {
        return totalHeight > height;
    }

    @Override
    public boolean onMouseClick(int mx, int my, int button, int action) {
        if (!visible || !contains(mx, my)) {
            return false;
        }

        if (InputConstants.isLeftButton(button) && InputConstants.isPress(action)) {
            float totalHeight = calculateTotalHeight();
            int scrollbarWidth = needsScrollbar(totalHeight) ? 10 : 0;

            if (mx < x + width - scrollbarWidth) {
                TreeNode clicked = findNodeAtY(my + scrollOffset - y);
                if (clicked != null) {
                    // Check if click is on expand icon
                    int depth = getNodeDepth(clicked);
                    float iconX = x + depth * indentWidth + 4;
                    if (!clicked.getChildren().isEmpty() && mx >= iconX && mx <= iconX + 14) {
                        clicked.setExpanded(!clicked.isExpanded());
                    } else {
                        setSelectedNode(clicked);
                    }
                }
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean onMouseMove(int mx, int my) {
        if (!visible) {
            return false;
        }

        if (contains(mx, my)) {
            float totalHeight = calculateTotalHeight();
            int scrollbarWidth = needsScrollbar(totalHeight) ? 10 : 0;

            if (mx < x + width - scrollbarWidth) {
                hoveredNode = findNodeAtY(my + scrollOffset - y);
            } else {
                hoveredNode = null;
            }
            return true;
        } else {
            hoveredNode = null;
            return false;
        }
    }

    @Override
    public boolean onMouseScroll(int mx, int my, double scrollX, double scrollY) {
        if (!visible || !contains(mx, my)) {
            return false;
        }

        float totalHeight = calculateTotalHeight();
        float maxScroll = Math.max(0, totalHeight - height);

        scrollOffset -= (float) (scrollY * itemHeight * 2);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        return true;
    }

    private TreeNode findNodeAtY(float targetY) {
        float[] currentY = {0};
        for (TreeNode node : rootNodes) {
            TreeNode found = findNodeAtYRecursive(node, targetY, currentY);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private TreeNode findNodeAtYRecursive(TreeNode node, float targetY, float[] currentY) {
        if (targetY >= currentY[0] && targetY < currentY[0] + itemHeight) {
            return node;
        }
        currentY[0] += itemHeight;

        if (node.isExpanded()) {
            for (TreeNode child : node.getChildren()) {
                TreeNode found = findNodeAtYRecursive(child, targetY, currentY);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private int getNodeDepth(TreeNode target) {
        for (TreeNode root : rootNodes) {
            int depth = getNodeDepthRecursive(root, target, 0);
            if (depth >= 0) {
                return depth;
            }
        }
        return 0;
    }

    private int getNodeDepthRecursive(TreeNode node, TreeNode target, int depth) {
        if (node == target) {
            return depth;
        }
        for (TreeNode child : node.getChildren()) {
            int found = getNodeDepthRecursive(child, target, depth + 1);
            if (found >= 0) {
                return found;
            }
        }
        return -1;
    }

    /**
     * Log tree structure for debugging.
     */
    private void logTreeStructure(TreeNode node, int depth) {
        String indent = "  ".repeat(depth);
        String expandedStr = node.isExpanded() ? "[+]" : "[-]";
        log.debug("{}{}  '{}' ({} children)", indent, expandedStr, node.getLabel(), node.getChildren().size());

        // Only log first 3 levels in detail to avoid spam
        if (depth < 3 && node.isExpanded()) {
            for (TreeNode child : node.getChildren()) {
                logTreeStructure(child, depth + 1);
            }
        } else if (depth == 3 && !node.getChildren().isEmpty()) {
            log.debug("{}  ... {} more children", indent, node.getChildren().size());
        }
    }
}
