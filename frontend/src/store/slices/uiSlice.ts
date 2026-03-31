import { createSlice } from '@reduxjs/toolkit';

interface UiState {
  sidebarOpen: boolean;
  activeRequests: number;
}

const initialState: UiState = {
  sidebarOpen: true,
  activeRequests: 0,
};

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    toggleSidebar(state) {
      state.sidebarOpen = !state.sidebarOpen;
    },
    requestStarted(state) {
      state.activeRequests += 1;
    },
    requestFinished(state) {
      state.activeRequests = Math.max(0, state.activeRequests - 1);
    },
  },
});

export const { toggleSidebar, requestStarted, requestFinished } = uiSlice.actions;
export const selectIsLoading = (state: { ui: UiState }) => state.ui.activeRequests > 0;
export default uiSlice.reducer;
