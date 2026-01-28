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

import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLButton;
import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLLabel;
import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLPanel;
import org.junit.jupiter.api.*;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GLPanel scrolling functionality.
 *
 * <p>Run with:
 * <pre>
 * ./mvnw test -pl lightning-engine/rendering-core -Dtest=GLPanelScrollTest
 * </pre>
 */
@Tag("unit")
@DisplayName("GLPanel Scroll Tests")
class GLPanelScrollTest {

    private GLPanel panel;

    @BeforeEach
    void setUp() {
        // Create a 200x200 panel
        panel = new GLPanel(0, 0, 200, 200);
    }

    // ========== Scroll State Tests ==========

    @Test
    @DisplayName("scrolling is disabled by default")
    void scrolling_isDisabledByDefault() {
        assertThat(panel.isScrollableHorizontal()).isFalse();
        assertThat(panel.isScrollableVertical()).isFalse();
    }

    @Test
    @DisplayName("setScrollable enables scrolling")
    void setScrollable_enablesScrolling() {
        panel.setScrollable(true, true);

        assertThat(panel.isScrollableHorizontal()).isTrue();
        assertThat(panel.isScrollableVertical()).isTrue();
    }

    @Test
    @DisplayName("setScrollable can enable only horizontal")
    void setScrollable_canEnableOnlyHorizontal() {
        panel.setScrollable(true, false);

        assertThat(panel.isScrollableHorizontal()).isTrue();
        assertThat(panel.isScrollableVertical()).isFalse();
    }

    @Test
    @DisplayName("setScrollable can enable only vertical")
    void setScrollable_canEnableOnlyVertical() {
        panel.setScrollable(false, true);

        assertThat(panel.isScrollableHorizontal()).isFalse();
        assertThat(panel.isScrollableVertical()).isTrue();
    }

    @Test
    @DisplayName("scroll position starts at zero")
    void scrollPosition_startsAtZero() {
        assertThat(panel.getScrollX()).isEqualTo(0);
        assertThat(panel.getScrollY()).isEqualTo(0);
    }

    @Test
    @DisplayName("setScrollPosition updates scroll position")
    void setScrollPosition_updatesScrollPosition() {
        panel.setScrollable(true, true);
        // Add content larger than panel
        panel.addChild(new GLLabel(300, 300, "Far away"));

        panel.setScrollPosition(50, 75);

        assertThat(panel.getScrollX()).isEqualTo(50);
        assertThat(panel.getScrollY()).isEqualTo(75);
    }

    @Test
    @DisplayName("setScrollPosition clamps to valid range")
    void setScrollPosition_clampsToValidRange() {
        panel.setScrollable(true, true);
        // Panel is 200x200, add 300x300 content
        panel.addChild(new GLLabel(250, 250, "Far"));

        // Try to scroll past the maximum
        panel.setScrollPosition(500, 500);

        // Should be clamped to max scroll (content - viewport)
        assertThat(panel.getScrollX()).isLessThanOrEqualTo(300);
        assertThat(panel.getScrollY()).isLessThanOrEqualTo(300);
    }

    @Test
    @DisplayName("setScrollPosition cannot go negative")
    void setScrollPosition_cannotGoNegative() {
        panel.setScrollable(true, true);

        panel.setScrollPosition(-100, -50);

        assertThat(panel.getScrollX()).isEqualTo(0);
        assertThat(panel.getScrollY()).isEqualTo(0);
    }

    // ========== Content Size Tests ==========

    @Test
    @DisplayName("getContentWidth returns maximum child right edge")
    void getContentWidth_returnsMaxChildRightEdge() {
        // Use buttons which have explicit width
        panel.addChild(new GLButton(10, 10, 50, 30, "A")); // right edge at 60
        panel.addChild(new GLButton(150, 20, 100, 30, "B")); // right edge at 250

        int contentWidth = panel.getContentWidth();

        assertThat(contentWidth).isEqualTo(250);
    }

    @Test
    @DisplayName("getContentHeight returns maximum child bottom edge")
    void getContentHeight_returnsMaxChildBottomEdge() {
        panel.addChild(new GLLabel(10, 10, "Top"));
        panel.addChild(new GLLabel(10, 250, "Bottom")); // at y=250

        int contentHeight = panel.getContentHeight();

        assertThat(contentHeight).isGreaterThanOrEqualTo(250);
    }

    @Test
    @DisplayName("getContentWidth returns 0 for empty panel")
    void getContentWidth_returnsZeroForEmptyPanel() {
        assertThat(panel.getContentWidth()).isEqualTo(0);
    }

    @Test
    @DisplayName("getContentHeight returns 0 for empty panel")
    void getContentHeight_returnsZeroForEmptyPanel() {
        assertThat(panel.getContentHeight()).isEqualTo(0);
    }

