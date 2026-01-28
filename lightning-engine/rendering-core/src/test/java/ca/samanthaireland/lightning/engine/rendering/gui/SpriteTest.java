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

import ca.samanthaireland.lightning.engine.rendering.render2d.Sprite;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the Sprite class.
 */
class SpriteTest {

    @Test
    void sprite_builderCreatesSprite() {
        Sprite sprite = Sprite.builder()
                .id(1)
                .texturePath("textures/test.png")
                .x(100)
                .y(200)
                .sizeX(64)
                .sizeY(64)
                .build();

        assertThat(sprite.getId()).isEqualTo(1);
        assertThat(sprite.getTexturePath()).isEqualTo("textures/test.png");
        assertThat(sprite.getX()).isEqualTo(100);
        assertThat(sprite.getY()).isEqualTo(200);
        assertThat(sprite.getSizeX()).isEqualTo(64);
        assertThat(sprite.getSizeY()).isEqualTo(64);
    }

    @Test
    void sprite_positionCanBeModified() {
        Sprite sprite = Sprite.builder()
                .id(1)
                .texturePath("textures/test.png")
                .x(100)
                .y(100)
                .sizeX(32)
                .sizeY(32)
                .build();

        sprite.setX(200);
        sprite.setY(300);

        assertThat(sprite.getX()).isEqualTo(200);
        assertThat(sprite.getY()).isEqualTo(300);
    }

    @Test
    void sprite_sizeCanBeModified() {
        Sprite sprite = Sprite.builder()
                .id(1)
                .texturePath("textures/test.png")
                .x(0)
                .y(0)
                .sizeX(32)
                .sizeY(32)
                .build();

        sprite.setSizeX(128);
        sprite.setSizeY(128);

        assertThat(sprite.getSizeX()).isEqualTo(128);
        assertThat(sprite.getSizeY()).isEqualTo(128);
    }

    @Test
    void sprite_texturePathCanBeModified() {
        Sprite sprite = Sprite.builder()
                .id(1)
                .texturePath("textures/old.png")
                .x(0)
                .y(0)
                .sizeX(32)
                .sizeY(32)
                .build();

        sprite.setTexturePath("textures/new.png");

        assertThat(sprite.getTexturePath()).isEqualTo("textures/new.png");
    }
}
