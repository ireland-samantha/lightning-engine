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

package ca.samanthaireland.lightning.controlplane.module.exception;

import ca.samanthaireland.lightning.controlplane.exception.ControlPlaneException;

/**
 * Exception thrown when module distribution to a node fails.
 */
public class ModuleDistributionException extends ControlPlaneException {

    private final String moduleName;
    private final String nodeId;

    public ModuleDistributionException(String moduleName, String nodeId, String message) {
        super("MODULE_DISTRIBUTION_FAILED", "Failed to distribute module " + moduleName + " to node " + nodeId + ": " + message);
        this.moduleName = moduleName;
        this.nodeId = nodeId;
    }

    public ModuleDistributionException(String moduleName, String nodeId, Throwable cause) {
        super("MODULE_DISTRIBUTION_FAILED", "Failed to distribute module " + moduleName + " to node " + nodeId, cause);
        this.moduleName = moduleName;
        this.nodeId = nodeId;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getNodeId() {
        return nodeId;
    }
}
