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


package ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl;

import ca.samanthaireland.lightning.engine.rendering.render2d.Sprite;
import ca.samanthaireland.lightning.engine.rendering.render2d.SpriteRenderer;
import ca.samanthaireland.lightning.engine.rendering.render2d.Texture;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL30.*;

/**
 * OpenGL sprite renderer.
 * OpenGL implementation of {@link SpriteRenderer}.
 */
public class GLSpriteRenderer implements SpriteRenderer {
    private int quadVAO;
    private final GLShader shader;
    private final Matrix4f projection;
    private boolean inBatch = false;

    // Texture cache for Sprite objects
    private final Map<String, GLTexture> textureCache = new HashMap<>();

    public GLSpriteRenderer(Matrix4f projection) {
        this.projection = projection;
        shader = new GLShader("shaders/sprite.vert", "shaders/sprite.frag");
        shader.use();

        shader.setMat4("projection", projection);
        shader.setInt("sprite", 0);

        bindQuad();
    }

    public GLSpriteRenderer(int windowWidth, int windowHeight) {
        this(new Matrix4f().ortho(0, windowWidth, windowHeight, 0, -1, 1));
    }

    private void bindQuad() {
        float[] vertices = {
                // x, y, u, v
                0f, 0f,   0f, 0f, // top-left
                1f, 0f,   1f, 0f, // top-right
                1f, 1f,   1f, 1f, // bottom-right
                0f, 1f,   0f, 1f  // bottom-left
        };

        int[] indices = {
                0, 1, 2,
                2, 3, 0
        };

        quadVAO = glGenVertexArrays();
        int vbo = glGenBuffers();
        int ebo = glGenBuffers();

        glBindVertexArray(quadVAO);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        // position attribute
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // texcoord attribute
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    /**
     * Draw a GLSprite (internal API).
     */
    public void draw(GLSprite sprite) {
        if (!inBatch) {
            shader.use();
        }

        Matrix4f model = new Matrix4f()
                .translate(sprite.position.x, sprite.position.y, 0f)
                .scale(sprite.size.x, sprite.size.y, 1f);

        shader.setMat4("model", model);

        glActiveTexture(GL_TEXTURE0);
        sprite.texture.bind();

        glBindVertexArray(quadVAO);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    @Override
    public void draw(Sprite sprite) {
        String texturePath = sprite.getTexturePath();
        if (texturePath == null || texturePath.isEmpty()) {
            // Skip sprites without textures
            return;
        }
        GLTexture texture = getOrLoadTexture(texturePath);
        draw(texture, sprite.getX(), sprite.getY(), sprite.getSizeX(), sprite.getSizeY());
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height) {
        draw(texture, x, y, width, height, 0f);
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height, float rotation) {
        if (!inBatch) {
            shader.use();
        }

        Matrix4f model = new Matrix4f()
                .translate(x, y, 0f);

        if (rotation != 0f) {
            // Rotate around center
            model.translate(width / 2f, height / 2f, 0f)
                 .rotateZ(rotation)
                 .translate(-width / 2f, -height / 2f, 0f);
        }

        model.scale(width, height, 1f);

        shader.setMat4("model", model);

        glActiveTexture(GL_TEXTURE0);
        texture.bind();

        glBindVertexArray(quadVAO);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    @Override
    public void begin() {
        inBatch = true;
        shader.use();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void end() {
        inBatch = false;
        glDisable(GL_BLEND);
    }

    /**
     * Get or load a texture from the cache.
     * Detects whether the path is a file system path or classpath resource.
     */
    private GLTexture getOrLoadTexture(String path) {
        return textureCache.computeIfAbsent(path, p -> {
            // Check if path is an absolute file path
            if (isFileSystemPath(p)) {
                return GLTexture.fromFile(java.nio.file.Path.of(p));
            } else {
                return new GLTexture(p);
            }
        });
    }

    /**
     * Check if the path is a file system path rather than a classpath resource.
     */
    private boolean isFileSystemPath(String path) {
        // Unix absolute path or Windows path (e.g., C:\)
        return path.startsWith("/") || (path.length() > 2 && path.charAt(1) == ':');
    }

    /**
     * Load a texture and add it to the cache.
     */
    public GLTexture loadTexture(String path) {
        return getOrLoadTexture(path);
    }

    @Override
    public void dispose() {
        shader.dispose();
        glDeleteVertexArrays(quadVAO);
        // Dispose all cached textures
        for (GLTexture texture : textureCache.values()) {
            texture.dispose();
        }
        textureCache.clear();
    }

    @Override
    public void close() {
        dispose();
    }
}
