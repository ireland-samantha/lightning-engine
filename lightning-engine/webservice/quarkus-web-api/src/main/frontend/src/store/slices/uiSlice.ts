/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */

import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import type { RootState } from "../store";

export type PanelType =
  | "dashboard"
  | "snapshot"
  | "history"
  | "logs"
  | "metrics"
  | "matches"
  | "players"
  | "sessions"
  | "commands"
  | "modules"
  | "ai"
  | "resources"
  | "users"
  | "roles"
  | "settings";

interface UiState {
  selectedContainerId: number | null;
  selectedMatchId: number | null;
  activePanel: PanelType;
  sidebarOpen: boolean;
  containerMenuOpen: boolean;
  adminMenuOpen: boolean;
  iamMenuOpen: boolean;
}

const initialState: UiState = {
  selectedContainerId: null,
  selectedMatchId: null,
  activePanel: "dashboard",
  sidebarOpen: true,
  containerMenuOpen: true,
  adminMenuOpen: false,
  iamMenuOpen: false,
};

const uiSlice = createSlice({
  name: "ui",
  initialState,
  reducers: {
    setSelectedContainerId: (state, action: PayloadAction<number | null>) => {
      state.selectedContainerId = action.payload;
      // Reset match selection when container changes
      state.selectedMatchId = null;
    },
    setSelectedMatchId: (state, action: PayloadAction<number | null>) => {
      state.selectedMatchId = action.payload;
    },
    setActivePanel: (state, action: PayloadAction<PanelType>) => {
      state.activePanel = action.payload;
    },
    setSidebarOpen: (state, action: PayloadAction<boolean>) => {
      state.sidebarOpen = action.payload;
    },
    toggleSidebar: (state) => {
      state.sidebarOpen = !state.sidebarOpen;
    },
    setContainerMenuOpen: (state, action: PayloadAction<boolean>) => {
      state.containerMenuOpen = action.payload;
    },
    toggleContainerMenu: (state) => {
      state.containerMenuOpen = !state.containerMenuOpen;
    },
    setAdminMenuOpen: (state, action: PayloadAction<boolean>) => {
      state.adminMenuOpen = action.payload;
    },
    toggleAdminMenu: (state) => {
      state.adminMenuOpen = !state.adminMenuOpen;
    },
    setIamMenuOpen: (state, action: PayloadAction<boolean>) => {
      state.iamMenuOpen = action.payload;
    },
    toggleIamMenu: (state) => {
      state.iamMenuOpen = !state.iamMenuOpen;
    },
  },
});

export const {
  setSelectedContainerId,
  setSelectedMatchId,
  setActivePanel,
  setSidebarOpen,
  toggleSidebar,
  setContainerMenuOpen,
  toggleContainerMenu,
  setAdminMenuOpen,
  toggleAdminMenu,
  setIamMenuOpen,
  toggleIamMenu,
} = uiSlice.actions;

// Selectors
export const selectSelectedContainerId = (state: RootState) =>
  state.ui.selectedContainerId;
export const selectSelectedMatchId = (state: RootState) =>
  state.ui.selectedMatchId;
export const selectActivePanel = (state: RootState) => state.ui.activePanel;
export const selectSidebarOpen = (state: RootState) => state.ui.sidebarOpen;
export const selectContainerMenuOpen = (state: RootState) =>
  state.ui.containerMenuOpen;
export const selectAdminMenuOpen = (state: RootState) => state.ui.adminMenuOpen;
export const selectIamMenuOpen = (state: RootState) => state.ui.iamMenuOpen;

export default uiSlice.reducer;
