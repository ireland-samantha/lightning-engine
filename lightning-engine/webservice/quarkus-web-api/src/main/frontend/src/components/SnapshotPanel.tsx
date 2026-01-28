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
    Circle as CircleIcon, ExpandMore as ExpandMoreIcon, Refresh as RefreshIcon
} from "@mui/icons-material";
import {
    Accordion, AccordionDetails, AccordionSummary, Alert, Box, Card,
    CardContent,
    CardHeader, Chip,
    CircularProgress, IconButton, Paper, Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow, Tooltip, Typography
} from "@mui/material";
import { useEffect, useState } from "react";
import { useSnapshot } from "../hooks/useSnapshot";

interface SnapshotPanelProps {
  containerId: number;
  matchId: number;
}

export const SnapshotPanel: React.FC<SnapshotPanelProps> = ({
  containerId,
  matchId,
}) => {
  const { snapshot, connected, error, requestSnapshot } = useSnapshot(
    containerId,
    matchId,
  );
  const [expandedModules, setExpandedModules] = useState<Set<string>>(
    new Set(),
  );

  useEffect(() => {
    // Expand first module by default
    if (snapshot && expandedModules.size === 0) {
      if (snapshot.modules.length > 0) {
        setExpandedModules(new Set([snapshot.modules[0].name]));
      }
    }
  }, [snapshot]);

  const handleModuleToggle = (moduleName: string) => {
    setExpandedModules((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(moduleName)) {
        newSet.delete(moduleName);
      } else {
        newSet.add(moduleName);
      }
      return newSet;
    });
  };

  const getEntityCount = (): number => {
    if (!snapshot?.modules || snapshot.modules.length === 0) return 0;
    const firstModule = snapshot.modules[0];
    if (firstModule.components.length === 0) return 0;
    return firstModule.components[0].values.length;
  };

  return (
    <Card sx={{ height: "100%", display: "flex", flexDirection: "column" }}>
      <CardHeader
        title="Live Snapshot"
        subheader={`Match ${matchId}`}
        action={
          <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
            <Tooltip title={connected ? "Connected" : "Disconnected"}>
              <CircleIcon
                sx={{
                  fontSize: 12,
                  color: connected ? "success.main" : "error.main",
                }}
              />
            </Tooltip>
            <Tooltip title="Refresh">
              <IconButton onClick={requestSnapshot} size="small">
                <RefreshIcon />
              </IconButton>
            </Tooltip>
          </Box>
        }
      />
      <CardContent sx={{ flexGrow: 1, overflow: "auto" }}>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {!snapshot && !error && (
          <Box sx={{ display: "flex", justifyContent: "center", p: 4 }}>
            <CircularProgress />
          </Box>
        )}

        {snapshot && (
          <>
            <Box sx={{ display: "flex", gap: 1, mb: 2, flexWrap: "wrap" }}>
              <Chip
                label={`Tick ${snapshot.tick}`}
                color="primary"
                variant="outlined"
              />
              <Chip
                label={`${getEntityCount()} Entities`}
                color="secondary"
                variant="outlined"
              />
              <Chip
                label={`${snapshot.modules.length} Modules`}
                variant="outlined"
              />
            </Box>

            {snapshot.modules.map((moduleData) => (
              <Accordion
                key={moduleData.name}
                expanded={expandedModules.has(moduleData.name)}
                onChange={() => handleModuleToggle(moduleData.name)}
                sx={{ mb: 1 }}
              >
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography variant="subtitle1" fontWeight="medium">
                    {moduleData.name}
                  </Typography>
                  <Chip
                    size="small"
                    label={`v${moduleData.version}`}
                    sx={{ ml: 1 }}
                    color="info"
                  />
                  <Chip
                    size="small"
                    label={`${moduleData.components.length} components`}
                    sx={{ ml: 1 }}
                  />
                </AccordionSummary>
                <AccordionDetails>
                  <TableContainer component={Paper} variant="outlined">
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Component</TableCell>
                          <TableCell align="right">Count</TableCell>
                          <TableCell>Values</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {moduleData.components.map((component) => (
                          <TableRow key={component.name}>
                            <TableCell component="th" scope="row">
                              <Typography
                                variant="body2"
                                fontFamily="monospace"
                              >
                                {component.name}
                              </Typography>
                            </TableCell>
                            <TableCell align="right">
                              {component.values.length}
                            </TableCell>
                            <TableCell>
                              <Typography
                                variant="body2"
                                fontFamily="monospace"
                                sx={{
                                  maxWidth: 200,
                                  overflow: "hidden",
                                  textOverflow: "ellipsis",
                                  whiteSpace: "nowrap",
                                }}
                              >
                                {component.values.slice(0, 3).join(", ") +
                                  (component.values.length > 3 ? "..." : "")}
                              </Typography>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </AccordionDetails>
              </Accordion>
            ))}
          </>
        )}
      </CardContent>
    </Card>
  );
};

export default SnapshotPanel;
