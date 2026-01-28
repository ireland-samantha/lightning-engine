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

import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class WindowComponentTest {

    @Test
    void label_createsWithCorrectProperties() {
        GLLabel label = new GLLabel(10, 20, "Test Text");

        assertThat(label.getX()).isEqualTo(10);
        assertThat(label.getY()).isEqualTo(20);
        assertThat(label.getText()).isEqualTo("Test Text");
        assertThat(label.isVisible()).isTrue();
    }

    @Test
    void label_setText_updatesText() {
        GLLabel label = new GLLabel(0, 0, "Initial");
        label.setText("Updated");

        assertThat(label.getText()).isEqualTo("Updated");
    }

    @Test
    void label_setFontSize_updatesHeight() {
        GLLabel label = new GLLabel(0, 0, "Test", 16.0f);
        assertThat(label.getHeight()).isEqualTo(16);

        label.setFontSize(24.0f);
        assertThat(label.getFontSize()).isEqualTo(24.0f);
        assertThat(label.getHeight()).isEqualTo(24);
    }

    @Test
    void button_createsWithCorrectProperties() {
        GLButton button = new GLButton(10, 20, 100, 30, "Click Me");

        assertThat(button.getX()).isEqualTo(10);
        assertThat(button.getY()).isEqualTo(20);
        assertThat(button.getWidth()).isEqualTo(100);
        assertThat(button.getHeight()).isEqualTo(30);
        assertThat(button.getText()).isEqualTo("Click Me");
    }

    @Test
    void button_onClick_executesCallback() {
        GLButton button = new GLButton(0, 0, 100, 30, "Test");
        AtomicInteger clickCount = new AtomicInteger(0);
        button.setOnClick(clickCount::incrementAndGet);

        // Simulate click (press then release inside button)
        button.onMouseClick(50, 15, 0, 1); // Press
        button.onMouseClick(50, 15, 0, 0); // Release

        assertThat(clickCount.get()).isEqualTo(1);
    }

    @Test
    void button_onClick_doesNotExecute_whenReleasedOutside() {
        GLButton button = new GLButton(0, 0, 100, 30, "Test");
        AtomicInteger clickCount = new AtomicInteger(0);
        button.setOnClick(clickCount::incrementAndGet);

        // Press inside, release outside
        button.onMouseClick(50, 15, 0, 1); // Press
        button.onMouseMove(200, 200); // Move outside
        button.onMouseClick(200, 200, 0, 0); // Release outside

        assertThat(clickCount.get()).isZero();
    }

    @Test
    void button_hover_updatesState() {
        GLButton button = new GLButton(0, 0, 100, 30, "Test");

        // Move inside
        boolean handled = button.onMouseMove(50, 15);
        assertThat(handled).isTrue();

        // Move outside
        handled = button.onMouseMove(200, 200);
        assertThat(handled).isTrue(); // State changed
    }

    @Test
    void panel_addsAndRemovesChildren() {
        GLPanel panel = new GLPanel(0, 0, 200, 200);
        GLLabel child = new GLLabel(10, 10, "Child");

        panel.addChild(child);
        assertThat(panel.getChildren()).hasSize(1);
        assertThat(panel.getChildren().getFirst()).isEqualTo(child);

        panel.removeChild(child);
        assertThat(panel.getChildren()).isEmpty();
    }

    @Test
    void panel_clearChildren_removesAll() {
        GLPanel panel = new GLPanel(0, 0, 200, 200);
        panel.addChild(new GLLabel(10, 10, "Child1"));
        panel.addChild(new GLLabel(20, 20, "Child2"));
        panel.addChild(new GLLabel(30, 30, "Child3"));

        assertThat(panel.getChildren()).hasSize(3);

        panel.clearChildren();
        assertThat(panel.getChildren()).isEmpty();
    }

    @Test
    void panel_propagatesMouseClick_toChildren() {
        GLPanel panel = new GLPanel(0, 0, 200, 200);
        GLButton button = new GLButton(10, 30, 80, 25, "Test");
        AtomicInteger clickCount = new AtomicInteger(0);
        button.setOnClick(clickCount::incrementAndGet);
        panel.addChild(button);

        // Click inside button
        panel.onMouseClick(50, 42, 0, 1);
        panel.onMouseClick(50, 42, 0, 0);

        assertThat(clickCount.get()).isEqualTo(1);
    }

    @Test
    void listView_setItems_updatesItems() {
        GLListView listView = new GLListView(0, 0, 200, 300);
        listView.setItems(List.of("Item 1", "Item 2", "Item 3"));

        assertThat(listView.getItems()).hasSize(3);
        assertThat(listView.getItems().get(0)).isEqualTo("Item 1");
    }

    @Test
    void listView_addItem_appendsItem() {
        GLListView listView = new GLListView(0, 0, 200, 300);
        listView.addItem("First");
        listView.addItem("Second");

        assertThat(listView.getItems()).hasSize(2);
    }

    @Test
    void listView_setSelectedIndex_updatesSelection() {
        GLListView listView = new GLListView(0, 0, 200, 300);
        listView.setItems(List.of("A", "B", "C"));
        AtomicInteger selectedIndex = new AtomicInteger(-1);
        listView.setOnSelectionChanged(selectedIndex::set);

        listView.setSelectedIndex(1);

        assertThat(listView.getSelectedIndex()).isEqualTo(1);
        assertThat(listView.getSelectedItem()).isEqualTo("B");
        assertThat(selectedIndex.get()).isEqualTo(1);
    }

    @Test
    void listView_clearItems_resetsSelection() {
        GLListView listView = new GLListView(0, 0, 200, 300);
        listView.setItems(List.of("A", "B", "C"));
        listView.setSelectedIndex(1);

        listView.clearItems();

        assertThat(listView.getItems()).isEmpty();
        assertThat(listView.getSelectedIndex()).isEqualTo(-1);
    }

    @Test
    void treeView_addRootNode_addsNode() {
        GLTreeView treeView = new GLTreeView(0, 0, 200, 300);
        GLTreeNode node = new GLTreeNode("Root");

        treeView.addRootNode(node);

        assertThat(treeView.getRootNodes()).hasSize(1);
        assertThat(treeView.getRootNodes().getFirst()).isEqualTo(node);
    }

    @Test
    void treeNode_addChild_createsHierarchy() {
        GLTreeNode root = new GLTreeNode("Root");
        GLTreeNode child1 = new GLTreeNode("Child 1");
        GLTreeNode child2 = new GLTreeNode("Child 2");

        root.addChild(child1);
        root.addChild(child2);

        assertThat(root.getChildren()).hasSize(2);
    }

    @Test
    void treeNode_setExpanded_togglesState() {
        GLTreeNode node = new GLTreeNode("Test");

        assertThat(node.isExpanded()).isFalse();

        node.setExpanded(true);
        assertThat(node.isExpanded()).isTrue();

        node.setExpanded(false);
        assertThat(node.isExpanded()).isFalse();
    }

    @Test
    void treeNode_userData_storesCustomData() {
        Object customData = new Object();
        GLTreeNode node = new GLTreeNode("Test", customData);

        assertThat(node.getUserData()).isEqualTo(customData);

        Object newData = "New Data";
        node.setUserData(newData);
        assertThat(node.getUserData()).isEqualTo(newData);
    }

    @Test
    void textField_getText_returnsText() {
        GLTextField field = new GLTextField(0, 0, 200, 30);

        assertThat(field.getText()).isEmpty();

        field.setText("Hello");
        assertThat(field.getText()).isEqualTo("Hello");
    }

    @Test
    void textField_onCharInput_appendsCharacter() {
        GLTextField field = new GLTextField(0, 0, 200, 30);
        field.setFocused(true);

        field.onCharInput('H');
        field.onCharInput('i');

        assertThat(field.getText()).isEqualTo("Hi");
    }

    @Test
    void textField_onCharInput_ignoresWhenNotFocused() {
        GLTextField field = new GLTextField(0, 0, 200, 30);
        field.setFocused(false);

        field.onCharInput('X');

        assertThat(field.getText()).isEmpty();
    }

    @Test
    void textField_onTextChanged_callsCallback() {
        GLTextField field = new GLTextField(0, 0, 200, 30);
        List<String> changes = new ArrayList<>();
        field.setOnTextChanged(changes::add);
        field.setFocused(true);

        field.onCharInput('A');
        field.onCharInput('B');

        assertThat(changes).containsExactly("A", "AB");
    }

    @Test
    void abstractComponent_contains_checksCorrectly() {
        GLLabel label = new GLLabel(10, 20, "Test");
        label.setSize(100, 30);

        assertThat(label.contains(50, 35)).isTrue();
        assertThat(label.contains(10, 20)).isTrue();
        assertThat(label.contains(109, 49)).isTrue();
        assertThat(label.contains(110, 35)).isFalse();
        assertThat(label.contains(5, 35)).isFalse();
    }

    @Test
    void abstractComponent_setPosition_updatesPosition() {
        GLLabel label = new GLLabel(0, 0, "Test");
        label.setPosition(100, 200);

        assertThat(label.getX()).isEqualTo(100);
        assertThat(label.getY()).isEqualTo(200);
    }

    @Test
    void guiColor_withAlpha_createsNewColor() {
        float[] original = GLColour.WHITE;
        float[] modified = GLColour.withAlpha(original, 0.5f);

        assertThat(modified[3]).isEqualTo(0.5f);
        assertThat(original[3]).isEqualTo(1.0f); // Original unchanged
    }

    @Test
    void guiColor_blend_blendsTwoColors() {
        float[] c1 = {0.0f, 0.0f, 0.0f, 1.0f};
        float[] c2 = {1.0f, 1.0f, 1.0f, 1.0f};

        float[] blended = GLColour.blend(c1, c2, 0.5f);

        assertThat(blended[0]).isEqualTo(0.5f);
        assertThat(blended[1]).isEqualTo(0.5f);
        assertThat(blended[2]).isEqualTo(0.5f);
    }
}
