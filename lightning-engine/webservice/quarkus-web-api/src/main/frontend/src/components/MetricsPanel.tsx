/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */

import {
  Cached as CachedIcon,
  CheckCircle as SuccessIcon,
  Error as ErrorIcon,
  Extension as ComponentIcon,
  Functions as SystemIcon,
  Memory as MemoryIcon,
  PlayArrow as CommandIcon,
  Queue as QueueIcon,
  Refresh as RefreshIcon,
  RestartAlt as ResetIcon,
  Speed as SpeedIcon,
  Storage as EntityIcon,
  Timer as TimerIcon,
  TrendingDown as MinIcon,
  TrendingUp as MaxIcon,
} from "@mui/icons-material";
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Grid,
  IconButton,
  LinearProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from "@mui/material";
import { useEffect, useState } from "react";
import {
  useGetContainerMetricsQuery,
  useResetContainerMetricsMutation,
} from "../store/api/apiSlice";
import { useAppSelector } from "../store/hooks";
import { selectSelectedContainerId } from "../store/slices/uiSlice";

// Threshold for warning color on command queue size
const COMMAND_QUEUE_WARNING_THRESHOLD = 100;

interface MetricCardProps {
  title: string;
  value: string | number;
  unit?: string;
  icon: React.ReactNode;
  color?: "primary" | "secondary" | "success" | "warning" | "error" | "info";
  subtitle?: string;
}

const MetricCard: React.FC<MetricCardProps> = ({
  title,
  value,
  unit,
  icon,
  color = "primary",
  subtitle,
}) => (
  <Card sx={{ height: "100%" }}>
    <CardContent>
      <Box sx={{ display: "flex", alignItems: "center", mb: 1 }}>
        <Box sx={{ color: `${color}.main`, mr: 1 }}>{icon}</Box>
        <Typography variant="subtitle2" color="text.secondary">
          {title}
        </Typography>
      </Box>
      <Typography variant="h4" component="div" sx={{ fontFamily: "monospace" }}>
        {typeof value === "number" ? value.toFixed(3) : value}
        {unit && (
          <Typography
            component="span"
            variant="body2"
            color="text.secondary"
            sx={{ ml: 0.5 }}
          >
            {unit}
          </Typography>
        )}
      </Typography>
      {subtitle && (
        <Typography variant="caption" color="text.secondary">
          {subtitle}
        </Typography>
      )}
    </CardContent>
  </Card>
);

