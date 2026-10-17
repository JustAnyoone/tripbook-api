import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { authApi, setUnauthorizedHandler } from '../api/client';
import type { AuthResult, Credentials, Registration, User } from '../api/types';
import { clearToken, getToken, saveToken } from './token';

// 'checking' lasts only while a stored token is being validated on startup
type Status = 'checking' | 'signed-in' | 'signed-out';

interface AuthValue {
  status: Status;
  user: User | null;
  login: (credentials: Credentials) => Promise<void>;
  register: (registration: Registration) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<Status>(() => (getToken() ? 'checking' : 'signed-out'));
  const [user, setUser] = useState<User | null>(null);

  const logout = useCallback(() => {
    clearToken();
    setUser(null);
    setStatus('signed-out');
  }, []);

  const startSession = useCallback((result: AuthResult) => {
    saveToken(result.token);
    setUser(result.user);
    setStatus('signed-in');
  }, []);

  // An expired or rejected token ends the session wherever the user is
  useEffect(() => {
    setUnauthorizedHandler(logout);
    return () => setUnauthorizedHandler(null);
  }, [logout]);

  // A token found in storage is checked with the API before the app trusts it
  useEffect(() => {
    if (!getToken()) return;
    let cancelled = false;
    authApi
      .me()
      .then((me) => {
        if (cancelled) return;
        setUser(me);
        setStatus('signed-in');
      })
      .catch(() => {
        if (!cancelled) logout();
      });
    return () => {
      cancelled = true;
    };
  }, [logout]);

  const value = useMemo<AuthValue>(
    () => ({
      status,
      user,
      login: async (credentials) => startSession(await authApi.login(credentials)),
      register: async (registration) => startSession(await authApi.register(registration)),
      logout,
    }),
    [status, user, startSession, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthValue {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth must be used inside an AuthProvider');
  return value;
}
