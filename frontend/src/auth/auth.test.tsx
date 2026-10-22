import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from '../App';
import { ThemeProvider } from '../theme/theme';
import { AuthProvider } from './AuthContext';
import { getToken, saveToken } from './token';

const ana = { id: 1, name: 'Ana Souza', email: 'ana@email.com' };
const authResult = { token: 'signed.jwt.token', tokenType: 'Bearer', expiresIn: 3600, user: ana };
const emptyPage = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true };

function respond(status: number, body: unknown) {
  return Promise.resolve(
    new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } }),
  );
}

type Handler = (method: string, url: string, init?: RequestInit) => Promise<Response>;
let fetchMock: ReturnType<typeof vi.fn>;

// Any list endpoint answers with an empty page unless the test says otherwise
function stubApi(handler: Handler = () => respond(200, emptyPage)) {
  fetchMock = vi.fn((url: string, init?: RequestInit) => handler(init?.method ?? 'GET', String(url), init));
  vi.stubGlobal('fetch', fetchMock);
}

function renderApp(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <ThemeProvider>
        <AuthProvider>
          <App />
        </AuthProvider>
      </ThemeProvider>
    </MemoryRouter>,
  );
}

function bodyOf(call: unknown[]): unknown {
  return JSON.parse(String((call[1] as RequestInit).body));
}

beforeEach(() => localStorage.clear());
afterEach(() => vi.unstubAllGlobals());

describe('authentication flow', () => {
  it('sends a signed-out visitor to the login page', async () => {
    stubApi();

    renderApp('/trips');

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('signs in, stores the token and opens the page the visitor asked for', async () => {
    stubApi((_method, url) => (url.endsWith('/api/auth/login') ? respond(200, authResult) : respond(200, emptyPage)));
    const user = userEvent.setup();
    renderApp('/passengers');

    await user.type(await screen.findByLabelText('Email'), 'ana@email.com');
    await user.type(screen.getByLabelText('Password'), 'a-long-passphrase');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('heading', { name: 'Passengers' })).toBeInTheDocument();
    expect(screen.getByText('Ana Souza')).toBeInTheDocument();
    expect(getToken()).toBe('signed.jwt.token');
    const loginCall = fetchMock.mock.calls.find(([url]) => String(url).endsWith('/api/auth/login'))!;
    expect(bodyOf(loginCall)).toEqual({ email: 'ana@email.com', password: 'a-long-passphrase' });
  });

  it('shows the API message for wrong credentials and stays on the login page', async () => {
    stubApi(() => respond(401, { status: 401, message: 'Invalid email or password' }));
    const user = userEvent.setup();
    renderApp('/login');

    await user.type(await screen.findByLabelText('Email'), 'ana@email.com');
    await user.type(screen.getByLabelText('Password'), 'wrong-password');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password');
    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(getToken()).toBeNull();
  });

  it('creates an account and signs the user in right away', async () => {
    stubApi((_method, url) => {
      if (url.endsWith('/api/auth/register')) return respond(201, authResult);
      if (url.endsWith('/api/dashboard')) return respond(200, { statusCounts: [], totals: [], upcoming: [], topDestinations: [] });
      return respond(200, emptyPage);
    });
    const user = userEvent.setup();
    renderApp('/register');

    await user.type(await screen.findByLabelText('Name'), 'Ana Souza');
    await user.type(screen.getByLabelText('Email'), 'ana@email.com');
    await user.type(screen.getByLabelText('Password'), 'a-long-passphrase');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
    const call = fetchMock.mock.calls.find(([url]) => String(url).endsWith('/api/auth/register'))!;
    expect(bodyOf(call)).toEqual({ name: 'Ana Souza', email: 'ana@email.com', password: 'a-long-passphrase' });
    expect(getToken()).toBe('signed.jwt.token');
  });

  it('keeps the user signed in after a refresh when the stored token is still valid', async () => {
    saveToken('signed.jwt.token');
    stubApi((_method, url) => (url.endsWith('/api/auth/me') ? respond(200, ana) : respond(200, emptyPage)));

    renderApp('/trips');

    expect(await screen.findByRole('heading', { name: 'Trips' })).toBeInTheDocument();
    const meCall = fetchMock.mock.calls.find(([url]) => String(url).endsWith('/api/auth/me'))!;
    expect((meCall[1] as RequestInit).headers).toMatchObject({ Authorization: 'Bearer signed.jwt.token' });
  });

  it('drops a stored token the API rejects and shows the login page', async () => {
    saveToken('expired.jwt.token');
    stubApi(() => respond(401, { status: 401, message: 'Authentication required' }));

    renderApp('/trips');

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(screen.getByText('Your session expired. Please sign in again.')).toBeInTheDocument();
    expect(getToken()).toBeNull();
  });

  it('signs out when the token expires in the middle of a session', async () => {
    saveToken('signed.jwt.token');
    let expired = false;
    stubApi((_method, url) => {
      if (url.endsWith('/api/auth/me')) return respond(200, ana);
      return expired ? respond(401, { status: 401, message: 'Authentication required' }) : respond(200, emptyPage);
    });
    const user = userEvent.setup();
    renderApp('/destinations');
    await screen.findByRole('heading', { name: 'Destinations' });

    expired = true;
    await user.click(screen.getByRole('link', { name: 'Passengers' }));

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(getToken()).toBeNull();
  });

  it('signs out on request', async () => {
    saveToken('signed.jwt.token');
    stubApi((_method, url) => (url.endsWith('/api/auth/me') ? respond(200, ana) : respond(200, emptyPage)));
    const user = userEvent.setup();
    renderApp('/trips');

    await user.click(await screen.findByRole('button', { name: 'Sign out' }));

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    // Signing out on purpose is not an expired session
    expect(screen.queryByText(/session expired/)).not.toBeInTheDocument();
    await waitFor(() => expect(getToken()).toBeNull());
  });
});
