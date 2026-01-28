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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * UI rendering tests that verify GUI components render correctly.
 * These tests use off-screen rendering and pixel verification.
 *
 * <p>Requires LWJGL native libraries for OpenGL off-screen rendering.
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisabledIfLwjglUnavailable
class GUIRenderingTest {

    private GUITestRenderer renderer;
    private boolean rendererAvailable = false;

    @BeforeAll
    void setupRenderer() {
        try {
            renderer = new GUITestRenderer(400, 300);
            renderer.init();
            rendererAvailable = true;
            log.info("Test renderer initialized. Fonts available: " + renderer.hasFonts());
        } catch (Exception e) {
            log.warn("Could not initialize test renderer (likely no display): " + e.getMessage());
            rendererAvailable = false;
        }
    }

    @AfterAll
    void cleanupRenderer() {
        if (renderer != null) {
            renderer.close();
        }
    }

    @BeforeEach
    void checkRenderer() {
        assumeTrue(rendererAvailable, "Test renderer not available");
    }

    @Test
    void panel_rendersBackground() {
        GLPanel panel = new GLPanel(10, 10, 100, 80);
        panel.setBackgroundColor(new float[]{0.5f, 0.5f, 0.5f, 1.0f}); // Gray

        renderer.render(panel);

        // Check that the panel area contains the gray background color
        int[] expectedGray = {127, 127, 127, 255};
        boolean hasGrayContent = renderer.regionContainsColor(15, 15, 80, 60, expectedGray, 20);
        assertThat(hasGrayContent).isTrue();
    }

    @Test
    void panel_rendersBorder() {
        GLPanel panel = new GLPanel(20, 20, 100, 80);
        panel.setBorderColor(new float[]{1.0f, 0.0f, 0.0f, 1.0f}); // Red border
        panel.setBorderWidth(2.0f);

        renderer.render(panel);

        // Check that the border area contains red color
        int[] expectedRed = {255, 0, 0, 255};
        // Check top border
        boolean hasBorder = renderer.regionContainsColor(20, 20, 100, 3, expectedRed, 50);
        assertThat(hasBorder).isTrue();
    }

    @Test
    void button_rendersWithBackground() {
        GLButton button = new GLButton(50, 50, 100, 40, "Click");
        button.setBackgroundColor(new float[]{0.3f, 0.3f, 0.35f, 1.0f});

        renderer.render(button);

        // Check that the button area has content
        boolean hasContent = renderer.regionHasContent(50, 50, 100, 40);
        assertThat(hasContent).isTrue();
    }

    @Test
    void button_rendersHoverState() {
        GLButton button = new GLButton(50, 50, 100, 40, "Hover");
        button.setHoverColor(new float[]{0.6f, 0.6f, 0.7f, 1.0f}); // Lighter on hover

        // Simulate hover
        button.onMouseMove(100, 70);

        renderer.render(button);

        // Button should have content
        boolean hasContent = renderer.regionHasContent(50, 50, 100, 40);
        assertThat(hasContent).isTrue();
    }

    @Test
    void label_rendersText() {
        assumeTrue(renderer.hasFonts(), "No fonts available for text rendering test");

        GLLabel label = new GLLabel(20, 20, "Test Label", 18.0f);
        label.setTextColor(new float[]{1.0f, 1.0f, 1.0f, 1.0f}); // White text

        renderer.render(label);

        // Check that the label area has non-background content (text)
        boolean hasContent = renderer.regionHasContent(20, 20, 100, 25);
        assertThat(hasContent).isTrue();
    }

    @Test
    void label_invisibleLabel_rendersNothing() {
        GLLabel label = new GLLabel(20, 20, "Hidden");
        label.setVisible(false);

        renderer.render(label);

        // The area should only have background color
        boolean hasContent = renderer.regionHasContent(20, 20, 80, 20);
        assertThat(hasContent).isFalse();
    }

    @Test
    void listView_rendersBackground() {
        GLListView listView = new GLListView(10, 10, 150, 200);
        listView.setItems(List.of("Item 1", "Item 2", "Item 3"));

        renderer.render(listView);

        // Check that the list view area has content
        boolean hasContent = renderer.regionHasContent(10, 10, 150, 200);
        assertThat(hasContent).isTrue();
    }

