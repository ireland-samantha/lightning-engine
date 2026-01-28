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


/**
 * GUI Framework - Abstract UI components with OpenGL/NanoVG implementation.
 *
 * <h2>Architecture</h2>
 * This package provides a GUI framework with two layers:
 * <ul>
 *   <li><b>Abstractions</b> - Interfaces and base classes for client code</li>
 *   <li><b>Implementation</b> - OpenGL/NanoVG rendering (internal use)</li>
 * </ul>
 *
 * <h2>For Client Code (gui module)</h2>
 * Use only these abstractions:
 * <ul>
 *   <li>{@link ca.samanthaireland.lightning.engine.rendering.render2d.Window} - Window interface</li>
 *   <li>{@link ca.samanthaireland.lightning.engine.rendering.render2d.WindowBuilder} - Create windows</li>
 *   <li>{@link ca.samanthaireland.lightning.engine.rendering.render2d.WindowComponent} - Component interface</li>
 *   <li>{@link ca.samanthaireland.lightning.engine.rendering.render2d.AbstractWindowComponent} - Base class</li>
 *   <li>{@link ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLPanel} - Container with title</li>
 *   <li>{@link ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLButton} - Clickable button</li>
 *   <li>{@link ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLLabel} - Text label</li>
 *   <li>{@link ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLTextField} - Text input</li>
 *   <li>{@link ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLTreeView} - Hierarchical data</li>
 *   <li>{@link ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLListView} - List data</li>
 *   <li>{@link ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLColour} - Color constants</li>
 * </ul>
 *
 * <h2>Implementation Classes (internal)</h2>
 * These handle OpenGL rendering - don't reference directly:
 * <ul>
 *   <li>{@link ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLWindow} - OpenGL window</li>
 *   <li>{@link ca.samanthaireland.lightning.engine.rendering.render2d.impl.opengl.GLContext} - NanoVG context holder</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Create window using builder
 * Window window = WindowBuilder.create()
 *     .size(800, 600)
 *     .title("My App")
 *     .build();
 *
 * // Add components
 * Button button = new Button(10, 10, 100, 30, "Click Me");
 * button.setOnClick(() -> System.out.println("Clicked!"));
 * window.addComponent(button);
 *
 * // Set update callback
 * window.setOnUpdate(() -> {
 *     // Called every frame
 * });
 *
 * // Run (blocks until window closes)
 * window.run();
 * }</pre>
 *
 * <h2>Creating Custom Components</h2>
 * <pre>{@code
 * public class MyWidget extends AbstractGUIComponent {
 *     public MyWidget(int x, int y, int width, int height) {
 *         super(x, y, width, height);
 *     }
 *
 *     @Override
 *     public void render(long nvg) {
 *         // Use NanoVG API for rendering
 *         nvgBeginPath(nvg);
 *         nvgRect(nvg, getX(), getY(), getWidth(), getHeight());
 *         nvgFillColor(nvg, GUIColor.toNVG(GUIColor.PANEL_BACKGROUND));
 *         nvgFill(nvg);
 *     }
 *
 *     @Override
 *     public boolean onMouseClick(int x, int y, int button, int action) {
 *         if (contains(x, y) && action == 1) {
 *             // Handle click
 *             return true;  // Event consumed
 *         }
 *         return false;
 *     }
 * }
 * }</pre>
 *
 * <h2>Event Handling</h2>
 * Events propagate from window to components in reverse order (top-most first).
 * Return {@code true} from event handlers to consume the event.
 *
 * @see ca.samanthaireland.lightning.engine.rendering.render2d.Window
 * @see ca.samanthaireland.lightning.engine.rendering.render2d.WindowBuilder
 * @see ca.samanthaireland.lightning.engine.rendering.render2d.WindowComponent
 */
package ca.samanthaireland.lightning.engine.rendering.render2d;
