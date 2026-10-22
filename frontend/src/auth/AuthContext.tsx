import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { authApi, setUnauthorizedHandler } from '../api/client';
import type { AuthResult, Credentials, Registration, User } from '../api/types';
import { clearToken, getToken, saveToken } from './token';

// 'checking' lasts only while a stored token is being validated on startup
type Status = 'checking' | 'signed-in' | 'signed-out';

interface AuthValue {
  status: Status;
  user: User | null;
  /** True after the server rejected the token, so the login page can say why the user is back there */
  sessionExpired: boolean;
  login: (credentials: Credentials) => Promise<void>;
  register: (registration: Registration) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<Status>(() => (getToken() ? 'checking' : 'signed-out'));
  const [user, setUser] = useState<User | null>(null);
  const [sessionExpired, setSessionExpired] = useState(false);

  const logout = useCallback(() => {
    clearToken();
    setUser(null);
    setStatus('signed-out');
  }, []);

  // The server refused the token (it expired, or the account is gone): end the session and say so
  const expireSession = useCallback(() => {
    setSessionExpired(true);
    logout();
  }, [logout]);

  const startSession = useCallback((result: AuthResult) => {
    setSessionExpired(false);
    saveToken(result.token);
    setUser(result.user);
    setStatus('signed-in');
  }, []);

  // An expired or rejected token ends the session wherever the user is
  useEffect(() => {
    setUnauthorizedHandler(expireSession);
    return () => setUnauthorizedHandler(null);
  }, [expireSession]);

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
        if (!cancelled) expireSession();
      });
    return () => {
      cancelled = true;
    };
  }, [expireSession]);

  const value = useMemo<AuthValue>(
    () => ({
      status,
      user,
      sessionExpired,
      login: async (credentials) => startSession(await authApi.login(credentials)),
      register: async (registration) => startSession(await authApi.register(registration)),
      logout,
    }),
    [status, user, sessionExpired, startSession, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthValue {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth must be used inside an AuthProvider');
  return value;
}