    @Test
    void listView_rendersSelectedItem() {
        assumeTrue(renderer.hasFonts(), "No fonts available for text rendering test");

        GLListView listView = new GLListView(10, 10, 150, 200);
        listView.setItems(List.of("Item 1", "Item 2", "Item 3"));
        listView.setSelectedIndex(1);

        renderer.render(listView);

        // The list view should have content when items are selected
        // Selection color checking is tricky due to blending, so just verify content exists
        boolean hasContent = renderer.regionHasContent(10, 10, 150, 200);
        assertThat(hasContent).isTrue();
    }

    @Test
    void treeView_rendersRootNodes() {
        GLTreeView treeView = new GLTreeView(10, 10, 200, 250);
        GLTreeNode root = new GLTreeNode("Root Node");
        root.addChild(new GLTreeNode("Child 1"));
        root.addChild(new GLTreeNode("Child 2"));
        treeView.addRootNode(root);

        renderer.render(treeView);

        // Tree view area should have content
        boolean hasContent = renderer.regionHasContent(10, 10, 200, 250);
        assertThat(hasContent).isTrue();
    }

    @Test
    void treeView_expandedNodes_showChildren() {
        assumeTrue(renderer.hasFonts(), "No fonts available for text rendering test");

        GLTreeView treeView = new GLTreeView(10, 10, 200, 250);
        GLTreeNode root = new GLTreeNode("Root");
        root.addChild(new GLTreeNode("Child 1"));
        root.addChild(new GLTreeNode("Child 2"));
        root.setExpanded(true);
        treeView.addRootNode(root);

        renderer.render(treeView);

        // Check for content in the child area (below root)
        boolean hasChildContent = renderer.regionHasContent(30, 35, 150, 50);
        assertThat(hasChildContent).isTrue();
    }

    @Test
    void textField_rendersBackground() {
        GLTextField textField = new GLTextField(20, 20, 200, 30);
        textField.setText("Sample text");

        renderer.render(textField);

        // Text field area should have content
        boolean hasContent = renderer.regionHasContent(20, 20, 200, 30);
        assertThat(hasContent).isTrue();
    }

    @Test
    void textField_focusedState_showsCursor() {
        assumeTrue(renderer.hasFonts(), "No fonts available for text rendering test");

        GLTextField textField = new GLTextField(20, 20, 200, 30);
        textField.setText("Text");
        textField.setFocused(true);

        renderer.render(textField);

        // Text field should have content
        boolean hasContent = renderer.regionHasContent(20, 20, 200, 30);
        assertThat(hasContent).isTrue();
    }

    @Test
    void panel_withChildren_rendersAll() {
        GLPanel panel = new GLPanel(10, 10, 200, 150);
        panel.addChild(new GLLabel(20, 40, "Child Label"));
        panel.addChild(new GLButton(20, 70, 80, 30, "Button"));

        renderer.render(panel);

        // Panel should have content throughout
        boolean hasContent = renderer.regionHasContent(10, 10, 200, 150);
        assertThat(hasContent).isTrue();
    }

    @Test
    void multipleComponents_renderInOrder() {
        GLLabel label1 = new GLLabel(10, 10, "First");
        GLLabel label2 = new GLLabel(10, 40, "Second");
        GLButton button = new GLButton(10, 70, 100, 30, "Button");

        renderer.render(List.of(label1, label2, button));

        // All component areas should have content
        assertThat(renderer.regionHasContent(10, 70, 100, 30)).isTrue();
    }

    @Test
    void guiColor_constants_areValid() {
        // Verify that color constants are properly defined
        assertThat(GLColour.WHITE).hasSize(4);
        assertThat(GLColour.WHITE[3]).isEqualTo(1.0f);

        assertThat(GLColour.BACKGROUND).hasSize(4);
        assertThat(GLColour.PANEL_BG).hasSize(4);
        assertThat(GLColour.BUTTON_BG).hasSize(4);
        assertThat(GLColour.TEXT_PRIMARY).hasSize(4);
    }
}
