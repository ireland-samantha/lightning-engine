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

import org.joml.Matrix4f;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

import static org.lwjgl.opengl.GL30.*;

/**
 * OpenGL shader program wrapper.
 *
 * <p>Implements {@link AutoCloseable} for proper resource cleanup.
 */
public class GLShader implements AutoCloseable {
    private final int id;

    public GLShader(String vertPath, String fragPath) {
        int vert = compile(load(vertPath), GL_VERTEX_SHADER);
        int frag = compile(load(fragPath), GL_FRAGMENT_SHADER);

        id = glCreateProgram();
        glAttachShader(id, vert);
        glAttachShader(id, frag);
        glLinkProgram(id);

        glDeleteShader(vert);
        glDeleteShader(frag);
    }

    private int compile(String source, int type) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException(
                    "Shader compile error:\n" + glGetShaderInfoLog(shader)
            );
        }
        return shader;
    }

    private static String load(String path) {
        try (InputStream in = GLShader.class
                .getClassLoader()
                .getResourceAsStream(path)) {

            if (in == null)
                throw new RuntimeException("Shader not found: " + path);

            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void use() {
        glUseProgram(id);
    }

    public void setMat4(String name, Matrix4f mat) {
        glUniformMatrix4fv(
                glGetUniformLocation(id, name),
                false,
                mat.get(new float[16])
        );
    }

    public void setInt(String name, int value) {
        glUniform1i(glGetUniformLocation(id, name), value);
    }

    public void dispose() {
        glDeleteProgram(id);
    }

    @Override
    public void close() {
        dispose();
    }
}
