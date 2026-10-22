import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { DestinationsPage } from './DestinationsPage';

const lisbon = { id: 1, city: 'Lisbon', country: 'Portugal', description: 'Portuguese capital' };

function page(content: unknown[]) {
  return { content, page: 0, size: 10, totalElements: content.length, totalPages: content.length ? 1 : 0, last: true };
}

function respond(status: number, body: unknown) {
  return Promise.resolve(
    new Response(body === null ? null : JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    }),
  );
}

type Handler = (method: string, url: string) => Promise<Response>;
let fetchMock: ReturnType<typeof vi.fn>;

function stubApi(handler: Handler) {
  fetchMock = vi.fn((url: string, init?: RequestInit) => handler(init?.method ?? 'GET', url));
  vi.stubGlobal('fetch', fetchMock);
}

function urlsCalled(): string[] {
  return fetchMock.mock.calls.map(([url]) => String(url));
}

beforeEach(() => stubApi(() => respond(200, page([lisbon]))));
afterEach(() => vi.unstubAllGlobals());

describe('DestinationsPage', () => {
  it('lists the destinations returned by the API', async () => {
    render(<DestinationsPage />);

    expect(await screen.findByText('Lisbon')).toBeInTheDocument();
    expect(screen.getByText('Portugal')).toBeInTheDocument();
    expect(screen.getByText('1 destination')).toBeInTheDocument();
  });

  it('shows placeholder rows while the first page loads', () => {
    stubApi(() => new Promise<Response>(() => {})); // never answers
    const { container } = render(<DestinationsPage />);

    expect(container.querySelectorAll('.skeleton-row')).toHaveLength(5);
    expect(screen.getByRole('status')).toHaveTextContent('Loading destinations');
  });

  it('invites the user to add the first destination when there are none', async () => {
    stubApi(() => respond(200, page([])));
    render(<DestinationsPage />);

    expect(await screen.findByText(/No destinations yet/)).toBeInTheDocument();
  });

  it('sends the country filter to the API', async () => {
    render(<DestinationsPage />);
    await screen.findByText('Lisbon');

    await userEvent.type(screen.getByLabelText('Search by country'), 'port');

    await waitFor(() => expect(urlsCalled().some((url) => url.includes('country=port'))).toBe(true));
  });

  it('asks the API for the sorted column', async () => {
    render(<DestinationsPage />);
    await screen.findByText('Lisbon');

    await userEvent.click(screen.getByRole('button', { name: 'City' }));

    await waitFor(() => expect(urlsCalled().some((url) => url.includes('sort=city%2Casc'))).toBe(true));
  });

  it('creates a destination from the form', async () => {
    stubApi((method) => (method === 'POST' ? respond(201, { id: 2, city: 'Tokyo', country: 'Japan', description: '' }) : respond(200, page([lisbon]))));
    render(<DestinationsPage />);
    await screen.findByText('Lisbon');

    await userEvent.click(screen.getByRole('button', { name: 'Add destination' }));
    const dialog = screen.getByRole('dialog');
    await userEvent.type(within(dialog).getByLabelText('City'), 'Tokyo');
    await userEvent.type(within(dialog).getByLabelText('Country'), 'Japan');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add destination' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    const post = fetchMock.mock.calls.find(([, init]) => (init as RequestInit | undefined)?.method === 'POST');
    expect(JSON.parse((post![1] as RequestInit).body as string)).toEqual({ city: 'Tokyo', country: 'Japan', description: '' });
  });

  it('shows the message of the API when a delete is refused', async () => {
    stubApi((method) =>
      method === 'DELETE'
        ? respond(409, { status: 409, message: 'Cannot delete destination: it has trips associated with it' })
        : respond(200, page([lisbon])),
    );
    render(<DestinationsPage />);
    await screen.findByText('Lisbon');

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Delete destination' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Cannot delete destination: it has trips associated with it');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('shows a readable error when the API cannot be reached', async () => {
    stubApi(() => Promise.reject(new TypeError('Failed to fetch')));
    render(<DestinationsPage />);

    expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the TripBook API');
  });
});
