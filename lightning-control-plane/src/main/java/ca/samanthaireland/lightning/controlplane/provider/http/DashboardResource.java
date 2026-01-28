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

import ca.samanthaireland.lightning.controlplane.provider.dto.DashboardOverview;
import ca.samanthaireland.lightning.controlplane.provider.dto.PagedResponse;
import ca.samanthaireland.lightning.controlplane.provider.service.DashboardService;
import ca.samanthaireland.lightning.controlplane.provider.dto.MatchResponse;
import ca.samanthaireland.lightning.controlplane.match.model.MatchStatus;
import ca.samanthaireland.lightning.controlplane.provider.dto.NodeResponse;
import ca.samanthaireland.lightning.controlplane.node.model.NodeStatus;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/**
 * REST resource for dashboard endpoints.
 * Provides aggregated views of cluster state for the admin dashboard.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET /api/dashboard/overview - Complete cluster overview</li>
 *   <li>GET /api/dashboard/nodes - Paginated node list</li>
 *   <li>GET /api/dashboard/matches - Paginated match list</li>
 * </ul>
 */
@Path("/api/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DashboardResource {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final DashboardService dashboardService;

    @Inject
    public DashboardResource(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Gets the complete dashboard overview with all cluster metrics.
     * This is the main endpoint for the dashboard home page.
     *
     * @return the dashboard overview
     */
    @GET
    @Path("/overview")
    public DashboardOverview getOverview() {
        return dashboardService.getOverview();
    }

    /**
     * Gets a paginated list of nodes with optional filtering.
     *
     * @param page     page number (0-indexed, default: 0)
     * @param pageSize items per page (default: 20, max: 100)
     * @param status   optional status filter (HEALTHY, DRAINING)
     * @return paginated list of nodes
     */
    @GET
    @Path("/nodes")
    public PagedResponse<NodeResponse> getNodes(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize,
            @QueryParam("status") NodeStatus status
    ) {
        // Validate and clamp page size
        pageSize = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
        page = Math.max(0, page);

        return dashboardService.getNodes(page, pageSize, status);
    }

    /**
     * Gets a paginated list of matches with optional filtering.
     *
     * @param page     page number (0-indexed, default: 0)
     * @param pageSize items per page (default: 20, max: 100)
     * @param status   optional status filter (CREATING, RUNNING, FINISHED, ERROR)
     * @param nodeId   optional node ID filter
     * @return paginated list of matches
     */
    @GET
    @Path("/matches")
    public PagedResponse<MatchResponse> getMatches(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize,
            @QueryParam("status") MatchStatus status,
            @QueryParam("nodeId") String nodeId
    ) {
        // Validate and clamp page size
        pageSize = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
        page = Math.max(0, page);

        return dashboardService.getMatches(page, pageSize, status, nodeId);
    }
}
