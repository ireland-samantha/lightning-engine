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


package ca.samanthaireland.game.renderering;

import ca.samanthaireland.lightning.engine.rendering.testing.headless.HeadlessWindow;
import ca.samanthaireland.game.domain.ControlSystem;
import ca.samanthaireland.game.domain.Sprite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DefaultGameRenderer using HeadlessWindow.
 */
class DefaultGameRendererTest {

    private HeadlessWindow window;
    private DefaultGameRenderer renderer;

    @BeforeEach
    void setUp() {
        window = new HeadlessWindow(800, 600, "Test Window");
        renderer = new DefaultGameRenderer(window);
    }

    @Nested
    @DisplayName("Sprite Rendering")
    class SpriteRendering {

        @Test
        @DisplayName("renderSprites adds sprites to window")
        void renderSprites_addsSpritesToWindow() {
            List<Sprite> sprites = List.of(
                    createSprite(1, 100, 200),
                    createSprite(2, 300, 400)
            );

            renderer.renderSprites(sprites);

            assertThat(window.getSprites()).hasSize(2);
        }

        @Test
        @DisplayName("renderSprites updates existing sprites")
        void renderSprites_updatesExistingSprites() {
            Sprite sprite = createSprite(1, 100, 200);
            renderer.renderSprites(List.of(sprite));

            // Update position
            sprite.setPosition(500, 600);
            renderer.renderSprites(List.of(sprite));

            var renderingSprites = window.getSprites();
            assertThat(renderingSprites).hasSize(1);
            assertThat(renderingSprites.get(0).getX()).isEqualTo(500);
            assertThat(renderingSprites.get(0).getY()).isEqualTo(600);
        }

        @Test
        @DisplayName("renderSprites removes sprites no longer present")
        void renderSprites_removesAbsentSprites() {
            Sprite sprite1 = createSprite(1, 100, 200);
            Sprite sprite2 = createSprite(2, 300, 400);
            renderer.renderSprites(List.of(sprite1, sprite2));

            assertThat(window.getSprites()).hasSize(2);

            // Remove sprite2
            renderer.renderSprites(List.of(sprite1));

            assertThat(window.getSprites()).hasSize(1);
            assertThat(window.getSprites().get(0).getId()).isEqualTo(1);
        }

        @Test
        @DisplayName("invisible sprites are not rendered")
        void invisibleSprites_notRendered() {
            Sprite visible = createSprite(1, 100, 200);
            Sprite invisible = createSprite(2, 300, 400);
            invisible.setVisible(false);

            renderer.renderSprites(List.of(visible, invisible));

            assertThat(window.getSprites()).hasSize(1);
            assertThat(window.getSprites().get(0).getId()).isEqualTo(1);
        }

        @Test
        @DisplayName("sprite properties are mapped correctly")
        void spriteProperties_mappedCorrectly() {
            Sprite sprite = createSprite(1, 100, 200);
            sprite.setWidth(64);
            sprite.setHeight(32);
            sprite.setZIndex(5);
            sprite.setTexturePath("/textures/player.png");

            renderer.renderSprites(List.of(sprite));

            var renderingSprite = window.getSprites().get(0);
            assertThat(renderingSprite.getX()).isEqualTo(100);
            assertThat(renderingSprite.getY()).isEqualTo(200);
            assertThat(renderingSprite.getSizeX()).isEqualTo(64);
            assertThat(renderingSprite.getSizeY()).isEqualTo(32);
            assertThat(renderingSprite.getZIndex()).isEqualTo(5);
            assertThat(renderingSprite.getTexturePath()).isEqualTo("/textures/player.png");
        }
    }

    @Nested
    @DisplayName("Control System")
    class ControlSystemTests {

        @Test
        @DisplayName("arrow key press events are routed to control system")
        void arrowKeyPress_routedToControlSystem() {
            AtomicInteger pressedKey = new AtomicInteger(-1);
            renderer.setControlSystem(new ControlSystem() {
                @Override
                public void onKeyPressed(int key) {
                    pressedKey.set(key);
                }
            });

            // Simulate arrow key press (KeyInputHandler only supports arrow keys)
            window.simulateKey(ControlSystem.KeyCodes.UP, 1);

            assertThat(pressedKey.get()).isEqualTo(ControlSystem.KeyCodes.UP);
        }

        @Test
        @DisplayName("different arrow keys are distinguished")
        void differentArrowKeys_distinguished() {
            List<Integer> pressedKeys = new ArrayList<>();
            renderer.setControlSystem(new ControlSystem() {
                @Override
                public void onKeyPressed(int key) {
                    pressedKeys.add(key);
                }
            });

            window.simulateKey(ControlSystem.KeyCodes.UP, 1);
            window.simulateKey(ControlSystem.KeyCodes.DOWN, 1);
            window.simulateKey(ControlSystem.KeyCodes.LEFT, 1);
            window.simulateKey(ControlSystem.KeyCodes.RIGHT, 1);

            assertThat(pressedKeys).containsExactly(
                    ControlSystem.KeyCodes.UP,
                    ControlSystem.KeyCodes.DOWN,
                    ControlSystem.KeyCodes.LEFT,
                    ControlSystem.KeyCodes.RIGHT
            );
        }

        @Test
        @DisplayName("onUpdate receives key states for arrow keys")
        void onUpdate_receivesKeyStates() {
            AtomicBoolean upWasPressed = new AtomicBoolean(false);
            renderer.setControlSystem(new ControlSystem() {
                @Override
                public void onUpdate(KeyStates keyStates) {
                    if (keyStates.isPressed(ControlSystem.KeyCodes.UP)) {
                        upWasPressed.set(true);
                    }
                }
            });

            // Simulate UP arrow key press
            window.simulateKey(ControlSystem.KeyCodes.UP, 1);

            // Run a frame to trigger onUpdate
            renderer.runFrames(1, () -> {});

            assertThat(upWasPressed.get()).isTrue();
        }
    }

    @Nested
    @DisplayName("Lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("runFrames calls update callback")
        void runFrames_callsUpdateCallback() {
            AtomicInteger updateCount = new AtomicInteger(0);

            renderer.runFrames(5, updateCount::incrementAndGet);

            assertThat(updateCount.get()).isEqualTo(5);
        }

        @Test
        @DisplayName("dispose clears all sprites")
        void dispose_clearsSprites() {
            renderer.renderSprites(List.of(
                    createSprite(1, 0, 0),
                    createSprite(2, 0, 0)
            ));
            assertThat(window.getSprites()).hasSize(2);

            renderer.dispose();

            assertThat(window.getSprites()).isEmpty();
        }

        @Test
        @DisplayName("window dimensions are accessible")
        void windowDimensions_accessible() {
            assertThat(renderer.getWidth()).isEqualTo(800);
            assertThat(renderer.getHeight()).isEqualTo(600);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("errors in update callback are handled")
        void errorsInUpdate_handled() {
            AtomicReference<Exception> caughtError = new AtomicReference<>();
            renderer.setOnError(caughtError::set);

            renderer.runFrames(1, () -> {
                throw new RuntimeException("Test error");
            });

            assertThat(caughtError.get()).isNotNull();
            assertThat(caughtError.get().getMessage()).isEqualTo("Test error");
        }
    }

    private Sprite createSprite(long entityId, float x, float y) {
        Sprite sprite = new Sprite(entityId);
        sprite.setPosition(x, y);
        return sprite;
    }
}
