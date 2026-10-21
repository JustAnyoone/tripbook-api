import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';

interface RefreshValue {
  /** Changes every time something outside a list (such as the sample data button) changed the data */
  version: number;
  refresh: () => void;
}

const RefreshContext = createContext<RefreshValue>({ version: 0, refresh: () => {} });

export function RefreshProvider({ children }: { children: ReactNode }) {
  const [version, setVersion] = useState(0);
  const refresh = useCallback(() => setVersion((current) => current + 1), []);
  const value = useMemo(() => ({ version, refresh }), [version, refresh]);
  return <RefreshContext.Provider value={value}>{children}</RefreshContext.Provider>;
}

export const useRefresh = () => useContext(RefreshContext);
