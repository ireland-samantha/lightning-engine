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

import ca.samanthaireland.lightning.engine.rendering.render2d.Panel;
import ca.samanthaireland.lightning.engine.rendering.render2d.TextField;
import ca.samanthaireland.lightning.engine.rendering.render2d.WindowComponent;
import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLPanel;
import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLTextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for text field focus management.
 *
 * <p>Reproduces bug: when clicking between text fields, only one should have focus.
 */
@DisplayName("TextField Focus Management Tests")
class GLTextFieldFocusTest {

    private GLPanel panel;
    private GLTextField textField1;
    private GLTextField textField2;
    private GLTextField textField3;

    @BeforeEach
    void setUp() {
        // Create a panel with 3 text fields stacked vertically
        panel = new GLPanel(0, 0, 400, 300);

        textField1 = new GLTextField(10, 50, 200, 30);
        textField2 = new GLTextField(10, 90, 200, 30);
        textField3 = new GLTextField(10, 130, 200, 30);

        panel.addChild(textField1);
        panel.addChild(textField2);
        panel.addChild(textField3);
    }

    @Test
    @DisplayName("Clicking a text field should give it focus")
    void clickTextField_shouldGiveFocus() {
        // Click inside textField1
        panel.onMouseClick(100, 65, 0, 1);  // x=100 is inside field, y=65 is inside field1

        assertThat(textField1.isFocused()).as("TextField1 should be focused").isTrue();
        assertThat(textField2.isFocused()).as("TextField2 should not be focused").isFalse();
        assertThat(textField3.isFocused()).as("TextField3 should not be focused").isFalse();
    }

    @Test
    @DisplayName("BUG REPRODUCTION: Clicking another text field should remove focus from first")
    void clickAnotherTextField_shouldRemoveFocusFromFirst() {
        // Click inside textField1 first
        panel.onMouseClick(100, 65, 0, 1);
        assertThat(textField1.isFocused()).as("TextField1 should be focused after first click").isTrue();

        // Now click inside textField2
        panel.onMouseClick(100, 105, 0, 1);  // y=105 is inside field2

        // This is the bug: textField1 should lose focus when textField2 is clicked
        assertThat(textField1.isFocused()).as("TextField1 should NOT be focused after clicking TextField2").isFalse();
        assertThat(textField2.isFocused()).as("TextField2 should be focused").isTrue();
        assertThat(textField3.isFocused()).as("TextField3 should not be focused").isFalse();
    }

    @Test
    @DisplayName("Only one text field should be focused at a time")
    void onlyOneTextFieldFocused_atATime() {
        // Click each text field in sequence
        panel.onMouseClick(100, 65, 0, 1);  // Click field1
        assertThat(countFocusedFields()).as("Only one field should be focused after clicking field1").isEqualTo(1);
        assertThat(textField1.isFocused()).isTrue();

        panel.onMouseClick(100, 105, 0, 1);  // Click field2
        assertThat(countFocusedFields()).as("Only one field should be focused after clicking field2").isEqualTo(1);
        assertThat(textField2.isFocused()).isTrue();

        panel.onMouseClick(100, 145, 0, 1);  // Click field3
        assertThat(countFocusedFields()).as("Only one field should be focused after clicking field3").isEqualTo(1);
        assertThat(textField3.isFocused()).isTrue();
    }

    @Test
    @DisplayName("Clicking outside all text fields should remove focus")
    void clickOutsideAllFields_shouldRemoveFocus() {
        // First focus a field
        panel.onMouseClick(100, 65, 0, 1);
        assertThat(textField1.isFocused()).isTrue();

        // Click on the panel but outside all text fields
        panel.onMouseClick(100, 200, 0, 1);  // y=200 is below all fields

        assertThat(textField1.isFocused()).as("TextField1 should lose focus").isFalse();
        assertThat(textField2.isFocused()).as("TextField2 should not be focused").isFalse();
        assertThat(textField3.isFocused()).as("TextField3 should not be focused").isFalse();
    }

    @Test
    @DisplayName("Typing in focused field should not affect other fields")
    void typingInFocusedField_shouldNotAffectOthers() {
        // Focus field1 and type
        panel.onMouseClick(100, 65, 0, 1);
        textField1.onCharInput('a');
        textField1.onCharInput('b');
        textField1.onCharInput('c');

        assertThat(textField1.getText()).isEqualTo("abc");
        assertThat(textField2.getText()).isEmpty();
        assertThat(textField3.getText()).isEmpty();

        // Now click field2 and type
        panel.onMouseClick(100, 105, 0, 1);
        textField2.onCharInput('x');
        textField2.onCharInput('y');
        textField2.onCharInput('z');

        assertThat(textField1.getText()).as("Field1 should retain its text").isEqualTo("abc");
        assertThat(textField2.getText()).as("Field2 should have new text").isEqualTo("xyz");
        assertThat(textField3.getText()).isEmpty();
    }

    private int countFocusedFields() {
        int count = 0;
        if (textField1.isFocused()) count++;
        if (textField2.isFocused()) count++;
        if (textField3.isFocused()) count++;
        return count;
    }
}
