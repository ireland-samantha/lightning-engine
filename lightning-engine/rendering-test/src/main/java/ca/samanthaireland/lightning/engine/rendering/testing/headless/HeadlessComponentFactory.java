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


package ca.samanthaireland.lightning.engine.rendering.testing.headless;

import ca.samanthaireland.lightning.engine.rendering.render2d.Button;
import ca.samanthaireland.lightning.engine.rendering.render2d.ComponentFactory;
import ca.samanthaireland.lightning.engine.rendering.render2d.Image;
import ca.samanthaireland.lightning.engine.rendering.render2d.Label;
import ca.samanthaireland.lightning.engine.rendering.render2d.ListView;
import ca.samanthaireland.lightning.engine.rendering.render2d.Panel;
import ca.samanthaireland.lightning.engine.rendering.render2d.TextField;
import ca.samanthaireland.lightning.engine.rendering.render2d.TreeNode;
import ca.samanthaireland.lightning.engine.rendering.render2d.TreeView;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Component factory for headless testing.
 * Creates mock components that don't require OpenGL or any rendering context.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ComponentFactory factory = new HeadlessComponentFactory();
 * Button button = factory.createButton(10, 10, 100, 30, "Click Me");
 * }</pre>
 */
public class HeadlessComponentFactory implements ComponentFactory {

    private static final HeadlessComponentFactory INSTANCE = new HeadlessComponentFactory();

    private final Colours colours = new HeadlessColours();

    /**
     * Get the singleton instance.
     * @return the factory instance
     */
    public static HeadlessComponentFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public Button createButton(int x, int y, int width, int height, String text) {
        return new MockButton(x, y, width, height, text);
    }

    @Override
    public Label createLabel(int x, int y, String text) {
        return new MockLabel(x, y, text);
    }

    @Override
    public Label createLabel(int x, int y, String text, float fontSize) {
        return new MockLabel(x, y, text, fontSize);
    }

    @Override
    public Panel createPanel(int x, int y, int width, int height) {
        return new MockPanel(x, y, width, height);
    }

    @Override
    public TextField createTextField(int x, int y, int width, int height) {
        return new MockTextField(x, y, width, height);
    }

    @Override
    public ListView createListView(int x, int y, int width, int height) {
        return new MockListView(x, y, width, height);
    }

    @Override
    public TreeView createTreeView(int x, int y, int width, int height) {
        return new MockTreeView(x, y, width, height);
    }

    @Override
    public TreeNode createTreeNode(String text) {
        return new MockTreeNode(text);
    }

    @Override
    public TreeNode createTreeNode(String text, Object userData) {
        return new MockTreeNode(text, userData);
    }

    @Override
    public Image createImage(int x, int y, int width, int height) {
        return new MockImage(x, y, width, height);
    }

    @Override
    public Colours getColours() {
        return colours;
    }

    @Override
    public Optional<Path> openFileDialog(String title, String initialDir, String filters, String filterDescription) {
        // File dialogs not supported in headless mode
        return Optional.empty();
    }

    /**
     * Headless colour scheme for testing.
     */
    private static class HeadlessColours implements Colours {

        @Override
        public float[] white() { return new float[]{1f, 1f, 1f, 1f}; }

        @Override
        public float[] black() { return new float[]{0f, 0f, 0f, 1f}; }

        @Override
        public float[] red() { return new float[]{1f, 0f, 0f, 1f}; }

        @Override
        public float[] green() { return new float[]{0f, 1f, 0f, 1f}; }

        @Override
        public float[] blue() { return new float[]{0f, 0f, 1f, 1f}; }

        @Override
        public float[] yellow() { return new float[]{1f, 1f, 0f, 1f}; }

        @Override
        public float[] transparent() { return new float[]{0f, 0f, 0f, 0f}; }

        @Override
        public float[] background() { return new float[]{0.1f, 0.1f, 0.1f, 1f}; }

        @Override
        public float[] panelBg() { return new float[]{0.15f, 0.15f, 0.15f, 1f}; }

        @Override
        public float[] buttonBg() { return new float[]{0.2f, 0.2f, 0.2f, 1f}; }

        @Override
        public float[] buttonHover() { return new float[]{0.25f, 0.25f, 0.25f, 1f}; }

        @Override
        public float[] buttonPressed() { return new float[]{0.18f, 0.18f, 0.18f, 1f}; }

        @Override
        public float[] border() { return new float[]{0.3f, 0.3f, 0.3f, 1f}; }

        @Override
        public float[] textPrimary() { return new float[]{0.9f, 0.9f, 0.9f, 1f}; }

        @Override
        public float[] textSecondary() { return new float[]{0.6f, 0.6f, 0.6f, 1f}; }

        @Override
        public float[] accent() { return new float[]{0.2f, 0.6f, 1f, 1f}; }

        @Override
        public float[] selected() { return new float[]{0.2f, 0.4f, 0.6f, 1f}; }

        @Override
        public float[] success() { return new float[]{0.2f, 0.8f, 0.2f, 1f}; }

        @Override
        public float[] warning() { return new float[]{0.9f, 0.7f, 0.1f, 1f}; }

        @Override
        public float[] error() { return new float[]{0.9f, 0.2f, 0.2f, 1f}; }

        @Override
        public float[] withAlpha(float[] colour, float alpha) {
            return new float[]{colour[0], colour[1], colour[2], alpha};
        }
    }
}
