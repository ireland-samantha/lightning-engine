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

import ca.samanthaireland.lightning.engine.util.IdGeneratorV2;
import lombok.ToString;

/**
 * A component with an associated permission level.
 *
 * <p>Permission components allow modules to control how other modules can access
 * their data. The permission level determines whether other modules can read,
 * write, or have no access to this component.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * // Private component - only this module can access
 * PermissionComponent internalState = PermissionComponent.create("INTERNAL_STATE", PermissionLevel.PRIVATE);
 *
 * // Read-only component - others can read but not modify
 * PermissionComponent position = PermissionComponent.create("POSITION", PermissionLevel.READ);
 *
 * // Writable component - any module can read and write
 * PermissionComponent velocity = PermissionComponent.create("VELOCITY", PermissionLevel.WRITE);
 * }</pre>
 *
 * @see PermissionLevel
 * @see BaseComponent
 */
@ToString(callSuper = true)
public class PermissionComponent extends BaseComponent {

    private final PermissionLevel permissionLevel;

    /**
     * Create a permission component with the specified ID, name, and permission level.
     *
     * @param id the component ID (must be positive)
     * @param name the component name (must not be null or blank)
     * @param permissionLevel the permission level for this component
     * @throws IllegalArgumentException if id is not positive, name is null/blank, or permissionLevel is null
     */
    public PermissionComponent(long id, String name, PermissionLevel permissionLevel) {
        super(id, name);
        if (permissionLevel == null) {
            throw new IllegalArgumentException("Permission level must not be null");
        }
        this.permissionLevel = permissionLevel;
    }

    /**
     * Create a permission component with an auto-generated ID.
     *
     * @param name the component name (must not be null or blank)
     * @param permissionLevel the permission level for this component
     * @throws IllegalArgumentException if name is null/blank or permissionLevel is null
     */
    public PermissionComponent(String name, PermissionLevel permissionLevel) {
        super(name);
        if (permissionLevel == null) {
            throw new IllegalArgumentException("Permission level must not be null");
        }
        this.permissionLevel = permissionLevel;
    }

    /**
     * Factory method to create a permission component with an auto-generated ID.
     *
     * @param name the component name
     * @param permissionLevel the permission level
     * @return a new permission component
     */
    public static PermissionComponent create(String name, PermissionLevel permissionLevel) {
        return new PermissionComponent(name, permissionLevel);
    }

    /**
     * Factory method to create a permission component with a specific ID.
     *
     * @param id the component ID
     * @param name the component name
     * @param permissionLevel the permission level
     * @return a new permission component
     */
    public static PermissionComponent withId(long id, String name, PermissionLevel permissionLevel) {
        return new PermissionComponent(id, name, permissionLevel);
    }

    /**
     * Get the permission level for this component.
     *
     * @return the permission level
     */
    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }
}
