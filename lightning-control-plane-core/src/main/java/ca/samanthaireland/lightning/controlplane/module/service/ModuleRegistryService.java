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

package ca.samanthaireland.lightning.controlplane.module.service;

import ca.samanthaireland.lightning.controlplane.module.model.ModuleMetadata;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing the module registry.
 */
public interface ModuleRegistryService {

    /**
     * Uploads a new module to the registry.
     *
     * @param name        module name
     * @param version     module version
     * @param description module description
     * @param fileName    original filename
     * @param jarData     JAR file contents
     * @param uploadedBy  uploader identifier
     * @return the created metadata
     */
    ModuleMetadata uploadModule(
            String name,
            String version,
            String description,
            String fileName,
            byte[] jarData,
            String uploadedBy
    );

    /**
     * Gets metadata for a specific module version.
     *
     * @param name    module name
     * @param version module version
     * @return the metadata if found
     */
    Optional<ModuleMetadata> getModule(String name, String version);

    /**
     * Lists all versions of a module.
     *
     * @param name module name
     * @return list of all versions
     */
    List<ModuleMetadata> getModuleVersions(String name);

    /**
     * Lists all modules in the registry.
     *
     * @return list of all modules
     */
    List<ModuleMetadata> listAllModules();

    /**
     * Downloads a module JAR file.
     *
     * @param name    module name
     * @param version module version
     * @return input stream for the JAR file
     */
    InputStream downloadModule(String name, String version);

    /**
     * Deletes a module from the registry.
     *
     * @param name    module name
     * @param version module version
     */
    void deleteModule(String name, String version);

    /**
     * Distributes a module to a specific node.
     *
     * @param name    module name
     * @param version module version
     * @param nodeId  target node ID
     */
    void distributeToNode(String name, String version, String nodeId);

    /**
     * Distributes a module to all healthy nodes.
     *
     * @param name    module name
     * @param version module version
     * @return number of nodes the module was distributed to
     */
    int distributeToAllNodes(String name, String version);
}
