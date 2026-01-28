/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */

import { useCallback, useEffect, useRef } from "react";
import type { GameError } from "../services/api";
import { useAppDispatch, useAppSelector } from "../store/hooks";
import { addNotification } from "../store/slices/notificationSlice";
import { selectSelectedContainerId } from "../store/slices/uiSlice";
import { useGetContainerMatchesQuery } from "../store/api/apiSlice";

/**
 * Hook that auto-subscribes to error streams for the selected container.
 * Displays errors as notifications when they occur.
 */
export function useContainerErrors() {
  const dispatch = useAppDispatch();
  const selectedContainerId = useAppSelector(selectSelectedContainerId);
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Get matches for the selected container
  const { data: matches = [] } = useGetContainerMatchesQuery(
    selectedContainerId!,
    { skip: selectedContainerId === null }
  );

  const handleError = useCallback(
    (error: GameError) => {
      dispatch(
        addNotification({
          message: error.message,
          severity: error.type === "COMMAND" ? "warning" : "error",
          source: `[${error.type}] ${error.source}`,
          details: error.details,
        })
      );
    },
    [dispatch]
  );

  const connect = useCallback(
    (containerId: number, matchId: number) => {
      // Clean up existing connection
      if (wsRef.current) {
        wsRef.current.close();
        wsRef.current = null;
      }

      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
        reconnectTimeoutRef.current = null;
      }

      const token = localStorage.getItem("authToken");
      if (!token) {
        console.warn("No auth token, cannot connect to error stream");
        return;
      }

      const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
      const wsUrl = `${protocol}//${window.location.host}/ws/containers/${containerId}/matches/${matchId}/errors?token=${token}`;

      try {
        const ws = new WebSocket(wsUrl);
        wsRef.current = ws;

        ws.onopen = () => {
          console.log(
            `Connected to error stream for container ${containerId}, match ${matchId}`
          );
        };

        ws.onmessage = (event) => {
          try {
            const error: GameError = JSON.parse(event.data);
            handleError(error);
          } catch (e) {
            console.error("Failed to parse error message:", e);
          }
        };

        ws.onerror = (event) => {
          console.error("Error stream WebSocket error:", event);
        };

        ws.onclose = (event) => {
          console.log(
            `Error stream disconnected (code: ${event.code}), will reconnect...`
          );
          wsRef.current = null;

          // Attempt to reconnect after 5 seconds if not intentionally closed
          if (event.code !== 1000) {
            reconnectTimeoutRef.current = setTimeout(() => {
              if (selectedContainerId === containerId) {
                connect(containerId, matchId);
              }
            }, 5000);
          }
        };
      } catch (e) {
        console.error("Failed to create error stream WebSocket:", e);
      }
    },
    [handleError, selectedContainerId]
  );

  // Connect/disconnect when container or first match changes
  useEffect(() => {
    if (selectedContainerId !== null && matches.length > 0) {
      // Connect to first match's error stream
      const firstMatch = matches[0];
      connect(selectedContainerId, firstMatch.id);
    }

    return () => {
      if (wsRef.current) {
        wsRef.current.close(1000, "Container changed");
        wsRef.current = null;
      }
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
        reconnectTimeoutRef.current = null;
      }
    };
  }, [selectedContainerId, matches, connect]);

  return {
    isConnected: wsRef.current?.readyState === WebSocket.OPEN,
  };
}
