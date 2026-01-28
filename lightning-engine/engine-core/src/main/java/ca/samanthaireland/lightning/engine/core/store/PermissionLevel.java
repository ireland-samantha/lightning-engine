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


package ca.samanthaireland.lightning.engine.core.store;

/**
 * Permission levels for ECS components.
 *
 * <p>Controls how modules can access components defined by other modules:
 * <ul>
 *   <li>{@link #PRIVATE} - Only the owning module can read or write</li>
 *   <li>{@link #READ} - Any module can read, only the owner can write</li>
 *   <li>{@link #WRITE} - Any module can read and write</li>
 * </ul>
 *
 * <p>A module always has full access to its own components regardless of permission level.
 */
public enum PermissionLevel {

    /**
     * Only the owning module can read or write this component.
     */
    PRIVATE,

    /**
     * Any module can read, but only the owning module can write.
     */
    READ,

    /**
     * Any module can read and write this component.
     */
    WRITE
}
