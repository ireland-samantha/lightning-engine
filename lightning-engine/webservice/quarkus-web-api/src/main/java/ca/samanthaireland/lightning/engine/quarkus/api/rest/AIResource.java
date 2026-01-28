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


package ca.samanthaireland.lightning.engine.quarkus.api.rest;

import ca.samanthaireland.lightning.engine.core.GameSimulation;
import ca.samanthaireland.lightning.engine.core.match.Match;
import ca.samanthaireland.lightning.engine.internal.ext.ai.AIManager;
import ca.samanthaireland.lightning.engine.quarkus.api.dto.AIResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.resteasy.reactive.RestForm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * REST resource for managing AI.
 *
 * <p>Provides endpoints to:
 * <ul>
 *   <li>Upload JAR AI</li>
 *   <li>List available AI</li>
 *   <li>Get AI details</li>
 *   <li>Delete/uninstall AI</li>
 * </ul>
 */
@Path("/api/ai")
@Produces(MediaType.APPLICATION_JSON)
public class AIResource {
    private static final Logger log = LoggerFactory.getLogger(AIResource.class);

    @Inject
    AIManager aiManager;

    @Inject
    GameSimulation gameSimulation;

    /**
     * Get all available AI.
     *
     * @return list of AI information
     */
    @GET
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public List<AIResponse> getAllAI() {
        return aiManager.getAvailableAIs().stream()
                .map(this::toAIResponse)
                .toList();
    }

    /**
     * Get a specific AI by name.
     *
     * @param aiName the AI name
     * @return the AI information
     */
    @GET
    @Path("/{aiName}")
    @RolesAllowed({"admin", "command_manager", "view_only"})
    public Response getAI(@PathParam("aiName") String aiName) {
        if (!aiManager.hasAI(aiName)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"AI not found: " + aiName + "\"}")
                    .build();
        }

        return Response.ok(toAIResponse(aiName)).build();
    }

    /**
     * Upload a JAR AI.
     *
     * <p>The JAR file will be copied to the AI directory and loaded.
     *
     * @param file the JAR file to upload
     * @return the newly installed AI information
     */
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed("admin")
    public Response uploadAI(@RestForm("file") FileUpload file) {
        if (file == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"No file provided\"}")
                    .build();
        }

        String fileName = file.fileName();
        if (fileName == null || !fileName.endsWith(".jar")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"File must be a JAR file\"}")
                    .build();
        }

        try {
            java.nio.file.Path uploadedPath = file.uploadedFile();
            java.nio.file.Path tempJar = Files.createTempFile("ai-", ".jar");
            Files.copy(uploadedPath, tempJar, StandardCopyOption.REPLACE_EXISTING);

            aiManager.installAI(tempJar);

            Files.deleteIfExists(tempJar);

            log.info("Successfully uploaded AI: {}", fileName);

            return Response.status(Response.Status.CREATED)
                    .entity(getAllAI())
                    .build();

        } catch (IOException e) {
            log.error("Failed to upload AI: {}", fileName, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Failed to upload AI: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Uninstall an AI by name.
     *
     * <p>This removes the AI from the active AI list.
     * Note: The JAR file is not deleted from disk.
     *
     * @param aiName the AI name to uninstall
     * @return success status
     */
    @DELETE
    @Path("/{aiName}")
    @RolesAllowed("admin")
    public Response deleteAI(@PathParam("aiName") String aiName) {
        boolean removed = aiManager.uninstallAI(aiName);

        if (removed) {
            log.info("Uninstalled AI: {}", aiName);
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"AI not found: " + aiName + "\"}")
                    .build();
        }
    }

    /**
     * Reload all AI from disk.
     *
     * @return list of all available AI after reload
     */
    @POST
    @Path("/reload")
    @RolesAllowed("admin")
    public Response reloadAI() {
        try {
            aiManager.reset();
            aiManager.reloadInstalled();
            log.info("Reloaded all AI");
            return Response.ok(getAllAI()).build();
        } catch (IOException e) {
            log.error("Failed to reload AI", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Failed to reload AI: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    private AIResponse toAIResponse(String aiName) {
        int enabledMatches = countEnabledMatches(aiName);
        return new AIResponse(aiName, enabledMatches);
    }

    private int countEnabledMatches(String aiName) {
        return (int) gameSimulation.getAllMatches().stream()
                .filter(match -> isAIEnabledInMatch(match, aiName))
                .count();
    }

    private boolean isAIEnabledInMatch(Match match, String aiName) {
        if (match.enabledAIs() == null) {
            return false;
        }
        return match.enabledAIs().contains(aiName);
    }
}
