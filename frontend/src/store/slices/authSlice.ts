import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit';
import api from '../../api/client';
import type { AppUser, AuthResponse, UserRole } from '../../types';
import type { RootState } from '../index';

interface AuthState {
  user: AppUser | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

const initialState: AuthState = {
  user: null,
  token: null,
  isAuthenticated: false,
  isLoading: true,
};

export const loginThunk = createAsyncThunk(
  'auth/login',
  async ({ username, password }: { username: string; password: string }) => {
    const { data } = await api.post<AuthResponse>('/auth/login', { username, password });
    const user: AppUser = {
      id: data.userId,
      username: data.username,
      email: '',
      fullName: data.fullName,
      role: data.role,
      unitId: data.unitId,
      unitCode: data.unitCode,
      unitDisplayName: data.unitDisplayName,
      active: true,
    };
    localStorage.setItem('pharma_cx_token', data.token);
    localStorage.setItem('pharma_cx_user', JSON.stringify(user));
    return { user, token: data.token };
  }
);

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    initAuth(state) {
      const token = localStorage.getItem('pharma_cx_token');
      const userStr = localStorage.getItem('pharma_cx_user');
      if (token && userStr) {
        try {
          state.user = JSON.parse(userStr) as AppUser;
          state.token = token;
          state.isAuthenticated = true;
        } catch {
          localStorage.removeItem('pharma_cx_token');
          localStorage.removeItem('pharma_cx_user');
        }
      }
      state.isLoading = false;
    },
    clearCredentials(state) {
      localStorage.removeItem('pharma_cx_token');
      localStorage.removeItem('pharma_cx_user');
      state.user = null;
      state.token = null;
      state.isAuthenticated = false;
      state.isLoading = false;
    },
    setCredentials(state, action: PayloadAction<{ user: AppUser; token: string }>) {
      state.user = action.payload.user;
      state.token = action.payload.token;
      state.isAuthenticated = true;
      state.isLoading = false;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(loginThunk.fulfilled, (state, action) => {
        state.user = action.payload.user;
        state.token = action.payload.token;
        state.isAuthenticated = true;
        state.isLoading = false;
      })
      .addCase(loginThunk.rejected, (state) => {
        state.isLoading = false;
      });
  },
});

export const { initAuth, clearCredentials, setCredentials } = authSlice.actions;

export const selectUser = (state: RootState) => state.auth.user;
export const selectIsAuthenticated = (state: RootState) => state.auth.isAuthenticated;
export const selectIsLoading = (state: RootState) => state.auth.isLoading;
export const selectHasRole = (state: RootState, ...roles: UserRole[]) =>
  state.auth.user != null && roles.includes(state.auth.user.role);
export const selectIsQA = (state: RootState) =>
  state.auth.user?.unitCode === 'QA';

export default authSlice.reducer;
