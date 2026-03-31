import { useCallback } from 'react';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { loginThunk, clearCredentials } from '../store/slices/authSlice';
import type { UserRole } from '../types';

export function useAuth() {
  const dispatch = useAppDispatch();
  const { user, token, isAuthenticated, isLoading } = useAppSelector((s) => s.auth);

  const login = useCallback(
    async (username: string, password: string) => {
      await dispatch(loginThunk({ username, password })).unwrap();
    },
    [dispatch]
  );

  const logout = useCallback(() => {
    dispatch(clearCredentials());
  }, [dispatch]);

  const hasRole = useCallback(
    (...roles: UserRole[]) => user != null && roles.includes(user.role),
    [user]
  );

  const isQA = useCallback(
    () => user?.unitCode === 'QA',
    [user]
  );

  return { user, token, isAuthenticated, isLoading, login, logout, hasRole, isQA };
}
