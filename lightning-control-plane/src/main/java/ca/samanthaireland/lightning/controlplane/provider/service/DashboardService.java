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

package ca.samanthaireland.lightning.controlplane.provider.service;

import ca.samanthaireland.lightning.controlplane.provider.dto.DashboardOverview;
import ca.samanthaireland.lightning.controlplane.provider.dto.PagedResponse;
import ca.samanthaireland.lightning.controlplane.provider.dto.MatchResponse;
import ca.samanthaireland.lightning.controlplane.match.model.MatchStatus;
import ca.samanthaireland.lightning.controlplane.provider.dto.NodeResponse;
import ca.samanthaireland.lightning.controlplane.node.model.NodeStatus;

/**
 * Service for aggregating data for the dashboard.
 */
public interface DashboardService {

    /**
     * Gets the complete dashboard overview.
     *
     * @return the dashboard overview
     */
    DashboardOverview getOverview();

    /**
     * Gets a paginated list of nodes with optional filtering.
     *
     * @param page     page number (0-indexed)
     * @param pageSize items per page
     * @param status   optional status filter
     * @return paginated node list
     */
    PagedResponse<NodeResponse> getNodes(int page, int pageSize, NodeStatus status);

    /**
     * Gets a paginated list of matches with optional filtering.
     *
     * @param page     page number (0-indexed)
     * @param pageSize items per page
     * @param status   optional status filter
     * @param nodeId   optional node ID filter
     * @return paginated match list
     */
    PagedResponse<MatchResponse> getMatches(int page, int pageSize, MatchStatus status, String nodeId);
}