const MetricsPanel: React.FC = () => {
  const selectedContainerId = useAppSelector(selectSelectedContainerId);
  const [autoRefresh, setAutoRefresh] = useState(true);

  const {
    data: metrics,
    isLoading,
    error,
    refetch,
  } = useGetContainerMetricsQuery(selectedContainerId!, {
    skip: selectedContainerId === null,
    pollingInterval: autoRefresh ? 1000 : 0,
  });

  const [resetMetrics, { isLoading: isResetting }] =
    useResetContainerMetricsMutation();

  // Refetch when container changes
  useEffect(() => {
    if (selectedContainerId !== null) {
      refetch();
    }
  }, [selectedContainerId, refetch]);

  const handleReset = async () => {
    if (selectedContainerId !== null) {
      await resetMetrics(selectedContainerId);
      refetch();
    }
  };

  if (selectedContainerId === null) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="info">
          Please select a container to view metrics.
        </Alert>
      </Box>
    );
  }

  if (isLoading && !metrics) {
    return (
      <Box
        sx={{ p: 3, display: "flex", justifyContent: "center", pt: 10 }}
      >
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">
          Failed to load metrics. Make sure the container is running.
        </Alert>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      <Box
        sx={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          mb: 3,
        }}
      >
        <Box>
          <Typography variant="h5" gutterBottom>
            Container Metrics
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Real-time tick timing and performance metrics
          </Typography>
        </Box>
        <Box sx={{ display: "flex", gap: 1, alignItems: "center" }}>
          <Chip
            label={autoRefresh ? "Auto-refresh ON" : "Auto-refresh OFF"}
            color={autoRefresh ? "success" : "default"}
            size="small"
            onClick={() => setAutoRefresh(!autoRefresh)}
          />
          <Tooltip title="Refresh">
            <IconButton onClick={() => refetch()} size="small">
              <RefreshIcon />
            </IconButton>
          </Tooltip>
          <Button
            variant="outlined"
            size="small"
            startIcon={<ResetIcon />}
            onClick={handleReset}
            disabled={isResetting}
          >
            Reset
          </Button>
        </Box>
      </Box>

      {metrics && (
        <>
          {/* Summary Stats */}
          <Paper sx={{ p: 2, mb: 3 }}>
            <Box
              sx={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
              }}
            >
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Current Tick
                </Typography>
                <Typography
                  variant="h3"
                  sx={{ fontFamily: "monospace", fontWeight: "bold" }}
                >
                  {metrics.currentTick.toLocaleString()}
                </Typography>
              </Box>
              <Box sx={{ textAlign: "right" }}>
                <Typography variant="subtitle2" color="text.secondary">
                  Total Ticks Processed
                </Typography>
                <Typography
                  variant="h3"
                  sx={{ fontFamily: "monospace", fontWeight: "bold" }}
                >
                  {metrics.totalTicks.toLocaleString()}
                </Typography>
              </Box>
            </Box>
          </Paper>

          {/* Entity/Store Metrics */}
          <Typography variant="h6" sx={{ mb: 2, display: "flex", alignItems: "center", gap: 1 }}>
            <EntityIcon /> Store Statistics
          </Typography>
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid size={{ xs: 12, sm: 6, md: 4 }}>
              <MetricCard
                title="Total Entities"
                value={metrics.totalEntities.toLocaleString()}
                icon={<EntityIcon />}
                color="primary"
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 4 }}>
              <MetricCard
                title="Component Types"
                value={metrics.totalComponentTypes.toLocaleString()}
                icon={<ComponentIcon />}
                color="info"
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 4 }}>
              <MetricCard
                title="Command Queue"
                value={metrics.commandQueueSize.toLocaleString()}
                icon={<QueueIcon />}
                color={metrics.commandQueueSize > COMMAND_QUEUE_WARNING_THRESHOLD ? "warning" : "success"}
                subtitle={metrics.commandQueueSize > 0 ? "pending commands" : "empty"}
              />
            </Grid>
          </Grid>

          {/* Timing Metrics */}
          <Typography variant="h6" sx={{ mb: 2, display: "flex", alignItems: "center", gap: 1 }}>
            <TimerIcon /> Tick Timing
          </Typography>
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <MetricCard
                title="Last Tick"
                value={metrics.lastTickMs}
                unit="ms"
                icon={<TimerIcon />}
                color="primary"
                subtitle={`${metrics.lastTickNanos.toLocaleString()} ns`}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <MetricCard
                title="Average Tick"
                value={metrics.avgTickMs}
                unit="ms"
                icon={<SpeedIcon />}
                color="info"
                subtitle={`${metrics.avgTickNanos.toLocaleString()} ns`}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <MetricCard
                title="Min Tick"
                value={metrics.minTickMs}
                unit="ms"
                icon={<MinIcon />}
                color="success"
                subtitle={`${metrics.minTickNanos.toLocaleString()} ns`}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <MetricCard
                title="Max Tick"
                value={metrics.maxTickMs}
                unit="ms"
                icon={<MaxIcon />}
                color="warning"
                subtitle={`${metrics.maxTickNanos.toLocaleString()} ns`}
              />
            </Grid>
          </Grid>

          {/* Snapshot Cache Metrics */}
          {metrics.snapshotMetrics && (
            <>
              <Typography variant="h6" sx={{ mt: 4, mb: 2, display: "flex", alignItems: "center", gap: 1 }}>
                <CachedIcon /> Snapshot Cache
              </Typography>
              <Grid container spacing={2}>
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                  <MetricCard
                    title="Cache Hit Rate"
                    value={(metrics.snapshotMetrics.cacheHitRate * 100).toFixed(1)}
                    unit="%"
                    icon={<CachedIcon />}
                    color={metrics.snapshotMetrics.cacheHitRate > 0.8 ? "success" : metrics.snapshotMetrics.cacheHitRate > 0.5 ? "warning" : "error"}
                    subtitle={`${metrics.snapshotMetrics.cacheHits} hits / ${metrics.snapshotMetrics.totalGenerations} total`}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                  <MetricCard
                    title="Avg Generation"
                    value={metrics.snapshotMetrics.avgGenerationMs}
                    unit="ms"
                    icon={<SpeedIcon />}
                    color="info"
                    subtitle={`Max: ${metrics.snapshotMetrics.maxGenerationMs.toFixed(2)} ms`}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                  <MetricCard
                    title="Full Rebuilds"
                    value={metrics.snapshotMetrics.fullRebuilds}
                    icon={<MemoryIcon />}
                    color="warning"
                    subtitle={`${metrics.snapshotMetrics.incrementalUpdates} incremental`}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                  <MetricCard
                    title="Cache Misses"
                    value={metrics.snapshotMetrics.cacheMisses}
                    icon={<ErrorIcon />}
                    color={metrics.snapshotMetrics.cacheMisses === 0 ? "success" : "warning"}
                  />
                </Grid>
              </Grid>
            </>
          )}

          {/* System Execution Metrics */}
          {metrics.lastTickSystems && metrics.lastTickSystems.length > 0 && (
            <>
              <Typography variant="h6" sx={{ mt: 4, mb: 2, display: "flex", alignItems: "center", gap: 1 }}>
                <SystemIcon /> Last Tick Systems ({metrics.lastTickSystems.length})
              </Typography>
              <TableContainer component={Paper}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>System</TableCell>
                      <TableCell align="right">Time (ms)</TableCell>
                      <TableCell align="right">Time (ns)</TableCell>
                      <TableCell align="center">Status</TableCell>
                      <TableCell>% of Tick</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {metrics.lastTickSystems.map((system, index) => {
                      const percentOfTick = metrics.lastTickNanos > 0
                        ? (system.executionTimeNanos / metrics.lastTickNanos) * 100
                        : 0;
                      return (
                        <TableRow key={index} sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
                          <TableCell component="th" scope="row">
                            <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
                              {system.systemName}
                            </Typography>
                          </TableCell>
                          <TableCell align="right">
                            <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
                              {system.executionTimeMs.toFixed(3)}
                            </Typography>
                          </TableCell>
                          <TableCell align="right">
                            <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
                              {system.executionTimeNanos.toLocaleString()}
                            </Typography>
                          </TableCell>
                          <TableCell align="center">
                            {system.success ? (
                              <SuccessIcon color="success" fontSize="small" />
                            ) : (
                              <ErrorIcon color="error" fontSize="small" />
                            )}
                          </TableCell>
                          <TableCell sx={{ width: 150 }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                              <LinearProgress
                                variant="determinate"
                                value={Math.min(percentOfTick, 100)}
                                sx={{ flexGrow: 1, height: 8, borderRadius: 1 }}
                                color={percentOfTick > 50 ? "warning" : percentOfTick > 20 ? "info" : "success"}
                              />
                              <Typography variant="caption" sx={{ minWidth: 40 }}>
                                {percentOfTick.toFixed(1)}%
                              </Typography>
                            </Box>
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            </>
          )}

          {/* Command Execution Metrics */}
          {metrics.lastTickCommands && metrics.lastTickCommands.length > 0 && (
            <>
              <Typography variant="h6" sx={{ mt: 4, mb: 2, display: "flex", alignItems: "center", gap: 1 }}>
                <CommandIcon /> Last Tick Commands ({metrics.lastTickCommands.length})
              </Typography>
              <TableContainer component={Paper}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Command</TableCell>
                      <TableCell align="right">Time (ms)</TableCell>
                      <TableCell align="right">Time (ns)</TableCell>
                      <TableCell align="center">Status</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {metrics.lastTickCommands.map((command, index) => (
                      <TableRow key={index} sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
                        <TableCell component="th" scope="row">
                          <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
                            {command.commandName}
                          </Typography>
                        </TableCell>
                        <TableCell align="right">
                          <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
                            {command.executionTimeMs.toFixed(3)}
                          </Typography>
                        </TableCell>
                        <TableCell align="right">
                          <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
                            {command.executionTimeNanos.toLocaleString()}
                          </Typography>
                        </TableCell>
                        <TableCell align="center">
                          {command.success ? (
                            <SuccessIcon color="success" fontSize="small" />
                          ) : (
                            <ErrorIcon color="error" fontSize="small" />
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </>
          )}

          {/* Performance Insights */}
          <Paper sx={{ p: 2, mt: 4 }}>
            <Typography variant="subtitle1" gutterBottom>
              Performance Insights
            </Typography>
            <Box sx={{ display: "flex", gap: 2, flexWrap: "wrap" }}>
              {metrics.avgTickMs < 1 && (
                <Chip
                  label="Excellent: Sub-millisecond average tick time"
                  color="success"
                  size="small"
                />
              )}
              {metrics.avgTickMs >= 1 && metrics.avgTickMs < 16 && (
                <Chip
                  label="Good: Average tick time under 16ms (60 FPS capable)"
                  color="info"
                  size="small"
                />
              )}
              {metrics.avgTickMs >= 16 && (
                <Chip
                  label="Warning: Average tick time over 16ms"
                  color="warning"
                  size="small"
                />
              )}
              {metrics.maxTickMs > metrics.avgTickMs * 10 && (
                <Chip
                  label="Spikes detected: Max tick significantly higher than average"
                  color="warning"
                  size="small"
                />
              )}
              <Chip
                label={`Tick rate: ~${Math.round(1000 / Math.max(metrics.avgTickMs, 0.001))} ticks/sec`}
                variant="outlined"
                size="small"
              />
              {metrics.lastTickSystems && metrics.lastTickSystems.length > 0 && (
                <Chip
                  label={`${metrics.lastTickSystems.length} systems executed`}
                  variant="outlined"
                  size="small"
                  icon={<SystemIcon />}
                />
              )}
              {metrics.lastTickCommands && metrics.lastTickCommands.length > 0 && (
                <Chip
                  label={`${metrics.lastTickCommands.length} commands processed`}
                  variant="outlined"
                  size="small"
                  icon={<CommandIcon />}
                />
              )}
            </Box>
          </Paper>
        </>
      )}
    </Box>
  );
};

export default MetricsPanel;
