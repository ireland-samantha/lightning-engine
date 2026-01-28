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

import ca.samanthaireland.lightning.engine.rendering.render2d.*;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Optional;

/**
 * OpenGL implementation of {@link ComponentFactory}.
 *
 * <p>This factory creates OpenGL-based GUI components using NanoVG for rendering.
 */
@Slf4j
public class GLComponentFactory implements ComponentFactory {

    private static final GLComponentFactory INSTANCE = new GLComponentFactory();
    private final GLColours colours = new GLColours();

    /**
     * Get the singleton instance.
     */
    public static GLComponentFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public Button createButton(int x, int y, int width, int height, String text) {
        return new GLButton(x, y, width, height, text);
    }

    @Override
    public Label createLabel(int x, int y, String text) {
        return new GLLabel(x, y, text);
    }

    @Override
    public Label createLabel(int x, int y, String text, float fontSize) {
        return new GLLabel(x, y, text, fontSize);
    }

    @Override
    public Panel createPanel(int x, int y, int width, int height) {
        return new GLPanel(x, y, width, height);
    }

    @Override
    public TextField createTextField(int x, int y, int width, int height) {
        return new GLTextField(x, y, width, height);
    }

    @Override
    public ListView createListView(int x, int y, int width, int height) {
        return new GLListView(x, y, width, height);
    }

    @Override
    public TreeView createTreeView(int x, int y, int width, int height) {
        return new GLTreeView(x, y, width, height);
    }

    @Override
    public TreeNode createTreeNode(String text) {
        return new GLTreeNode(text);
    }

    @Override
    public TreeNode createTreeNode(String text, Object userData) {
        return new GLTreeNode(text, userData);
    }

    @Override
    public Image createImage(int x, int y, int width, int height) {
        return new GLImage(x, y, width, height);
    }

    @Override
    public Colours getColours() {
        return colours;
    }

    @Override
    public Optional<Path> openFileDialog(String title, String initialDir, String filters, String filterDescription) {
        // Convert comma-separated filter string to array
        String[] filterArray = filters != null ? filters.split(",") : new String[0];
        return GLFileDialog.openFile(title, initialDir, filterArray, filterDescription);
    }

    /**
     * OpenGL colour implementation providing standard theme colours.
     */
    private static class GLColours implements Colours {

        @Override
        public float[] white() {
            return GLColour.WHITE;
        }

        @Override
        public float[] black() {
            return GLColour.BLACK;
        }

        @Override
        public float[] red() {
            return GLColour.RED;
        }

        @Override
        public float[] green() {
            return GLColour.GREEN;
        }

        @Override
        public float[] blue() {
            return GLColour.BLUE;
        }

        @Override
        public float[] yellow() {
            return GLColour.YELLOW;
        }

        @Override
        public float[] transparent() {
            return GLColour.TRANSPARENT;
        }

        @Override
        public float[] background() {
            return GLColour.BACKGROUND;
        }

        @Override
        public float[] panelBg() {
            return GLColour.PANEL_BG;
        }

        @Override
        public float[] buttonBg() {
            return GLColour.BUTTON_BG;
        }

        @Override
        public float[] buttonHover() {
            return GLColour.BUTTON_HOVER;
        }

        @Override
        public float[] buttonPressed() {
            return GLColour.BUTTON_PRESSED;
        }

        @Override
        public float[] border() {
            return GLColour.BORDER;
        }

        @Override
        public float[] textPrimary() {
            return GLColour.TEXT_PRIMARY;
        }

        @Override
        public float[] textSecondary() {
            return GLColour.TEXT_SECONDARY;
        }

        @Override
        public float[] accent() {
            return GLColour.ACCENT;
        }

        @Override
        public float[] selected() {
            return GLColour.SELECTED;
        }

        @Override
        public float[] success() {
            return GLColour.SUCCESS;
        }

        @Override
        public float[] warning() {
            return GLColour.WARNING;
        }

        @Override
        public float[] error() {
            return GLColour.ERROR;
        }

        @Override
        public float[] withAlpha(float[] colour, float alpha) {
            return GLColour.withAlpha(colour, alpha);
        }
    }
}
