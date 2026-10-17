import { useCallback, useEffect, useState } from 'react';
import { messageOf } from '../api/client';
import type { Page } from '../api/types';

interface ListState<T> {
  data: Page<T> | null;
  loading: boolean;
  error: string | null;
}

// Loads a page whenever one of deps changes. The previous page stays on screen while the next one loads.
export function useList<T>(load: () => Promise<Page<T>>, deps: readonly unknown[]) {
  const [state, setState] = useState<ListState<T>>({ data: null, loading: true, error: null });
  const [version, setVersion] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setState((current) => ({ ...current, loading: true, error: null }));
    load()
      .then((data) => {
        if (!cancelled) setState({ data, loading: false, error: null });
      })
      .catch((error: unknown) => {
        if (!cancelled) setState((current) => ({ ...current, loading: false, error: messageOf(error) }));
      });
    return () => {
      cancelled = true; // a newer request replaced this one; ignore its answer
    };
  }, [...deps, version]);

  const reload = useCallback(() => setVersion((current) => current + 1), []);
  return { ...state, reload };
}
