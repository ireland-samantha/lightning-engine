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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InputConstants}.
 */
class InputConstantsTest {

    // ========== Mouse Button Constants ==========

    @Test
    @DisplayName("BUTTON_LEFT should be 0 (GLFW_MOUSE_BUTTON_LEFT)")
    void buttonLeftShouldBeZero() {
        assertThat(InputConstants.BUTTON_LEFT).isEqualTo(0);
    }

    @Test
    @DisplayName("BUTTON_RIGHT should be 1 (GLFW_MOUSE_BUTTON_RIGHT)")
    void buttonRightShouldBeOne() {
        assertThat(InputConstants.BUTTON_RIGHT).isEqualTo(1);
    }

    @Test
    @DisplayName("BUTTON_MIDDLE should be 2 (GLFW_MOUSE_BUTTON_MIDDLE)")
    void buttonMiddleShouldBeTwo() {
        assertThat(InputConstants.BUTTON_MIDDLE).isEqualTo(2);
    }

    @Test
    @DisplayName("Mouse button constants should be unique")
    void mouseButtonConstantsShouldBeUnique() {
        assertThat(InputConstants.BUTTON_LEFT)
                .isNotEqualTo(InputConstants.BUTTON_RIGHT)
                .isNotEqualTo(InputConstants.BUTTON_MIDDLE);
        assertThat(InputConstants.BUTTON_RIGHT)
                .isNotEqualTo(InputConstants.BUTTON_MIDDLE);
    }

    // ========== Action Constants ==========

    @Test
    @DisplayName("ACTION_RELEASE should be 0 (GLFW_RELEASE)")
    void actionReleaseShouldBeZero() {
        assertThat(InputConstants.ACTION_RELEASE).isEqualTo(0);
    }

    @Test
    @DisplayName("ACTION_PRESS should be 1 (GLFW_PRESS)")
    void actionPressShouldBeOne() {
        assertThat(InputConstants.ACTION_PRESS).isEqualTo(1);
    }

    @Test
    @DisplayName("ACTION_REPEAT should be 2 (GLFW_REPEAT)")
    void actionRepeatShouldBeTwo() {
        assertThat(InputConstants.ACTION_REPEAT).isEqualTo(2);
    }

    @Test
    @DisplayName("Action constants should be unique")
    void actionConstantsShouldBeUnique() {
        assertThat(InputConstants.ACTION_RELEASE)
                .isNotEqualTo(InputConstants.ACTION_PRESS)
                .isNotEqualTo(InputConstants.ACTION_REPEAT);
        assertThat(InputConstants.ACTION_PRESS)
                .isNotEqualTo(InputConstants.ACTION_REPEAT);
    }

    // ========== isPress() Helper Method ==========

    @Test
    @DisplayName("isPress() should return true for ACTION_PRESS")
    void isPressReturnsTrueForPressAction() {
        assertThat(InputConstants.isPress(InputConstants.ACTION_PRESS)).isTrue();
    }

    @Test
    @DisplayName("isPress() should return false for ACTION_RELEASE")
    void isPressReturnsFalseForReleaseAction() {
        assertThat(InputConstants.isPress(InputConstants.ACTION_RELEASE)).isFalse();
    }

    @Test
    @DisplayName("isPress() should return false for ACTION_REPEAT")
    void isPressReturnsFalseForRepeatAction() {
        assertThat(InputConstants.isPress(InputConstants.ACTION_REPEAT)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 3, 100, Integer.MAX_VALUE, Integer.MIN_VALUE})
    @DisplayName("isPress() should return false for invalid action values")
    void isPressReturnsFalseForInvalidValues(int invalidAction) {
        assertThat(InputConstants.isPress(invalidAction)).isFalse();
    }

    // ========== isRelease() Helper Method ==========

    @Test
    @DisplayName("isRelease() should return true for ACTION_RELEASE")
    void isReleaseReturnsTrueForReleaseAction() {
        assertThat(InputConstants.isRelease(InputConstants.ACTION_RELEASE)).isTrue();
    }

    @Test
    @DisplayName("isRelease() should return false for ACTION_PRESS")
    void isReleaseReturnsFalseForPressAction() {
        assertThat(InputConstants.isRelease(InputConstants.ACTION_PRESS)).isFalse();
    }