    // ========== Mouse Scroll Tests ==========

    @Test
    @DisplayName("onMouseScroll updates scroll position when scrollable")
    void onMouseScroll_updatesScrollPositionWhenScrollable() {
        panel.setScrollable(false, true);
        // Add content larger than panel
        panel.addChild(new GLLabel(10, 500, "Far down"));

        // Scroll down (negative scrollY delta means scroll content up/scroll position down)
        panel.onMouseScroll(100, 100, 0, -1);

        assertThat(panel.getScrollY()).isGreaterThan(0);
    }

    @Test
    @DisplayName("onMouseScroll does nothing when not scrollable")
    void onMouseScroll_doesNothingWhenNotScrollable() {
        // Add content larger than panel
        panel.addChild(new GLLabel(10, 500, "Far down"));

        // Try to scroll (but scrolling is disabled)
        boolean handled = panel.onMouseScroll(100, 100, 0, -1);

        assertThat(handled).isFalse();
        assertThat(panel.getScrollY()).isEqualTo(0);
    }

    @Test
    @DisplayName("onMouseScroll returns false when mouse is outside panel")
    void onMouseScroll_returnsFalseWhenMouseOutside() {
        panel.setScrollable(false, true);
        panel.addChild(new GLLabel(10, 500, "Far down"));

        boolean handled = panel.onMouseScroll(500, 500, 0, -1);

        assertThat(handled).isFalse();
    }

    // ========== scrollToChild Tests ==========

    @Test
    @DisplayName("scrollToChild makes child visible")
    void scrollToChild_makesChildVisible() {
        panel.setScrollable(false, true);
        GLLabel farLabel = new GLLabel(10, 400, "Far down");
        panel.addChild(farLabel);

        panel.scrollToChild(farLabel);

        // After scrolling, the label should be in view
        // This means scrollY should be adjusted so label is visible
        int viewportHeight = 200; // panel height
        assertThat(panel.getScrollY()).isGreaterThan(0);
        assertThat(panel.getScrollY()).isLessThanOrEqualTo(400);
    }

    @Test
    @DisplayName("scrollToChild does nothing for unknown child")
    void scrollToChild_doesNothingForUnknownChild() {
        panel.setScrollable(false, true);
        GLLabel unknownLabel = new GLLabel(10, 400, "Not in panel");

        panel.scrollToChild(unknownLabel);

        assertThat(panel.getScrollY()).isEqualTo(0);
    }

    // ========== Mouse Click with Scroll Offset Tests ==========

    @Test
    @DisplayName("mouse click adjusts for scroll offset")
    void mouseClick_adjustsForScrollOffset() {
        panel.setScrollable(false, true);

        // Add content to make panel scrollable (need content height > viewport)
        // Panel is 200x200, so add some filler content first
        panel.addChild(new GLButton(10, 400, 50, 30, "Filler")); // extends content to y=430

        // Create a button at y=150 (within first scroll)
        AtomicBoolean clicked = new AtomicBoolean(false);
        GLButton button = new GLButton(50, 150, 100, 30, "Click me");
        button.setOnClick(() -> clicked.set(true));
        panel.addChild(button);

        // Scroll down by 100
        panel.setScrollPosition(0, 100);

        // Button at y=150 should now appear at screen y=50 (150 - 100 scroll)
        // Click at y=50 within panel, x=100 (within button x range 50-150)
        // GLButton requires press (action=1) followed by release (action=0) to trigger onClick
        panel.onMouseClick(100, 50, 0, 1); // Press
        panel.onMouseClick(100, 50, 0, 0); // Release

        assertThat(clicked.get()).isTrue();
    }

    // ========== Panel Properties Tests ==========

    @Test
    @DisplayName("panel children can be added and retrieved")
    void children_canBeAddedAndRetrieved() {
        GLLabel label1 = new GLLabel(10, 10, "Label 1");
        GLLabel label2 = new GLLabel(10, 30, "Label 2");

        panel.addChild(label1);
        panel.addChild(label2);

        assertThat(panel.getChildren()).hasSize(2);
        assertThat(panel.getChildren()).contains(label1, label2);
    }

    @Test
    @DisplayName("removeChild removes the child")
    void removeChild_removesTheChild() {
        GLLabel label = new GLLabel(10, 10, "Label");
        panel.addChild(label);

        panel.removeChild(label);

        assertThat(panel.getChildren()).isEmpty();
    }

    @Test
    @DisplayName("clearChildren removes all children")
    void clearChildren_removesAllChildren() {
        panel.addChild(new GLLabel(10, 10, "Label 1"));
        panel.addChild(new GLLabel(10, 30, "Label 2"));

        panel.clearChildren();

        assertThat(panel.getChildren()).isEmpty();
    }
}
