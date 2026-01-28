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

package ca.samanthaireland.lightning.controlplane.module.repository;

import ca.samanthaireland.lightning.controlplane.module.model.ModuleMetadata;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Repository for storing and retrieving module metadata and JAR files.
 */
public interface ModuleRepository {

    /**
     * Saves module metadata and JAR file.
     *
     * @param metadata the module metadata
     * @param jarData  the JAR file contents
     * @return the saved metadata
     */
    ModuleMetadata save(ModuleMetadata metadata, byte[] jarData);

    /**
     * Finds module metadata by name and version.
     *
     * @param name    the module name
     * @param version the module version
     * @return the metadata if found
     */
    Optional<ModuleMetadata> findByNameAndVersion(String name, String version);

    /**
     * Finds all versions of a module by name.
     *
     * @param name the module name
     * @return list of all versions
     */
    List<ModuleMetadata> findByName(String name);

    /**
     * Finds all modules.
     *
     * @return list of all module metadata
     */
    List<ModuleMetadata> findAll();

    /**
     * Gets the JAR file contents for a module.
     *
     * @param name    the module name
     * @param version the module version
     * @return input stream for the JAR file, or empty if not found
     */
    Optional<InputStream> getJarFile(String name, String version);

    /**
     * Deletes a module by name and version.
     *
     * @param name    the module name
     * @param version the module version
     * @return true if deleted, false if not found
     */
    boolean delete(String name, String version);

    /**
     * Checks if a module exists.
     *
     * @param name    the module name
     * @param version the module version
     * @return true if exists
     */
    boolean exists(String name, String version);
}
