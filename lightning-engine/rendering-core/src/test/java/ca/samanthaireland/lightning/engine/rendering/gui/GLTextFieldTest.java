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

import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLTextField;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Tests for GLTextField including copy/paste functionality.
 *
 * <p>Run with:
 * <pre>
 * ./mvnw test -pl lightning-engine/rendering-core -Dtest=GLTextFieldTest -DenableGLTests=true
 * </pre>
 *
 * <p>Note: This test class imports GLFW classes which require native libraries.
 * Although most tests only use compile-time constants, the class import still
 * requires LWJGL to be available for class verification.
 */
@Tag("integration")
@DisplayName("GLTextField Tests")
@DisabledIfLwjglUnavailable
class GLTextFieldTest {

    private GLTextField textField;

    @BeforeEach
    void setUp() {
        textField = new GLTextField(10, 10, 200, 30);
    }

    // ========== Basic functionality tests (no OpenGL required) ==========

    @Test
    @DisplayName("getText returns empty string initially")
    void getText_returnsEmptyStringInitially() {
        assertThat(textField.getText()).isEmpty();
    }

    @Test
    @DisplayName("setText updates text content")
    void setText_updatesTextContent() {
        textField.setText("Hello World");
        assertThat(textField.getText()).isEqualTo("Hello World");
    }

    @Test
    @DisplayName("setText with null sets empty string")
    void setText_withNull_setsEmptyString() {
        textField.setText("test");
        textField.setText(null);
        assertThat(textField.getText()).isEmpty();
    }

    @Test
    @DisplayName("setPlaceholder updates placeholder text")
    void setPlaceholder_updatesPlaceholder() {
        textField.setPlaceholder("Enter text...");
        assertThat(textField.getPlaceholder()).isEqualTo("Enter text...");
    }

    @Test
    @DisplayName("isFocused returns false initially")
    void isFocused_returnsFalseInitially() {
        assertThat(textField.isFocused()).isFalse();
    }

    @Test
    @DisplayName("setFocused changes focus state")
    void setFocused_changesFocusState() {
        textField.setFocused(true);
        assertThat(textField.isFocused()).isTrue();

        textField.setFocused(false);
        assertThat(textField.isFocused()).isFalse();
    }

    @Test
    @DisplayName("onTextChanged callback is called when text changes via setText")
    void onTextChanged_calledWhenTextChanges() {
        AtomicReference<String> receivedText = new AtomicReference<>();
        textField.setOnTextChanged(receivedText::set);

        textField.setText("Test");

        assertThat(receivedText.get()).isEqualTo("Test");
    }

    @Test
    @DisplayName("onSubmit callback is called on Enter key")
    void onSubmit_calledOnEnterKey() {
        AtomicReference<String> submittedText = new AtomicReference<>();
        textField.setOnSubmit(submittedText::set);
        textField.setText("Submit me");
        textField.setFocused(true);

        // Simulate Enter key press
        textField.onKeyPress(GLFW_KEY_ENTER, 1, 0);

        assertThat(submittedText.get()).isEqualTo("Submit me");
    }

    @Test
    @DisplayName("Backspace deletes character before cursor")
    void backspace_deletesCharacterBeforeCursor() {
        textField.setText("Hello");
        textField.setFocused(true);

        // Cursor is at end (5), backspace should delete 'o'
        textField.onKeyPress(GLFW_KEY_BACKSPACE, 1, 0);

        assertThat(textField.getText()).isEqualTo("Hell");
    }

    @Test
    @DisplayName("Delete key removes character at cursor")
    void delete_removesCharacterAtCursor() {
        textField.setText("Hello");
        textField.setFocused(true);

        // Move cursor to beginning
        textField.onKeyPress(GLFW_KEY_HOME, 1, 0);

        // Delete should remove 'H'
        textField.onKeyPress(GLFW_KEY_DELETE, 1, 0);

        assertThat(textField.getText()).isEqualTo("ello");
    }

