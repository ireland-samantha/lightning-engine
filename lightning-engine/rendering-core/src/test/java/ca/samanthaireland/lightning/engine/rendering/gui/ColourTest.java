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

import ca.samanthaireland.lightning.engine.rendering.render2d.Colour;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for the Colour interface utility methods.
 */
class ColourTest {

    @Test
    void colour_standardColorsAreDefined() {
        assertThat(Colour.WHITE).hasSize(4);
        assertThat(Colour.WHITE).containsExactly(1.0f, 1.0f, 1.0f, 1.0f);

        assertThat(Colour.BLACK).hasSize(4);
        assertThat(Colour.BLACK).containsExactly(0.0f, 0.0f, 0.0f, 1.0f);

        assertThat(Colour.RED).hasSize(4);
        assertThat(Colour.RED).containsExactly(1.0f, 0.0f, 0.0f, 1.0f);

        assertThat(Colour.GREEN).hasSize(4);
        assertThat(Colour.GREEN).containsExactly(0.0f, 1.0f, 0.0f, 1.0f);

        assertThat(Colour.BLUE).hasSize(4);
        assertThat(Colour.BLUE).containsExactly(0.0f, 0.0f, 1.0f, 1.0f);

        assertThat(Colour.TRANSPARENT).hasSize(4);
        assertThat(Colour.TRANSPARENT).containsExactly(0.0f, 0.0f, 0.0f, 0.0f);
    }

    @Test
    void colour_withAlpha_createsNewColorWithAlpha() {
        float[] color = Colour.withAlpha(Colour.RED, 0.5f);

        assertThat(color).hasSize(4);
        assertThat(color[0]).isEqualTo(1.0f);
        assertThat(color[1]).isEqualTo(0.0f);
        assertThat(color[2]).isEqualTo(0.0f);
        assertThat(color[3]).isEqualTo(0.5f);
    }

    @Test
    void colour_withAlpha_doesNotModifyOriginal() {
        float[] original = Colour.BLUE.clone();
        Colour.withAlpha(Colour.BLUE, 0.25f);

        assertThat(Colour.BLUE).containsExactly(original);
    }

    @Test
    void colour_blend_blendsTwoColors() {
        // Blend white (1,1,1,1) and black (0,0,0,1) at 50%
        float[] blended = Colour.blend(Colour.WHITE, Colour.BLACK, 0.5f);

        assertThat(blended).hasSize(4);
        assertThat(blended[0]).isCloseTo(0.5f, within(0.001f));
        assertThat(blended[1]).isCloseTo(0.5f, within(0.001f));
        assertThat(blended[2]).isCloseTo(0.5f, within(0.001f));
        assertThat(blended[3]).isCloseTo(1.0f, within(0.001f));
    }

    @Test
    void colour_blend_zeroFactorReturnsFirstColor() {
        float[] blended = Colour.blend(Colour.RED, Colour.BLUE, 0.0f);

        assertThat(blended).containsExactly(Colour.RED);
    }

    @Test
    void colour_blend_oneFactorReturnsSecondColor() {
        float[] blended = Colour.blend(Colour.RED, Colour.BLUE, 1.0f);

        assertThat(blended).containsExactly(Colour.BLUE);
    }

    @Test
    void colour_rgb_createsColorFrom255Values() {
        float[] color = Colour.rgb(255, 128, 0);

        assertThat(color).hasSize(4);
        assertThat(color[0]).isCloseTo(1.0f, within(0.001f));
        assertThat(color[1]).isCloseTo(0.502f, within(0.01f));
        assertThat(color[2]).isEqualTo(0.0f);
        assertThat(color[3]).isEqualTo(1.0f);
    }

    @Test
    void colour_rgba_createsColorWithAlphaFrom255Values() {
        float[] color = Colour.rgba(255, 0, 0, 128);

        assertThat(color).hasSize(4);
        assertThat(color[0]).isEqualTo(1.0f);
        assertThat(color[1]).isEqualTo(0.0f);
        assertThat(color[2]).isEqualTo(0.0f);
        assertThat(color[3]).isCloseTo(0.502f, within(0.01f));
    }

    @Test
    void colour_themeColorsAreDefined() {
        assertThat(Colour.BACKGROUND).hasSize(4);
        assertThat(Colour.PANEL_BG).hasSize(4);
        assertThat(Colour.BUTTON_BG).hasSize(4);
        assertThat(Colour.BUTTON_HOVER).hasSize(4);
        assertThat(Colour.BUTTON_PRESSED).hasSize(4);
        assertThat(Colour.BORDER).hasSize(4);
        assertThat(Colour.TEXT_PRIMARY).hasSize(4);
        assertThat(Colour.TEXT_SECONDARY).hasSize(4);
        assertThat(Colour.ACCENT).hasSize(4);
        assertThat(Colour.SUCCESS).hasSize(4);
        assertThat(Colour.WARNING).hasSize(4);
        assertThat(Colour.ERROR).hasSize(4);
    }
}
