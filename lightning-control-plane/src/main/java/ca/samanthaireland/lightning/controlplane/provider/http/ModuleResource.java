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

package ca.samanthaireland.lightning.controlplane.provider.http;

import ca.samanthaireland.lightning.controlplane.provider.dto.ModuleResponse;
import ca.samanthaireland.lightning.controlplane.module.exception.ModuleNotFoundException;
import ca.samanthaireland.lightning.controlplane.module.model.ModuleMetadata;
import ca.samanthaireland.lightning.controlplane.module.service.ModuleRegistryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.RestForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;

/**
 * REST resource for module registry operations.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/modules - Upload a module</li>
 *   <li>GET /api/modules - List all modules</li>
 *   <li>GET /api/modules/{name} - List versions of a module</li>
 *   <li>GET /api/modules/{name}/{version} - Get module metadata</li>
 *   <li>GET /api/modules/{name}/{version}/download - Download module JAR</li>
 *   <li>DELETE /api/modules/{name}/{version} - Delete a module</li>
 *   <li>POST /api/modules/{name}/{version}/distribute - Distribute to all nodes</li>
 *   <li>POST /api/modules/{name}/{version}/distribute/{nodeId} - Distribute to specific node</li>
 * </ul>
 */
@Path("/api/modules")
@Produces(MediaType.APPLICATION_JSON)
public class ModuleResource {
    private static final Logger log = LoggerFactory.getLogger(ModuleResource.class);

    private final ModuleRegistryService moduleRegistryService;

    @Inject
    public ModuleResource(ModuleRegistryService moduleRegistryService) {
        this.moduleRegistryService = moduleRegistryService;
    }

    /**
     * Uploads a new module to the registry.
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadModule(
            @RestForm("name") String name,
            @RestForm("version") String version,
            @RestForm("description") String description,
            @RestForm("file") FileUpload file
    ) throws IOException {
        log.info("Upload module request: name={}, version={}, file={}",
                name, version, file.fileName());

        byte[] jarData = Files.readAllBytes(file.uploadedFile());

        ModuleMetadata metadata = moduleRegistryService.uploadModule(
                name,
                version,
                description,
                file.fileName(),
                jarData,
                "api"  // TODO: get from auth context
        );

        return Response.created(URI.create("/api/modules/" + name + "/" + version))
                .entity(ModuleResponse.from(metadata))
                .build();
    }

    /**
     * Lists all modules in the registry.
     */
    @GET
    public List<ModuleResponse> listModules() {
        return moduleRegistryService.listAllModules().stream()
                .map(ModuleResponse::from)
                .toList();
    }

    /**
     * Lists all versions of a specific module.
     */
    @GET
    @Path("/{name}")
    public List<ModuleResponse> listModuleVersions(@PathParam("name") String name) {
        return moduleRegistryService.getModuleVersions(name).stream()
                .map(ModuleResponse::from)
                .toList();
    }

    /**
     * Gets metadata for a specific module version.
     */
    @GET
    @Path("/{name}/{version}")
    public ModuleResponse getModule(
            @PathParam("name") String name,
            @PathParam("version") String version
    ) {
        return moduleRegistryService.getModule(name, version)
                .map(ModuleResponse::from)
                .orElseThrow(() -> new ModuleNotFoundException(name, version));
    }

    /**
     * Downloads the module JAR file.
     */
    @GET
    @Path("/{name}/{version}/download")
    @Produces("application/java-archive")
    public Response downloadModule(
            @PathParam("name") String name,
            @PathParam("version") String version
    ) {
        ModuleMetadata metadata = moduleRegistryService.getModule(name, version)
                .orElseThrow(() -> new ModuleNotFoundException(name, version));

        InputStream jarStream = moduleRegistryService.downloadModule(name, version);

        StreamingOutput output = outputStream -> {
            try (jarStream) {
                jarStream.transferTo(outputStream);
            }
        };

        return Response.ok(output)
                .header("Content-Disposition", "attachment; filename=\"" + metadata.fileName() + "\"")
                .header("Content-Length", metadata.fileSize())
                .build();
    }

    /**
     * Deletes a module from the registry.
     */
    @DELETE
    @Path("/{name}/{version}")
    public Response deleteModule(
            @PathParam("name") String name,
            @PathParam("version") String version
    ) {
        log.info("Delete module request: {}:{}", name, version);

        moduleRegistryService.deleteModule(name, version);

        return Response.noContent().build();
    }

    /**
     * Distributes a module to all healthy nodes.
     */
    @POST
    @Path("/{name}/{version}/distribute")
    public DistributionResult distributeToAllNodes(
            @PathParam("name") String name,
            @PathParam("version") String version
    ) {
        log.info("Distribute module request: {}:{} to all nodes", name, version);

        int count = moduleRegistryService.distributeToAllNodes(name, version);

        return new DistributionResult(name, version, count, null);
    }

    /**
     * Distributes a module to a specific node.
     */
    @POST
    @Path("/{name}/{version}/distribute/{nodeId}")
    public Response distributeToNode(
            @PathParam("name") String name,
            @PathParam("version") String version,
            @PathParam("nodeId") String nodeId
    ) {
        log.info("Distribute module request: {}:{} to node {}", name, version, nodeId);

        moduleRegistryService.distributeToNode(name, version, nodeId);

        return Response.ok(new DistributionResult(name, version, 1, nodeId)).build();
    }

    /**
     * Result of a module distribution operation.
     */
    public record DistributionResult(
            String moduleName,
            String moduleVersion,
            int nodesUpdated,
            String targetNode
    ) {
    }
}
