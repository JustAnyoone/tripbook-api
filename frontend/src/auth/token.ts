// The access token lives in localStorage so a page refresh keeps the user signed in.
// Trade-off: any script running on the page can read it (XSS), which is why the front-end renders
// everything through React and never injects raw HTML. Every access is wrapped in try/catch
// because storage can be blocked (private windows, strict browser settings).
const KEY = 'tripbook.token';

export function getToken(): string | null {
  try {
    return localStorage.getItem(KEY);
  } catch {
    return null;
  }
}

export function saveToken(token: string): void {
  try {
    localStorage.setItem(KEY, token);
  } catch {
    // Without storage the session lasts until the page is closed
  }
}

export function clearToken(): void {
  try {
    localStorage.removeItem(KEY);
  } catch {
    // Nothing to clear
  }
}
