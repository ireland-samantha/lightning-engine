/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */

import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import type { RootState } from "../store";

export interface Notification {
  id: string;
  message: string;
  severity: "error" | "warning" | "info" | "success";
  timestamp: number;
  source?: string;
  details?: string;
}

interface NotificationState {
  notifications: Notification[];
  maxNotifications: number;
}

const initialState: NotificationState = {
  notifications: [],
  maxNotifications: 50,
};

const notificationSlice = createSlice({
  name: "notifications",
  initialState,
  reducers: {
    addNotification: (
      state,
      action: PayloadAction<Omit<Notification, "id" | "timestamp">>
    ) => {
      const notification: Notification = {
        ...action.payload,
        id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
        timestamp: Date.now(),
      };
      state.notifications.unshift(notification);
      // Keep only the last maxNotifications
      if (state.notifications.length > state.maxNotifications) {
        state.notifications = state.notifications.slice(
          0,
          state.maxNotifications
        );
      }
    },
    removeNotification: (state, action: PayloadAction<string>) => {
      state.notifications = state.notifications.filter(
        (n) => n.id !== action.payload
      );
    },
    clearNotifications: (state) => {
      state.notifications = [];
    },
  },
});

export const { addNotification, removeNotification, clearNotifications } =
  notificationSlice.actions;

// Selectors
export const selectNotifications = (state: RootState) =>
  state.notifications.notifications;
export const selectLatestNotification = (state: RootState) =>
  state.notifications.notifications[0] || null;

export default notificationSlice.reducer;
