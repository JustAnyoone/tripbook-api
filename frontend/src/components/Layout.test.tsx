import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';

vi.mock('../auth/AuthContext', () => ({ useAuth: () => ({ user: { name: 'Rhyan', email: 'r@x.com' }, logout: vi.fn() }) }));

import { setLang } from '../i18n';
import { ThemeProvider } from '../theme/theme';
import { Layout } from './Layout';

afterEach(() => vi.unstubAllGlobals());

describe('Layout', () => {
  it('shows an icon next to every navigation link', () => {
    const { container } = render(
      <ThemeProvider>
        <MemoryRouter>
          <Layout />
        </MemoryRouter>
      </ThemeProvider>,
    );
    const links = container.querySelectorAll('.nav a');
    expect(links).toHaveLength(4);
    links.forEach((link) => expect(link.querySelector('svg.icon')).not.toBeNull());
  });

  it('asks before loading sample data and reports the result', async () => {
    const user = userEvent.setup();
    const empty = { content: [], page: 0, size: 500, totalElements: 0, totalPages: 0, last: true };
    let id = 1;
    vi.stubGlobal(
      'fetch',
      vi.fn(async (_url: string, init?: RequestInit) => {
        const body = init?.method && init.method !== 'GET' ? { id: id++, ...JSON.parse(String(init.body)), passenger: { id: 1 }, destination: { id: 1 } } : empty;
        return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } });
      }),
    );
    render(
      <ThemeProvider>
        <MemoryRouter>
          <Layout />
        </MemoryRouter>
      </ThemeProvider>,
    );

    await user.click(screen.getByRole('button', { name: /Sample data/ }));
    await user.click(screen.getByRole('button', { name: 'Add sample data' }));

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent(/Added 6 destinations, 4 passengers/));
  });
});

describe('Layout language switch', () => {
  it('shows the interface in Portuguese after the switch and back in English', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <ThemeProvider>
        <MemoryRouter>
          <Layout />
        </MemoryRouter>
      </ThemeProvider>,
    );
    expect(screen.getByRole('link', { name: 'Trips' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Mudar para português' }));

    expect(screen.getByRole('link', { name: 'Viagens' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Passageiros' })).toBeInTheDocument();
    expect(container.querySelector('.lang-toggle')).toHaveTextContent('PT');

    await user.click(screen.getByRole('button', { name: 'Switch to English' }));

    expect(screen.getByRole('link', { name: 'Trips' })).toBeInTheDocument();
    setLang('en');
  });
});
