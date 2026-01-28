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

import ca.samanthaireland.lightning.engine.rendering.render2d.KeyInputHandler;
import ca.samanthaireland.lightning.engine.rendering.render2d.SpriteInputHandler;
import ca.samanthaireland.lightning.engine.rendering.render2d.Window;
import ca.samanthaireland.lightning.engine.rendering.render2d.WindowBuilder;
import ca.samanthaireland.game.domain.ControlSystem;
import ca.samanthaireland.game.domain.Sprite;
import ca.samanthaireland.game.orchestrator.SpriteSnapshotMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Default implementation of GameRenderer using rendering-core's Window abstraction.
 *
 * <p>This renderer:
 * <ul>
 *   <li>Creates a window using rendering-core's WindowBuilder</li>
 *   <li>Converts game sprites to rendering-core sprites</li>
 *   <li>Handles input events and routes them to ControlSystem</li>
 *   <li>Manages the render loop lifecycle</li>
 * </ul>
 */
public class DefaultGameRenderer implements GameRenderer {

    private final Window window;
    private final int width;
    private final int height;

    private ControlSystem controlSystem;
    private SpriteSnapshotMapper spriteMapper;
    private Consumer<Exception> errorHandler;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Map<Long, ca.samanthaireland.lightning.engine.rendering.render2d.Sprite> renderingSpriteMap = new HashMap<>();
    private final Map<Long, Sprite> gameSpriteMap = new HashMap<>();
    private final Set<Integer> pressedKeys = new HashSet<>();

    private Sprite hoveredSprite = null;
    private float lastMouseX = 0;
    private float lastMouseY = 0;

    /**
     * Create a DefaultGameRenderer with the specified dimensions and title.
     *
     * @param width  window width
     * @param height window height
     * @param title  window title
     */
    public DefaultGameRenderer(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.window = WindowBuilder.create()
                .size(width, height)
                .title(title)
                .build();
        setupInputHandlers();
    }

    /**
     * Create a DefaultGameRenderer with a pre-built window.
     * Useful for testing with HeadlessWindow.
     *
     * @param window the window to use
     */
    public DefaultGameRenderer(Window window) {
        this.window = window;
        this.width = window.getWidth();
        this.height = window.getHeight();
        setupInputHandlers();
    }

    private void setupInputHandlers() {
        // Add keyboard handler
        window.addControls(new KeyInputHandler() {
            @Override
            public void onArrowKeyPress(KeyType keyType) {
                if (controlSystem == null) return;

                int key = switch (keyType) {
                    case UP -> ControlSystem.KeyCodes.UP;
                    case DOWN -> ControlSystem.KeyCodes.DOWN;
                    case LEFT -> ControlSystem.KeyCodes.LEFT;
                    case RIGHT -> ControlSystem.KeyCodes.RIGHT;
                };

                pressedKeys.add(key);
                controlSystem.onKeyPressed(key);
            }
        });
    }

    @Override
    public void setControlSystem(ControlSystem controlSystem) {
        this.controlSystem = controlSystem;
    }

    @Override
    public void setSpriteMapper(SpriteSnapshotMapper mapper) {
        this.spriteMapper = mapper;
    }

    @Override
    public void renderSnapshot(Object snapshot) {
        if (spriteMapper == null) {
            return;
        }

        List<Sprite> sprites = spriteMapper.spritesFromSnapshot(snapshot);
        renderSprites(sprites);
    }

    @Override
    public void renderSprites(List<Sprite> sprites) {
        // Track which sprites are still present
        Set<Long> currentEntityIds = new HashSet<>();

        for (Sprite gameSprite : sprites) {
            if (!gameSprite.isVisible()) continue;

            currentEntityIds.add(gameSprite.getEntityId());
            gameSpriteMap.put(gameSprite.getEntityId(), gameSprite);

            // Get or create rendering sprite
            ca.samanthaireland.lightning.engine.rendering.render2d.Sprite renderingSprite =
                    renderingSpriteMap.get(gameSprite.getEntityId());

            if (renderingSprite == null) {
                // Create new rendering sprite
                renderingSprite = createRenderingSprite(gameSprite);
                renderingSpriteMap.put(gameSprite.getEntityId(), renderingSprite);
                window.addSprite(renderingSprite);
            } else {
                // Update existing sprite
                updateRenderingSprite(renderingSprite, gameSprite);
            }
        }

        // Remove sprites that are no longer present
        List<Long> toRemove = new ArrayList<>();
        for (Long entityId : renderingSpriteMap.keySet()) {
            if (!currentEntityIds.contains(entityId)) {
                toRemove.add(entityId);
            }
        }
        for (Long entityId : toRemove) {
            ca.samanthaireland.lightning.engine.rendering.render2d.Sprite removed = renderingSpriteMap.remove(entityId);
            gameSpriteMap.remove(entityId);
            if (removed != null) {
                window.removeSprite(removed);
            }
        }
    }

