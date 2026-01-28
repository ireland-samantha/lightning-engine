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

import {
    Compress as CompressIcon, History as HistoryIcon, Refresh as RefreshIcon, Timeline as TimelineIcon
} from "@mui/icons-material";
import {
    Alert, Box, Card,
    CardContent,
    CardHeader, Chip,
    CircularProgress, Divider,
    Grid, IconButton, List,
    ListItem,
    ListItemButton,
    ListItemText, Paper, Tooltip, Typography
} from "@mui/material";
import { useState } from "react";
import { useContainerContext } from "../contexts/ContainerContext";
import {
    useGetContainerHistorySnapshotsQuery, useGetContainerHistorySummaryQuery,
    useGetContainerMatchHistorySummaryQuery, useGetDeltaQuery
} from "../store/api/apiSlice";

export const HistoryPanel: React.FC = () => {
  const { selectedContainerId } = useContainerContext();
  const [selectedMatchId, setSelectedMatchId] = useState<number | null>(null);
  const [selectedTick, setSelectedTick] = useState<number | null>(null);
  const [previousTick, setPreviousTick] = useState<number | null>(null);

  // Fetch history summary for the selected container
  const {
    data: summary,
    isLoading: summaryLoading,
    error: summaryError,
    refetch: refetchSummary,
  } = useGetContainerHistorySummaryQuery(selectedContainerId!, {
    skip: !selectedContainerId,
  });

  // Fetch match summaries for all matches
  // We'll use a map approach - fetch each match summary individually
  const matchIds = summary?.matchIds ?? [];

  // Fetch snapshots for selected match
  const {
    data: snapshots = [],
    isLoading: snapshotsLoading,
    refetch: refetchSnapshots,
  } = useGetContainerHistorySnapshotsQuery(
    {
      containerId: selectedContainerId!,
      matchId: selectedMatchId!,
      limit: 100,
    },
    { skip: !selectedContainerId || !selectedMatchId },
  );

  // Fetch delta between ticks
  const { data: delta } = useGetDeltaQuery(
    {
      matchId: selectedMatchId!,
      fromTick: previousTick!,
      toTick: selectedTick!,
    },
    {
      skip: !selectedMatchId || previousTick === null || selectedTick === null,
    },
  );

  const loading = summaryLoading || snapshotsLoading;
  const error = summaryError ? "Failed to load history summary" : null;

  const handleMatchSelect = (matchId: number) => {
    setSelectedMatchId(matchId);
    setSelectedTick(null);
    setPreviousTick(null);
  };

  const handleSnapshotSelect = (tick: number) => {
    if (selectedTick !== null) {
      setPreviousTick(selectedTick);
    }
    setSelectedTick(tick);
  };

  const handleRefresh = () => {
    refetchSummary();
    if (selectedMatchId) {
      refetchSnapshots();
    }
  };

  if (!selectedContainerId) {
    return (
      <Card sx={{ height: "100%", display: "flex", flexDirection: "column" }}>
        <CardHeader title="Snapshot History" avatar={<HistoryIcon />} />
        <CardContent
          sx={{
            flexGrow: 1,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          <Typography color="text.secondary" textAlign="center">
            Select a container to view snapshot history
          </Typography>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card sx={{ height: "100%", display: "flex", flexDirection: "column" }}>
      <CardHeader
        title="Snapshot History"
        avatar={<HistoryIcon />}
        action={
          <Tooltip title="Refresh">
            <IconButton onClick={handleRefresh} disabled={loading}>
              <RefreshIcon />
            </IconButton>
          </Tooltip>
        }
      />
      <CardContent sx={{ flexGrow: 1, overflow: "auto" }}>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {loading && !summary && (
          <Box sx={{ display: "flex", justifyContent: "center", p: 4 }}>
            <CircularProgress />
          </Box>
        )}

        {summary && (
          <>
            <Box sx={{ display: "flex", gap: 1, mb: 2, flexWrap: "wrap" }}>
              <Chip
                icon={<TimelineIcon />}
                label={`${summary.totalSnapshots} Total Snapshots`}
                color="primary"
                variant="outlined"
              />
              <Chip
                label={`${summary.matchCount} Matches`}
                color="secondary"
                variant="outlined"
              />
            </Box>

            <Grid container spacing={2}>
              {/* Match List */}
              <Grid size={{ xs: 12, md: 6 }}>
                <Paper variant="outlined" sx={{ height: 300 }}>
                  <Typography
                    variant="subtitle2"
                    sx={{ p: 1.5, bgcolor: "background.default" }}
                  >
                    Match History
                  </Typography>
                  <Divider />
                  <List dense sx={{ overflow: "auto", maxHeight: 250 }}>
                    {matchIds.map((matchId) => (
                      <MatchListItem
                        key={matchId}
                        containerId={selectedContainerId!}
                        matchId={matchId}
                        selected={selectedMatchId === matchId}
                        onClick={() => handleMatchSelect(matchId)}
                      />
                    ))}
                    {matchIds.length === 0 && (
                      <ListItem>
                        <ListItemText
                          secondary="No matches found"
                          sx={{ textAlign: "center" }}
                        />
                      </ListItem>
                    )}
                  </List>
                </Paper>
              </Grid>

              {/* Snapshot List */}
              <Grid size={{ xs: 12, md: 6 }}>
                <Paper variant="outlined" sx={{ height: 300 }}>
                  <Typography
                    variant="subtitle2"
                    sx={{ p: 1.5, bgcolor: "background.default" }}
                  >
                    Snapshots{" "}
                    {selectedMatchId ? `(Match ${selectedMatchId})` : ""}
                  </Typography>
                  <Divider />
                  <List dense sx={{ overflow: "auto", maxHeight: 250 }}>
                    {snapshots.map((snapshot) => (
                      <ListItem key={snapshot.tick} disablePadding>
                        <ListItemButton
                          selected={selectedTick === snapshot.tick}
                          onClick={() => handleSnapshotSelect(snapshot.tick)}
                        >
                          <ListItemText
                            primary={`Tick ${snapshot.tick}`}
                            secondary={`${snapshot.modules.length} modules`}
                          />
                        </ListItemButton>
                      </ListItem>
                    ))}
                    {selectedMatchId && snapshots.length === 0 && (
                      <ListItem>
                        <ListItemText
                          secondary="No snapshots found"
                          sx={{ textAlign: "center" }}
                        />
                      </ListItem>
                    )}
                    {!selectedMatchId && (
                      <ListItem>
                        <ListItemText
                          secondary="Select a match to view snapshots"
                          sx={{ textAlign: "center" }}
                        />
                      </ListItem>
                    )}
                  </List>
                </Paper>
              </Grid>
            </Grid>

            {/* Delta Information */}
            {delta && (
              <Paper variant="outlined" sx={{ mt: 2, p: 2 }}>
                <Box
                  sx={{ display: "flex", alignItems: "center", gap: 1, mb: 1 }}
                >
                  <CompressIcon color="primary" />
                  <Typography variant="subtitle1" fontWeight="medium">
                    Delta Compression
                  </Typography>
                </Box>
                <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
                  <Chip
                    label={`Tick ${delta.fromTick} → ${delta.toTick}`}
                    variant="outlined"
                    size="small"
                  />
                  {delta.changedCount !== undefined && (
                    <Chip
                      label={`${delta.changedCount} Changes`}
                      color="info"
                      variant="outlined"
                      size="small"
                    />
                  )}
                  <Chip
                    label={`+${delta.addedEntities.length} Added`}
                    color="success"
                    variant="outlined"
                    size="small"
                  />
                  <Chip
                    label={`-${delta.removedEntities.length} Removed`}
                    color="error"
                    variant="outlined"
                    size="small"
                  />
                </Box>
              </Paper>
            )}
          </>
        )}
      </CardContent>
    </Card>
  );
};

// Separate component for match list items to use individual RTK Query hooks
interface MatchListItemProps {
  containerId: number;
  matchId: number;
  selected: boolean;
  onClick: () => void;
}

const MatchListItem: React.FC<MatchListItemProps> = ({
  containerId,
  matchId,
  selected,
  onClick,
}) => {
  const { data: matchSummary } = useGetContainerMatchHistorySummaryQuery({
    containerId,
    matchId,
  });

  return (
    <ListItem disablePadding>
      <ListItemButton selected={selected} onClick={onClick}>
        <ListItemText
          primary={`Match ${matchId}`}
          secondary={
            matchSummary
              ? `${matchSummary.snapshotCount} snapshots (tick ${matchSummary.firstTick}-${matchSummary.lastTick})`
              : "Loading..."
          }
        />
      </ListItemButton>
    </ListItem>
  );
};

export default HistoryPanel;