    @Test
    @DisplayName("isRelease() should return false for ACTION_REPEAT")
    void isReleaseReturnsFalseForRepeatAction() {
        assertThat(InputConstants.isRelease(InputConstants.ACTION_REPEAT)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 3, 100, Integer.MAX_VALUE, Integer.MIN_VALUE})
    @DisplayName("isRelease() should return false for invalid action values")
    void isReleaseReturnsFalseForInvalidValues(int invalidAction) {
        assertThat(InputConstants.isRelease(invalidAction)).isFalse();
    }

    // ========== isLeftButton() Helper Method ==========

    @Test
    @DisplayName("isLeftButton() should return true for BUTTON_LEFT")
    void isLeftButtonReturnsTrueForLeftButton() {
        assertThat(InputConstants.isLeftButton(InputConstants.BUTTON_LEFT)).isTrue();
    }

    @Test
    @DisplayName("isLeftButton() should return false for BUTTON_RIGHT")
    void isLeftButtonReturnsFalseForRightButton() {
        assertThat(InputConstants.isLeftButton(InputConstants.BUTTON_RIGHT)).isFalse();
    }

    @Test
    @DisplayName("isLeftButton() should return false for BUTTON_MIDDLE")
    void isLeftButtonReturnsFalseForMiddleButton() {
        assertThat(InputConstants.isLeftButton(InputConstants.BUTTON_MIDDLE)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 3, 100, Integer.MAX_VALUE, Integer.MIN_VALUE})
    @DisplayName("isLeftButton() should return false for invalid button values")
    void isLeftButtonReturnsFalseForInvalidValues(int invalidButton) {
        assertThat(InputConstants.isLeftButton(invalidButton)).isFalse();
    }

    // ========== isRightButton() Helper Method ==========

    @Test
    @DisplayName("isRightButton() should return true for BUTTON_RIGHT")
    void isRightButtonReturnsTrueForRightButton() {
        assertThat(InputConstants.isRightButton(InputConstants.BUTTON_RIGHT)).isTrue();
    }

    @Test
    @DisplayName("isRightButton() should return false for BUTTON_LEFT")
    void isRightButtonReturnsFalseForLeftButton() {
        assertThat(InputConstants.isRightButton(InputConstants.BUTTON_LEFT)).isFalse();
    }

    @Test
    @DisplayName("isRightButton() should return false for BUTTON_MIDDLE")
    void isRightButtonReturnsFalseForMiddleButton() {
        assertThat(InputConstants.isRightButton(InputConstants.BUTTON_MIDDLE)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 3, 100, Integer.MAX_VALUE, Integer.MIN_VALUE})
    @DisplayName("isRightButton() should return false for invalid button values")
    void isRightButtonReturnsFalseForInvalidValues(int invalidButton) {
        assertThat(InputConstants.isRightButton(invalidButton)).isFalse();
    }

    // ========== Typical Usage Patterns ==========

    @Test
    @DisplayName("Typical left-click detection pattern should work")
    void typicalLeftClickPatternWorks() {
        int button = InputConstants.BUTTON_LEFT;
        int action = InputConstants.ACTION_PRESS;

        boolean isLeftClick = InputConstants.isLeftButton(button) && InputConstants.isPress(action);

        assertThat(isLeftClick).isTrue();
    }

    @Test
    @DisplayName("Typical right-click context menu pattern should work")
    void typicalRightClickPatternWorks() {
        int button = InputConstants.BUTTON_RIGHT;
        int action = InputConstants.ACTION_PRESS;

        boolean isRightClick = InputConstants.isRightButton(button) && InputConstants.isPress(action);

        assertThat(isRightClick).isTrue();
    }

    @Test
    @DisplayName("Button release detection pattern should work")
    void buttonReleasePatternWorks() {
        int button = InputConstants.BUTTON_LEFT;
        int action = InputConstants.ACTION_RELEASE;

        boolean isLeftRelease = InputConstants.isLeftButton(button) && InputConstants.isRelease(action);

        assertThat(isLeftRelease).isTrue();
    }
}
