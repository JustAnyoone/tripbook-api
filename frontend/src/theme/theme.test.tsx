import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ThemeToggle } from '../components/ThemeToggle';
import { initialTheme, ThemeProvider } from './theme';

function stubSystem(dark: boolean) {
  vi.stubGlobal('matchMedia', (query: string) => ({ matches: dark && query.includes('dark'), media: query }));
}

beforeEach(() => {
  localStorage.clear();
  delete document.documentElement.dataset.theme;
});
afterEach(() => vi.unstubAllGlobals());

describe('theme', () => {
  it('follows the operating system when nothing is saved', () => {
    stubSystem(true);
    expect(initialTheme()).toBe('dark');
    stubSystem(false);
    expect(initialTheme()).toBe('light');
  });

  it('prefers the saved choice over the operating system', () => {
    stubSystem(true);
    localStorage.setItem('tripbook.theme', 'light');
    expect(initialTheme()).toBe('light');
  });

  it('switches, applies the theme to the page and remembers it', async () => {
    stubSystem(false);
    const user = userEvent.setup();
    render(
      <ThemeProvider>
        <ThemeToggle />
      </ThemeProvider>,
    );
    expect(document.documentElement.dataset.theme).toBe('light');

    await user.click(screen.getByRole('button', { name: 'Switch to dark mode' }));

    expect(document.documentElement.dataset.theme).toBe('dark');
    expect(localStorage.getItem('tripbook.theme')).toBe('dark');
    expect(screen.getByRole('button', { name: 'Switch to light mode' })).toBeInTheDocument();
  });

  it('still switches when the browser blocks storage', async () => {
    stubSystem(false);
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('blocked');
    });
    const user = userEvent.setup();
    render(
      <ThemeProvider>
        <ThemeToggle />
      </ThemeProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'Switch to dark mode' }));

    expect(document.documentElement.dataset.theme).toBe('dark');
    vi.restoreAllMocks();
  });
});
