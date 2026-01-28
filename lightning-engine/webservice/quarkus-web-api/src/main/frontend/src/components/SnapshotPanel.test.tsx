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

import { screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render } from "../test/testUtils";
import SnapshotPanel from "./SnapshotPanel";

// Mock the useSnapshot hook
vi.mock("../hooks/useSnapshot", () => ({
  useSnapshot: vi.fn(),
}));

import { useSnapshot } from "../hooks/useSnapshot";

const mockUseSnapshot = useSnapshot as ReturnType<typeof vi.fn>;

describe("SnapshotPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders loading state when no snapshot", () => {
    mockUseSnapshot.mockReturnValue({
      snapshot: null,
      connected: false,
      error: null,
      requestSnapshot: vi.fn(),
    });

    render(<SnapshotPanel containerId={1} matchId={1} />);

    expect(screen.getByRole("progressbar")).toBeInTheDocument();
  });

  it("renders error state", () => {
    mockUseSnapshot.mockReturnValue({
      snapshot: null,
      connected: false,
      error: "Connection failed",
      requestSnapshot: vi.fn(),
    });

    render(<SnapshotPanel containerId={1} matchId={1} />);

    expect(screen.getByText(/connection failed/i)).toBeInTheDocument();
  });

  it("renders snapshot data", async () => {
    const mockSnapshot = {
      matchId: 1,
      tick: 42,
      modules: [
        {
          name: "GameModule",
          version: "1.0",
          components: [
            { name: "POSITION_X", values: [100, 200, 300] },
            { name: "POSITION_Y", values: [50, 60, 70] },
          ],
        },
      ],
    };

    mockUseSnapshot.mockReturnValue({
      snapshot: mockSnapshot,
      connected: true,
      error: null,
      requestSnapshot: vi.fn(),
    });

    render(<SnapshotPanel containerId={1} matchId={1} />);

    await waitFor(() => {
      expect(screen.getByText(/tick 42/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/3 entities/i)).toBeInTheDocument();
    expect(screen.getByText(/gamemodule/i)).toBeInTheDocument();
  });

  it("shows connected status indicator", () => {
    mockUseSnapshot.mockReturnValue({
      snapshot: null,
      connected: true,
      error: null,
      requestSnapshot: vi.fn(),
    });

    render(<SnapshotPanel containerId={1} matchId={1} />);

    // Just verify the component renders without error
    expect(screen.getByText(/live snapshot/i)).toBeInTheDocument();
  });

  it("displays match ID in subheader", () => {
    mockUseSnapshot.mockReturnValue({
      snapshot: null,
      connected: false,
      error: null,
      requestSnapshot: vi.fn(),
    });

    render(<SnapshotPanel containerId={1} matchId={5} />);

    expect(screen.getByText(/match 5/i)).toBeInTheDocument();
  });
});
