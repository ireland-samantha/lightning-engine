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


package ca.samanthaireland.lightning.engine.auth.module;

import java.util.Map;
import java.util.Set;

/**
 * Represents the authentication token for a module, containing JWT claims
 * about what permissions the module has.
 *
 * <p>Permission claims are stored in the format {@code moduleName.componentName} with values
 * of {@code owner}, {@code read}, or {@code write}. For example:
 * <ul>
 *   <li>{@code EntityModule.ENTITY_TYPE.owner} - EntityModule owns this component</li>
 *   <li>{@code RigidBodyModule.VELOCITY_X.read} - This module can read VELOCITY_X</li>
 *   <li>{@code GridMapModule.POSITION_X.write} - This module can write POSITION_X</li>
 * </ul>
 *
 * <p>Claims include:
 * <ul>
 *   <li>moduleName - The name of the module this token was issued to</li>
 *   <li>componentPermissions - Map of component keys to permission levels</li>
 *   <li>superuser - Whether this module bypasses permission checks (e.g., EntityModule)</li>
 * </ul>
 *
 * <p>This is an immutable record that gets created when verifying a JWT.
 *
 * @param moduleName the name of the module
 * @param componentPermissions map of "moduleName.componentName" to permission level (owner/read/write)
 * @param superuser whether this module has superuser privileges
 * @param jwtToken the raw JWT token string
 */
public record ModuleAuthToken(
        String moduleName,
        Map<String, ComponentPermission> componentPermissions,
        boolean superuser,
        String jwtToken
) {
    /**
     * Permission level for a component.
     */
    public enum ComponentPermission {
        /** Full access - the module owns this component */
        OWNER,
        /** Read-only access to another module's component */
        READ,
        /** Read and write access to another module's component */
        WRITE
    }

    /**
     * JWT claim key for the module name.
     */
    public static final String CLAIM_MODULE_NAME = "module_name";

    /**
     * JWT claim key for component permissions map.
     */
    public static final String CLAIM_COMPONENT_PERMISSIONS = "component_permissions";

    /**
     * JWT claim key for superuser status.
     */
    public static final String CLAIM_SUPERUSER = "superuser";

    /**
     * Build a component permission key.
     *
     * @param ownerModuleName the module that owns the component
     * @param componentName the component name
     * @return the permission key in format "moduleName.componentName"
     */
    public static String permissionKey(String ownerModuleName, String componentName) {
        return ownerModuleName + "." + componentName;
    }

    /**
     * Check if this module owns a specific component.
     *
     * @param ownerModuleName the module that declares the component
     * @param componentName the component name
     * @return true if the module owns this component
     */
    public boolean ownsComponent(String ownerModuleName, String componentName) {
        String key = permissionKey(ownerModuleName, componentName);
        ComponentPermission permission = componentPermissions.get(key);
        return permission == ComponentPermission.OWNER;
    }

    /**
     * Check if this module can read a component based on JWT claims.
     * Superusers can always read. Otherwise, requires OWNER, READ, or WRITE permission.
     *
     * @param ownerModuleName the module that declares the component
     * @param componentName the component name
     * @return true if the module can read this component
     */
    public boolean canRead(String ownerModuleName, String componentName) {
        if (superuser) {
            return true;
        }
        String key = permissionKey(ownerModuleName, componentName);
        ComponentPermission permission = componentPermissions.get(key);
        // OWNER, READ, or WRITE all allow reading
        return permission != null;
    }

    /**
     * Check if this module can write a component based on JWT claims.
     * Superusers can always write. Otherwise, requires OWNER or WRITE permission.
     *
     * @param ownerModuleName the module that declares the component
     * @param componentName the component name
     * @return true if the module can write this component
     */
    public boolean canWrite(String ownerModuleName, String componentName) {
        if (superuser) {
            return true;
        }
        String key = permissionKey(ownerModuleName, componentName);
        ComponentPermission permission = componentPermissions.get(key);
        return permission == ComponentPermission.OWNER || permission == ComponentPermission.WRITE;
    }

    /**
     * Get all permission claims as a formatted string for logging.
     *
     * @return formatted permission claims
     */
    public String formatPermissions() {
        if (componentPermissions.isEmpty()) {
            return "(no permissions)";
        }
        StringBuilder sb = new StringBuilder();
        componentPermissions.forEach((key, permission) ->
                sb.append("\n    ").append(key).append(".").append(permission.name().toLowerCase()));
        return sb.toString();
    }
}
