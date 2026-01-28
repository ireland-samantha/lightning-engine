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


package ca.samanthaireland.lightning.engine.core.container;

/**
 * Represents the lifecycle status of an execution container.
 */
public enum ContainerStatus {
    /**
     * Container created but not yet started.
     * Modules can be installed, but tick execution is not active.
     */
    CREATED,

    /**
     * Container is initializing - loading classloader and modules.
     */
    STARTING,

    /**
     * Container is fully operational and can process ticks.
     */
    RUNNING,

    /**
     * Container is temporarily paused. Can be resumed.
     */
    PAUSED,

    /**
     * Container is shutting down. Resources are being released.
     */
    STOPPING,

    /**
     * Container has been fully stopped. All resources released.
     * Cannot be restarted - create a new container instead.
     */
    STOPPED
}