    private ca.samanthaireland.lightning.engine.rendering.render2d.Sprite createRenderingSprite(Sprite gameSprite) {
        ca.samanthaireland.lightning.engine.rendering.render2d.Sprite sprite =
                ca.samanthaireland.lightning.engine.rendering.render2d.Sprite.builder()
                        .id((int) gameSprite.getEntityId())
                        .x((int) gameSprite.getX())
                        .y((int) gameSprite.getY())
                        .sizeX((int) gameSprite.getWidth())
                        .sizeY((int) gameSprite.getHeight())
                        .texturePath(gameSprite.getTexturePath())
                        .zIndex(gameSprite.getZIndex())
                        .build();

        // Set up input handler to route events to ControlSystem
        sprite.setInputHandler(createSpriteInputHandler(gameSprite.getEntityId()));

        return sprite;
    }

    private void updateRenderingSprite(
            ca.samanthaireland.lightning.engine.rendering.render2d.Sprite renderingSprite,
            Sprite gameSprite) {
        renderingSprite.setX((int) gameSprite.getX());
        renderingSprite.setY((int) gameSprite.getY());
        renderingSprite.setSizeX((int) gameSprite.getWidth());
        renderingSprite.setSizeY((int) gameSprite.getHeight());
        renderingSprite.setTexturePath(gameSprite.getTexturePath());
        renderingSprite.setZIndex(gameSprite.getZIndex());
    }

    private SpriteInputHandler createSpriteInputHandler(long entityId) {
        return new SpriteInputHandler() {
            @Override
            public boolean onMouseClick(
                    ca.samanthaireland.lightning.engine.rendering.render2d.Sprite sprite,
                    int button,
                    int action) {
                if (controlSystem == null) return false;

                Sprite gameSprite = gameSpriteMap.get(entityId);
                if (gameSprite == null) return false;

                if (action == 1) { // Press
                    controlSystem.onSpriteClicked(gameSprite, button);
                } else if (action == 0) { // Release
                    controlSystem.onSpriteReleased(gameSprite, button);
                }
                return true;
            }

            @Override
            public void onMouseEnter(ca.samanthaireland.lightning.engine.rendering.render2d.Sprite sprite) {
                if (controlSystem == null) return;

                Sprite gameSprite = gameSpriteMap.get(entityId);
                if (gameSprite != null) {
                    hoveredSprite = gameSprite;
                    controlSystem.onSpriteEntered(gameSprite);
                }
            }

            @Override
            public void onMouseExit(ca.samanthaireland.lightning.engine.rendering.render2d.Sprite sprite) {
                if (controlSystem == null) return;

                Sprite gameSprite = gameSpriteMap.get(entityId);
                if (gameSprite != null) {
                    if (hoveredSprite == gameSprite) {
                        hoveredSprite = null;
                    }
                    controlSystem.onSpriteExited(gameSprite);
                }
            }
        };
    }

    @Override
    public void start(Runnable onUpdate) {
        running.set(true);
        window.setOnUpdate(() -> {
            try {
                // Call control system update with key states
                if (controlSystem != null) {
                    controlSystem.onUpdate(key -> pressedKeys.contains(key));
                }
                // Call user update callback
                if (onUpdate != null) {
                    onUpdate.run();
                }
            } catch (Exception e) {
                handleError(e);
            }
        });

        try {
            window.run();
        } finally {
            running.set(false);
        }
    }

    @Override
    public void startAsync(Runnable onUpdate) {
        Thread renderThread = new Thread(() -> start(onUpdate), "GameRenderer-Thread");
        renderThread.start();
    }

    @Override
    public void runFrames(int frames, Runnable onUpdate) {
        running.set(true);
        window.setOnUpdate(() -> {
            try {
                if (controlSystem != null) {
                    controlSystem.onUpdate(key -> pressedKeys.contains(key));
                }
                if (onUpdate != null) {
                    onUpdate.run();
                }
            } catch (Exception e) {
                handleError(e);
            }
        });

        try {
            window.runFrames(frames);
        } finally {
            running.set(false);
        }
    }

    @Override
    public void stop() {
        window.stop();
        running.set(false);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setOnError(Consumer<Exception> handler) {
        this.errorHandler = handler;
    }

    @Override
    public void dispose() {
        stop();
        window.clearSprites();
        window.clearComponents();
        renderingSpriteMap.clear();
        gameSpriteMap.clear();
    }

    private void handleError(Exception e) {
        if (errorHandler != null) {
            errorHandler.accept(e);
        } else {
            e.printStackTrace();
        }
    }

    /**
     * Get the underlying rendering-core Window.
     * Useful for testing or adding GUI components.
     *
     * @return the Window instance
     */
    public Window getWindow() {
        return window;
    }
}
