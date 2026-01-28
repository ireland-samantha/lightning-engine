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

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import ca.samanthaireland.lightning.engine.rendering.render2d.Texture;

import static org.lwjgl.opengl.GL30.*;

/**
 * OpenGL texture wrapper.
 * OpenGL implementation of {@link Texture}.
 */
public class GLTexture implements Texture {
    private int id;
    private int width;
    private int height;

    /**
     * Load texture from classpath resource.
     *
     * @param texturePath classpath resource path
     */
    public GLTexture(String texturePath) {
        bindFromClasspath(texturePath);
    }

    /**
     * Load texture from file path.
     *
     * @param filePath file path to the texture
     * @return the loaded texture
     */
    public static GLTexture fromFile(Path filePath) {
        return new GLTexture(filePath);
    }

    /**
     * Private constructor for file-based loading.
     */
    private GLTexture(Path filePath) {
        bindFromFile(filePath);
    }

    private void bindFromClasspath(String path) {
        id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        IntBuffer c = BufferUtils.createIntBuffer(1);

        STBImage.stbi_set_flip_vertically_on_load(true);

        // Load image bytes from classpath
        ByteBuffer imageBuffer = loadImageFromClasspath(path);

        // Decode PNG -> raw RGBA pixels
        ByteBuffer data = STBImage.stbi_load_from_memory(imageBuffer, w, h, c, 4);
        if (data == null)
            throw new RuntimeException("Failed to decode texture: " + STBImage.stbi_failure_reason());

        width = w.get(0);
        height = h.get(0);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height,
                0, GL_RGBA, GL_UNSIGNED_BYTE, data);

        STBImage.stbi_image_free(data);
    }

    private void bindFromFile(Path filePath) {
        id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        IntBuffer c = BufferUtils.createIntBuffer(1);

        STBImage.stbi_set_flip_vertically_on_load(true);

        // Load image bytes from file
        ByteBuffer imageBuffer;
        try {
            byte[] bytes = Files.readAllBytes(filePath);
            imageBuffer = BufferUtils.createByteBuffer(bytes.length);
            imageBuffer.put(bytes);
            imageBuffer.flip();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load texture from file: " + filePath, e);
        }

        // Decode image -> raw RGBA pixels
        ByteBuffer data = STBImage.stbi_load_from_memory(imageBuffer, w, h, c, 4);
        if (data == null) {
            throw new RuntimeException("Failed to decode texture: " + STBImage.stbi_failure_reason());
        }

        width = w.get(0);
        height = h.get(0);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height,
                0, GL_RGBA, GL_UNSIGNED_BYTE, data);

        STBImage.stbi_image_free(data);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    private ByteBuffer loadImageFromClasspath(String path) {
        ByteBuffer imageBuffer;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null)
                throw new RuntimeException("Texture not found: " + path);
            imageBuffer = ioResourceToByteBuffer(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load texture: " + path, e);
        }
        return imageBuffer;
    }

    private ByteBuffer ioResourceToByteBuffer(InputStream source) throws IOException {
        ReadableByteChannel rbc = Channels.newChannel(source);
        ByteBuffer buffer = BufferUtils.createByteBuffer(1024);
        while (true) {
            int bytes = rbc.read(buffer);
            if (bytes == -1)
                break;
            if (buffer.remaining() == 0)
                buffer = resizeBuffer(buffer, buffer.capacity() * 2);
        }
        buffer.flip();
        return buffer;
    }


    private ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, id);
    }

    public void dispose() {
        glDeleteTextures(id);
    }

    @Override
    public void close() {
        dispose();
    }
}
