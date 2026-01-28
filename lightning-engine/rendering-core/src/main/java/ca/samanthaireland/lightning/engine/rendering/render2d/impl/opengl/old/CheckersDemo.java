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


package ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.old;

import ca.samanthaireland.lightning.engine.rendering.render2d.*;
import ca.samanthaireland.lightning.engine.rendering.render2d.Window;
import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLComponentFactory;

/**
 * Demo application showcasing sprite rendering with keyboard controls.
 *
 * <p>Creates two checker sprites and allows moving the red checker
 * using arrow keys.
 *
 * <p>Usage:
 * <pre>
 *   CheckersDemo.main(new String[]{});
 * </pre>
 *
 * <p>Controls:
 * <ul>
 *   <li>Arrow keys - Move red checker</li>
 *   <li>ESC - Exit</li>
 * </ul>
 */
public class CheckersDemo {

    public static void main(String[] args) {
        // Create sprites for the checkers
        Sprite redChecker = Sprite.builder()
                .id(1)
                .texturePath("textures/red-checker.png")
                .sizeX(64)
                .sizeY(64)
                .x(100)
                .y(100)
                .build();

        Sprite blackChecker = Sprite.builder()
                .id(2)
                .texturePath("textures/black-checker.png")
                .sizeX(64)
                .sizeY(64)
                .x(200)
                .y(200)
                .build();

        // Create window using the builder
        Window window = WindowBuilder.create()
                .size(1000, 1000)
                .title("Lightning Engine - Checkers Demo")
                .debugMode(false)
                .build();

        // Add sprites to the window
        window.addSprite(redChecker);
        window.addSprite(blackChecker);

        // Add keyboard controls for moving the red checker
        window.addControls(keyType -> {
            int speed = 5;
            switch (keyType) {
                case UP -> redChecker.setY(redChecker.getY() - speed);
                case DOWN -> redChecker.setY(redChecker.getY() + speed);
                case LEFT -> redChecker.setX(redChecker.getX() - speed);
                case RIGHT -> redChecker.setX(redChecker.getX() + speed);
            }
        });

        // Run the window (blocking call)
        GLComponentFactory glFactory = GLComponentFactory.getInstance();
        Label label = glFactory.createLabel(100, 100, "Checkers");
        window.addComponent(label);

        window.run();
    }
}