    @Test
    @DisplayName("Arrow keys move cursor")
    void arrowKeys_moveCursor() {
        textField.setText("Test");
        textField.setFocused(true);

        // Cursor starts at end (4)
        // Left should move to position 3
        textField.onKeyPress(GLFW_KEY_LEFT, 1, 0);

        // Now delete should remove 't' (the last character)
        textField.onKeyPress(GLFW_KEY_DELETE, 1, 0);

        assertThat(textField.getText()).isEqualTo("Tes");
    }

    @Test
    @DisplayName("Home key moves cursor to beginning")
    void homeKey_movesCursorToBeginning() {
        textField.setText("Hello");
        textField.setFocused(true);

        textField.onKeyPress(GLFW_KEY_HOME, 1, 0);

        // Delete should now remove 'H'
        textField.onKeyPress(GLFW_KEY_DELETE, 1, 0);

        assertThat(textField.getText()).isEqualTo("ello");
    }

    @Test
    @DisplayName("End key moves cursor to end")
    void endKey_movesCursorToEnd() {
        textField.setText("Hello");
        textField.setFocused(true);

        // Move to beginning first
        textField.onKeyPress(GLFW_KEY_HOME, 1, 0);
        // Then to end
        textField.onKeyPress(GLFW_KEY_END, 1, 0);

        // Backspace should remove 'o'
        textField.onKeyPress(GLFW_KEY_BACKSPACE, 1, 0);

        assertThat(textField.getText()).isEqualTo("Hell");
    }

