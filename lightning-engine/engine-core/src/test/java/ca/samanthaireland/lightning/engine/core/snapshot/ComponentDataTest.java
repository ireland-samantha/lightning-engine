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

package ca.samanthaireland.lightning.engine.core.snapshot;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ComponentDataTest {

    @Test
    void of_withNameAndList_createsComponent() {
        ComponentData component = ComponentData.of("POSITION_X", List.of(1.0f, 2.0f, 3.0f));

        assertThat(component.name()).isEqualTo("POSITION_X");
        assertThat(component.values()).containsExactly(1.0f, 2.0f, 3.0f);
    }

    @Test
    void of_withVarargs_createsComponent() {
        ComponentData component = ComponentData.of("VELOCITY_Y", 10.0f, 20.0f);

        assertThat(component.name()).isEqualTo("VELOCITY_Y");
        assertThat(component.values()).containsExactly(10.0f, 20.0f);
    }

    @Test
    void constructor_withNullName_throwsException() {
        assertThatThrownBy(() -> new ComponentData(null, List.of(1.0f)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Component name cannot be null");
    }

    @Test
    void constructor_withNullValues_throwsException() {
        assertThatThrownBy(() -> new ComponentData("TEST", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Component values cannot be null");
    }

    @Test
    void entityCount_returnsSize() {
        ComponentData component = ComponentData.of("TEST", 1.0f, 2.0f, 3.0f, 4.0f);

        assertThat(component.entityCount()).isEqualTo(4);
    }

    @Test
    void valueAt_returnsCorrectValue() {
        ComponentData component = ComponentData.of("TEST", 100.0f, 200.0f, 300.0f);

        assertThat(component.valueAt(0)).isEqualTo(100.0f);
        assertThat(component.valueAt(1)).isEqualTo(200.0f);
        assertThat(component.valueAt(2)).isEqualTo(300.0f);
    }

    @Test
    void valueAt_withInvalidIndex_throwsException() {
        ComponentData component = ComponentData.of("TEST", 1.0f);

        assertThatThrownBy(() -> component.valueAt(5))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void isEmpty_withNoValues_returnsTrue() {
        ComponentData component = ComponentData.of("EMPTY", List.of());

        assertThat(component.isEmpty()).isTrue();
    }

    @Test
    void isEmpty_withValues_returnsFalse() {
        ComponentData component = ComponentData.of("TEST", 1.0f);

        assertThat(component.isEmpty()).isFalse();
    }

    @Test
    void values_areImmutable() {
        List<Float> mutableList = new java.util.ArrayList<>(List.of(1.0f, 2.0f));
        ComponentData component = ComponentData.of("TEST", mutableList);

        // Modify the original list
        mutableList.add(3.0f);

        // Component should be unaffected
        assertThat(component.values()).containsExactly(1.0f, 2.0f);
    }

    @Test
    void values_cannotBeModified() {
        ComponentData component = ComponentData.of("TEST", 1.0f, 2.0f);

        assertThatThrownBy(() -> component.values().add(3.0f))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
