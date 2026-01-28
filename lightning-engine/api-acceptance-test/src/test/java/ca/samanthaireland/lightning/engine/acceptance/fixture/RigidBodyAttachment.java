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
 * DTO for attaching a rigid body to an entity.
 *
 * @param entityId the entity to attach the rigid body to
 * @param positionX the initial X position
 * @param positionY the initial Y position
 * @param velocityX the initial X velocity
 * @param velocityY the initial Y velocity
 * @param mass the mass of the rigid body
 * @param linearDrag the linear drag coefficient
 */
public record RigidBodyAttachment(
        long entityId,
        float positionX,
        float positionY,
        float velocityX,
        float velocityY,
        float mass,
        float linearDrag
) {
    /**
     * Creates a rigid body attachment with default mass and no velocity.
     */
    public static RigidBodyAttachment at(long entityId, float x, float y) {
        return new RigidBodyAttachment(entityId, x, y, 0, 0, 1.0f, 0.01f);
    }

    /**
     * Creates a rigid body attachment with position and velocity.
     */
    public static RigidBodyAttachment withVelocity(long entityId, float x, float y, float vx, float vy) {
        return new RigidBodyAttachment(entityId, x, y, vx, vy, 1.0f, 0.01f);
    }

    /**
     * Creates a builder for more complex configurations.
     */
    public static Builder builder(long entityId) {
        return new Builder(entityId);
    }

    public static class Builder {
        private final long entityId;
        private float positionX;
        private float positionY;
        private float velocityX;
        private float velocityY;
        private float mass = 1.0f;
        private float linearDrag = 0.01f;

        private Builder(long entityId) {
            this.entityId = entityId;
        }

        public Builder position(float x, float y) {
            this.positionX = x;
            this.positionY = y;
            return this;
        }

        public Builder velocity(float vx, float vy) {
            this.velocityX = vx;
            this.velocityY = vy;
            return this;
        }

        public Builder mass(float mass) {
            this.mass = mass;
            return this;
        }

        public Builder linearDrag(float linearDrag) {
            this.linearDrag = linearDrag;
            return this;
        }

        public RigidBodyAttachment build() {
            return new RigidBodyAttachment(entityId, positionX, positionY, velocityX, velocityY, mass, linearDrag);
        }
    }
}
