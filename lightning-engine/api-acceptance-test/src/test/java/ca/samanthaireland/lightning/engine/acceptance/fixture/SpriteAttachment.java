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

package ca.samanthaireland.lightning.engine.acceptance.fixture;

/**
 * DTO for attaching a sprite to an entity.
 *
 * @param entityId the entity to attach the sprite to
 * @param resourceId the resource ID for the sprite texture
 * @param width the sprite width
 * @param height the sprite height
 * @param visible whether the sprite is visible
 */
public record SpriteAttachment(
        long entityId,
        long resourceId,
        int width,
        int height,
        boolean visible
) {
    /**
     * Creates a sprite attachment with default size and visibility.
     */
    public static SpriteAttachment forEntity(long entityId, long resourceId) {
        return new SpriteAttachment(entityId, resourceId, 32, 32, true);
    }

    /**
     * Creates a sprite attachment with specified size.
     */
    public static SpriteAttachment sized(long entityId, long resourceId, int width, int height) {
        return new SpriteAttachment(entityId, resourceId, width, height, true);
    }

    /**
     * Creates a builder for more complex configurations.
     */
    public static Builder builder(long entityId, long resourceId) {
        return new Builder(entityId, resourceId);
    }

    public static class Builder {
        private final long entityId;
        private final long resourceId;
        private int width = 32;
        private int height = 32;
        private boolean visible = true;

        private Builder(long entityId, long resourceId) {
            this.entityId = entityId;
            this.resourceId = resourceId;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder visible(boolean visible) {
            this.visible = visible;
            return this;
        }

        public SpriteAttachment build() {
            return new SpriteAttachment(entityId, resourceId, width, height, visible);
        }
    }
}