    @Test
    @DisplayName("Character input adds character at cursor position")
    void charInput_addsCharacterAtCursor() {
        textField.setText("Hllo");
        textField.setFocused(true);

        // Move cursor to position 1 (after 'H')
        textField.onKeyPress(GLFW_KEY_HOME, 1, 0);
        textField.onKeyPress(GLFW_KEY_RIGHT, 1, 0);

        // Type 'e'
        textField.onCharInput('e');

        assertThat(textField.getText()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("Key press does nothing when not focused")
    void keyPress_doesNothingWhenNotFocused() {
        textField.setText("Test");
        // Not focused

        boolean handled = textField.onKeyPress(GLFW_KEY_BACKSPACE, 1, 0);

        assertThat(handled).isFalse();
        assertThat(textField.getText()).isEqualTo("Test");
    }

    @Test
    @DisplayName("Mouse click inside focuses the field")
    void mouseClick_inside_focusesField() {
        // Click inside the field bounds (10, 10, 200, 30)
        textField.onMouseClick(50, 20, 0, 1);

        assertThat(textField.isFocused()).isTrue();
    }

    @Test
    @DisplayName("Mouse click outside unfocuses the field")
    void mouseClick_outside_unfocusesField() {
        textField.setFocused(true);

        // Click outside the field bounds
        textField.onMouseClick(300, 300, 0, 1);

        assertThat(textField.isFocused()).isFalse();
    }

    @Test
    @DisplayName("clearFocus removes focus from the field")
    void clearFocus_removesFocus() {
        textField.setFocused(true);
        assertThat(textField.isFocused()).isTrue();

        textField.clearFocus();

        assertThat(textField.isFocused()).isFalse();
    }

    @Test
    @DisplayName("clearFocus does nothing when not focused")
    void clearFocus_doesNothingWhenNotFocused() {
        assertThat(textField.isFocused()).isFalse();

        textField.clearFocus();

        assertThat(textField.isFocused()).isFalse();
    }

    @Test
    @DisplayName("Ctrl+A selects all text")
    void ctrlA_selectsAllText() {
        textField.setText("Hello World");
        textField.setFocused(true);

        // Move cursor to beginning
        textField.onKeyPress(GLFW_KEY_HOME, 1, 0);

        // Ctrl+A should select all text
        textField.onKeyPress(GLFW_KEY_A, 1, GLFW_MOD_CONTROL);

        // Verify selection exists
        assertThat(textField.hasSelection()).isTrue();
        assertThat(textField.getSelectedText()).isEqualTo("Hello World");
    }

    @Test
    @DisplayName("Backspace deletes selection when text is selected")
    void backspace_deletesSelection() {
        textField.setText("Hello World");
        textField.setFocused(true);

        // Select all
        textField.onKeyPress(GLFW_KEY_A, 1, GLFW_MOD_CONTROL);

        // Backspace should delete all selected text
        textField.onKeyPress(GLFW_KEY_BACKSPACE, 1, 0);

        assertThat(textField.getText()).isEmpty();
    }

    @Test
    @DisplayName("Ctrl+X cuts text (clears field)")
    void ctrlX_cutsText() {
        textField.setText("Cut me");
        textField.setFocused(true);

        // Ctrl+X should clear the text
        textField.onKeyPress(GLFW_KEY_X, 1, GLFW_MOD_CONTROL);

        assertThat(textField.getText()).isEmpty();
    }

    // ========== Copy/Paste tests requiring OpenGL context ==========

    @Test
    @EnabledIfSystemProperty(named = "enableGLTests", matches = "true")
    @DisplayName("Ctrl+V pastes text from clipboard")
    void ctrlV_pastesFromClipboard() {
        // This test requires OpenGL context for clipboard access
        try (GUITestRenderer renderer = new GUITestRenderer(400, 300)) {
            renderer.init();

            textField.setText("Before");
            textField.setFocused(true);

            // Set clipboard content
            org.lwjgl.glfw.GLFW.glfwSetClipboardString(0, " Pasted");

            // Ctrl+V to paste
            textField.onKeyPress(GLFW_KEY_V, 1, GLFW_MOD_CONTROL);

            assertThat(textField.getText()).isEqualTo("Before Pasted");
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "enableGLTests", matches = "true")
    @DisplayName("Ctrl+C copies text to clipboard")
    void ctrlC_copiesToClipboard() {
        // This test requires OpenGL context for clipboard access
        try (GUITestRenderer renderer = new GUITestRenderer(400, 300)) {
            renderer.init();

            textField.setText("Copy me");
            textField.setFocused(true);

            // Ctrl+C to copy
            textField.onKeyPress(GLFW_KEY_C, 1, GLFW_MOD_CONTROL);

            // Verify clipboard content
            String clipboard = org.lwjgl.glfw.GLFW.glfwGetClipboardString(0);
            assertThat(clipboard).isEqualTo("Copy me");
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "enableGLTests", matches = "true")
    @DisplayName("Cmd+V pastes text on macOS")
    void cmdV_pastesFromClipboard() {
        // This test requires OpenGL context for clipboard access
        try (GUITestRenderer renderer = new GUITestRenderer(400, 300)) {
            renderer.init();

            textField.setText("");
            textField.setFocused(true);

            // Set clipboard content
            org.lwjgl.glfw.GLFW.glfwSetClipboardString(0, "MacOS paste");

            // Cmd+V (SUPER key on macOS)
            textField.onKeyPress(GLFW_KEY_V, 1, GLFW_MOD_SUPER);

            assertThat(textField.getText()).isEqualTo("MacOS paste");
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "enableGLTests", matches = "true")
    @DisplayName("Paste filters out control characters")
    void paste_filtersControlCharacters() {
        try (GUITestRenderer renderer = new GUITestRenderer(400, 300)) {
            renderer.init();

            textField.setText("");
            textField.setFocused(true);

            // Set clipboard with newlines and tabs
            org.lwjgl.glfw.GLFW.glfwSetClipboardString(0, "Line1\nLine2\tTabbed");

            // Ctrl+V to paste
            textField.onKeyPress(GLFW_KEY_V, 1, GLFW_MOD_CONTROL);

            // Control characters should be filtered
            assertThat(textField.getText()).isEqualTo("Line1Line2Tabbed");
        }
    }
}
