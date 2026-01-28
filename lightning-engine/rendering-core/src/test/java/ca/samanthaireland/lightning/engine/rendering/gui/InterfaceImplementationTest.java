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


package ca.samanthaireland.lightning.engine.rendering.gui;

import ca.samanthaireland.lightning.engine.rendering.render2d.*;
import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests verifying that GL implementations correctly implement their interfaces.
 */
class InterfaceImplementationTest {

    @Test
    void glButton_implementsButtonInterface() {
        Button button = new GLButton(0, 0, 100, 30, "Test");

        assertThat(button).isInstanceOf(Button.class);
        assertThat(button).isInstanceOf(WindowComponent.class);

        // Test interface methods
        button.setText("New Text");
        assertThat(button.getText()).isEqualTo("New Text");

        button.setFontSize(16.0f);
        button.setTextColor(Colour.WHITE);
        button.setBackgroundColor(Colour.BLUE);
        button.setHoverColor(Colour.ACCENT);
        button.setPressedColor(Colour.BLACK);
        button.setBorderColor(Colour.BORDER);
        button.setCornerRadius(8.0f);
    }

    @Test
    void glLabel_implementsLabelInterface() {
        Label label = new GLLabel(0, 0, "Test");

        assertThat(label).isInstanceOf(Label.class);
        assertThat(label).isInstanceOf(WindowComponent.class);

        // Test interface methods
        label.setText("New Text");
        assertThat(label.getText()).isEqualTo("New Text");

        label.setFontSize(18.0f);
        assertThat(label.getFontSize()).isEqualTo(18.0f);

        label.setTextColor(Colour.RED);
        assertThat(label.getTextColor()).isEqualTo(Colour.RED);

        label.setAlignment(0); // NVG_ALIGN_LEFT | NVG_ALIGN_TOP
    }

    @Test
    void glPanel_implementsPanelInterface() {
        Panel panel = new GLPanel(0, 0, 200, 150);

        assertThat(panel).isInstanceOf(Panel.class);
        assertThat(panel).isInstanceOf(WindowComponent.class);

        // Test interface methods
        GLButton child = new GLButton(10, 10, 50, 20, "Child");
        panel.addChild(child);
        assertThat(panel.getChildren()).hasSize(1);
        assertThat(panel.getChildren()).contains(child);

        panel.removeChild(child);
        assertThat(panel.getChildren()).isEmpty();

        panel.addChild(child);
        panel.clearChildren();
        assertThat(panel.getChildren()).isEmpty();

        panel.setBackgroundColor(Colour.PANEL_BG);
        panel.setBorderColor(Colour.BORDER);
        panel.setBorderWidth(2.0f);
        panel.setCornerRadius(6.0f);
        panel.setTitle("Test Panel");
        panel.setTitleFontSize(16.0f);
    }

    @Test
    void glTreeNode_implementsTreeNodeInterface() {
        TreeNode node = new GLTreeNode("Root");

        assertThat(node).isInstanceOf(TreeNode.class);

        // Test interface methods
        node.setLabel("New Label");
        assertThat(node.getLabel()).isEqualTo("New Label");

        TreeNode child = new GLTreeNode("Child");
        node.addChild(child);
        assertThat(node.getChildren()).hasSize(1);
        assertThat(node.hasChildren()).isTrue();

        node.setExpanded(true);
        assertThat(node.isExpanded()).isTrue();

        node.setUserData("test data");
        assertThat(node.getUserData()).isEqualTo("test data");

        node.removeChild(child);
        assertThat(node.getChildren()).isEmpty();
        assertThat(node.hasChildren()).isFalse();

        node.addChild(child);
        node.clearChildren();
        assertThat(node.getChildren()).isEmpty();
    }

    @Test
    void glTreeView_implementsTreeViewInterface() {
        TreeView treeView = new GLTreeView(0, 0, 200, 300);

        assertThat(treeView).isInstanceOf(TreeView.class);
        assertThat(treeView).isInstanceOf(WindowComponent.class);

        // Test interface methods
        TreeNode root = new GLTreeNode("Root");
        treeView.addRootNode(root);
        assertThat(treeView.getRootNodes()).hasSize(1);

        treeView.expandAll();
        treeView.collapseAll();

        treeView.setItemHeight(24);
        treeView.setIndentWidth(20);

        treeView.clearNodes();
        assertThat(treeView.getRootNodes()).isEmpty();
    }

    @Test
    void windowComponent_interfaceMethodsWork() {
        WindowComponent component = new GLButton(10, 20, 100, 50, "Test");

        assertThat(component.getX()).isEqualTo(10);
        assertThat(component.getY()).isEqualTo(20);
        assertThat(component.getWidth()).isEqualTo(100);
        assertThat(component.getHeight()).isEqualTo(50);

        component.setPosition(30, 40);
        assertThat(component.getX()).isEqualTo(30);
        assertThat(component.getY()).isEqualTo(40);

        component.setSize(150, 75);
        assertThat(component.getWidth()).isEqualTo(150);
        assertThat(component.getHeight()).isEqualTo(75);

        assertThat(component.isVisible()).isTrue();
        component.setVisible(false);
        assertThat(component.isVisible()).isFalse();

        // Test contains
        component.setPosition(100, 100);
        component.setSize(50, 50);
        component.setVisible(true);

        assertThat(component.contains(100, 100)).isTrue();
        assertThat(component.contains(125, 125)).isTrue();
        assertThat(component.contains(149, 149)).isTrue();
        assertThat(component.contains(150, 150)).isFalse();
        assertThat(component.contains(99, 100)).isFalse();
    }
}
