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

import ca.samanthaireland.lightning.engine.rendering.render2d.KeyInputHandler;
import ca.samanthaireland.lightning.engine.rendering.render2d.Sprite;
import ca.samanthaireland.lightning.engine.rendering.render2d.MouseInputHandler;
import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLKeyListener;
import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLSprite;
import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLSpriteRenderer;
import ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLTexture;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.*;
@Deprecated
public class GLWindow implements Window {
    private final List<Sprite> sprites = new ArrayList<>();
    private final Map<Integer, Sprite> spriteMap = new HashMap<>();

    private KeyInputHandler keyListener;

    @Override
    public void addSprite(Sprite sprite) {
        sprites.add(sprite);
    }

    @Override
    public void addControls(KeyInputHandler handler) {
        keyListener = handler;
    }

    @Override
    public void addControls(MouseInputHandler handler) {
        // TODO impement
    }

    @Override
    public void render() {
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        long window = glfwCreateWindow(800, 600, "Lightning Engine", 0, 0);
        if (window == 0) throw new RuntimeException("Failed to create window");

        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);


        glfwSwapInterval(1);

        Matrix4f projection = new Matrix4f().ortho2D(0, 800, 600, 0);
        GLSpriteRenderer renderer = new GLSpriteRenderer(projection);

        List<GLSprite> glSprites = new ArrayList<>();
        List<GLTexture> textures = new ArrayList<>();

        for (Sprite sprite : sprites) {
            GLTexture playerTex = new GLTexture(sprite.getTexturePath());
            GLSprite player = new GLSprite(playerTex);
            player.id = sprite.getId();
            player.position.set(sprite.getX(), sprite.getY());
            player.size.set(sprite.getSizeX(), sprite.getSizeY());
            glSprites.add(player);
            textures.add(playerTex);

            spriteMap.put(sprite.getId(), sprite);
        }

        attachEventHandlers(window);

        while (!glfwWindowShouldClose(window)) {
            glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            for (GLSprite sprite : glSprites) {
                Sprite newSprite = spriteMap.get(sprite.id);
                sprite.position.x = newSprite.getX();
                sprite.position.y = newSprite.getY();
                renderer.draw(sprite);
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }

        // Clean up
        textures.forEach(GLTexture::dispose);
        glfwTerminate();
    }

    private void attachEventHandlers(long window) {
        if (keyListener != null) {
            GLKeyListener glHandler = new GLKeyListener(window, keyListener);
            glHandler.attach();
        }
    }
}
