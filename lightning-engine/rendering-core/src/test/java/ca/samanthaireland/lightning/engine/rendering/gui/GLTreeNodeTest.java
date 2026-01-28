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

import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLTreeNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GLTreeNode}.
 */
@DisplayName("TreeNode")
class GLTreeNodeTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should create node with text")
        void shouldCreateNodeWithText() {
            GLTreeNode node = new GLTreeNode("Test Node");

            assertThat(node.getText()).isEqualTo("Test Node");
            assertThat(node.getUserData()).isNull();
            assertThat(node.isExpanded()).isFalse();
            assertThat(node.getChildren()).isEmpty();
        }

        @Test
        @DisplayName("should create node with text and userData")
        void shouldCreateNodeWithTextAndUserData() {
            Object userData = new Object();
            GLTreeNode node = new GLTreeNode("Test Node", userData);

            assertThat(node.getText()).isEqualTo("Test Node");
            assertThat(node.getUserData()).isSameAs(userData);
            assertThat(node.isExpanded()).isFalse();
        }
    }

    @Nested
    @DisplayName("text property")
    class TextProperty {

        @Test
        @DisplayName("should get and set text")
        void shouldGetAndSetText() {
            GLTreeNode node = new GLTreeNode("Initial");

            node.setText("Updated");

            assertThat(node.getText()).isEqualTo("Updated");
        }
    }

    @Nested
    @DisplayName("userData property")
    class UserDataProperty {

        @Test
        @DisplayName("should get and set userData")
        void shouldGetAndSetUserData() {
            GLTreeNode node = new GLTreeNode("Test");
            Object userData = "custom data";

            node.setUserData(userData);

            assertThat(node.getUserData()).isSameAs(userData);
        }

        @Test
        @DisplayName("should allow null userData")
        void shouldAllowNullUserData() {
            GLTreeNode node = new GLTreeNode("Test", "initial");

            node.setUserData(null);

            assertThat(node.getUserData()).isNull();
        }
    }

    @Nested
    @DisplayName("expanded property")
    class ExpandedProperty {

        @Test
        @DisplayName("should get and set expanded state")
        void shouldGetAndSetExpandedState() {
            GLTreeNode node = new GLTreeNode("Test");

            assertThat(node.isExpanded()).isFalse();

            node.setExpanded(true);
            assertThat(node.isExpanded()).isTrue();

            node.setExpanded(false);
            assertThat(node.isExpanded()).isFalse();
        }
    }

    @Nested
    @DisplayName("children management")
    class ChildrenManagement {

        @Test
        @DisplayName("should add child")
        void shouldAddChild() {
            GLTreeNode parent = new GLTreeNode("Parent");
            GLTreeNode child = new GLTreeNode("Child");

            parent.addChild(child);

            assertThat(parent.getChildren()).containsExactly(child);
            assertThat(parent.hasChildren()).isTrue();
            assertThat(parent.getChildCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should add multiple children")
        void shouldAddMultipleChildren() {
            GLTreeNode parent = new GLTreeNode("Parent");
            GLTreeNode child1 = new GLTreeNode("Child 1");
            GLTreeNode child2 = new GLTreeNode("Child 2");
            GLTreeNode child3 = new GLTreeNode("Child 3");

            parent.addChild(child1);
            parent.addChild(child2);
            parent.addChild(child3);

            assertThat(parent.getChildren()).containsExactly(child1, child2, child3);
            assertThat(parent.getChildCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should clear children")
        void shouldClearChildren() {
            GLTreeNode parent = new GLTreeNode("Parent");
            parent.addChild(new GLTreeNode("Child 1"));
            parent.addChild(new GLTreeNode("Child 2"));

            parent.clearChildren();

            assertThat(parent.getChildren()).isEmpty();
            assertThat(parent.hasChildren()).isFalse();
            assertThat(parent.getChildCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should report hasChildren correctly for empty node")
        void shouldReportHasChildrenCorrectlyForEmptyNode() {
            GLTreeNode node = new GLTreeNode("Empty");

            assertThat(node.hasChildren()).isFalse();
        }
    }

    @Nested
    @DisplayName("tree structure")
    class TreeStructure {

        @Test
        @DisplayName("should support nested tree structure")
        void shouldSupportNestedTreeStructure() {
            GLTreeNode root = new GLTreeNode("Root");
            GLTreeNode level1a = new GLTreeNode("Level 1A");
            GLTreeNode level1b = new GLTreeNode("Level 1B");
            GLTreeNode level2a = new GLTreeNode("Level 2A");
            GLTreeNode level2b = new GLTreeNode("Level 2B");

            root.addChild(level1a);
            root.addChild(level1b);
            level1a.addChild(level2a);
            level1a.addChild(level2b);

            assertThat(root.getChildCount()).isEqualTo(2);
            assertThat(level1a.getChildCount()).isEqualTo(2);
            assertThat(level1b.getChildCount()).isEqualTo(0);
            assertThat(level2a.getChildCount()).isEqualTo(0);
        }
    }
}
