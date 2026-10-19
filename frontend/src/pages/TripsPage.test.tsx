import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { TripsPage } from './TripsPage';

const emptyPage = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true };
const ana = { id: 1, name: 'Ana Souza', documentNumber: 'AB123456', email: 'ana@email.com', birthDate: null };
const lisbon = { id: 1, city: 'Lisbon', country: 'Portugal', description: null };

function page(content: unknown[]) {
  return { ...emptyPage, content, totalElements: content.length, totalPages: content.length ? 1 : 0 };
}

let fetchMock: ReturnType<typeof vi.fn>;

function stubApi(trips: unknown[] = []) {
  fetchMock = vi.fn((url: string, init?: RequestInit) => {
    const path = String(url);
    let body: unknown = page(trips);
    if (path.includes('/api/passengers')) body = page([ana]);
    else if (path.includes('/api/destinations')) body = page([lisbon]);
    else if (init?.method === 'POST') body = { id: 9 };
    return Promise.resolve(new Response(JSON.stringify(body), { status: init?.method === 'POST' ? 201 : 200, headers: { 'Content-Type': 'application/json' } }));
  });
  vi.stubGlobal('fetch', fetchMock);
}

// Only the clock is faked, so timers and promises keep working. "Today" is Monday 19 October 2026.
beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] });
  vi.setSystemTime(new Date(2026, 9, 19, 12, 0, 0));
  stubApi();
});
afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

async function openForm(user: ReturnType<typeof userEvent.setup>) {
  await user.click((await screen.findAllByRole('button', { name: 'Add trip' }))[0]);
  await screen.findByLabelText('Passenger');
}

const day = (label: string) => screen.getByRole('button', { name: label });

describe('trip form calendar', () => {
  it('offers no day before today for the departure', async () => {
    const user = userEvent.setup();
    render(<TripsPage />);
    await openForm(user);

    await user.click(screen.getByLabelText('Departure date'));

    expect(day('Sunday, October 18, 2026')).toBeDisabled();
    expect(day('Monday, October 19, 2026')).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Previous month' })).toBeDisabled();
  });

  it('offers no return day before the departure', async () => {
    const user = userEvent.setup();
    render(<TripsPage />);
    await openForm(user);

    await user.click(screen.getByLabelText('Departure date'));
    await user.click(day('Tuesday, October 20, 2026'));
    await user.click(screen.getByLabelText('Return date'));

    expect(day('Monday, October 19, 2026')).toBeDisabled();
    expect(day('Tuesday, October 20, 2026')).toBeEnabled();
  });

  it('clears the return date when a later departure makes it invalid', async () => {
    const user = userEvent.setup();
    render(<TripsPage />);
    await openForm(user);

    await user.click(screen.getByLabelText('Departure date'));
    await user.click(day('Tuesday, October 20, 2026'));
    await user.click(screen.getByLabelText('Return date'));
    await user.click(day('Wednesday, October 21, 2026'));
    expect(screen.getByLabelText('Return date')).toHaveTextContent('21 Oct 2026');

    await user.click(screen.getByLabelText('Departure date'));
    await user.click(day('Friday, October 23, 2026'));

    expect(screen.getByLabelText('Return date')).toHaveTextContent('No return date');
  });

  it('sends the picked dates in the request', async () => {
    const user = userEvent.setup();
    render(<TripsPage />);
    await openForm(user);

    await user.selectOptions(screen.getByLabelText('Passenger'), '1');
    await user.selectOptions(screen.getByLabelText('Destination'), '1');
    await user.click(screen.getByLabelText('Departure date'));
    await user.click(day('Tuesday, October 20, 2026'));
    await user.click(screen.getByLabelText('Return date'));
    await user.click(day('Sunday, October 25, 2026'));
    await user.type(screen.getByLabelText('Price'), '100');
    const form = screen.getByLabelText('Price').closest('form')!;
    await user.click(within(form).getByRole('button', { name: 'Add trip' }));

    const post = fetchMock.mock.calls.find(([, init]) => (init as RequestInit | undefined)?.method === 'POST')!;
    expect(JSON.parse(String((post[1] as RequestInit).body))).toMatchObject({
      departureDate: '2026-10-20',
      returnDate: '2026-10-25',
      passengerId: 1,
      destinationId: 1,
    });
  });

  it('asks for a departure date instead of sending the form without one', async () => {
    const user = userEvent.setup();
    render(<TripsPage />);
    await openForm(user);

    await user.selectOptions(screen.getByLabelText('Passenger'), '1');
    await user.selectOptions(screen.getByLabelText('Destination'), '1');
    await user.type(screen.getByLabelText('Price'), '100');
    const form = screen.getByLabelText('Price').closest('form')!;
    await user.click(within(form).getByRole('button', { name: 'Add trip' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Choose a departure date.');
    expect(fetchMock.mock.calls.some(([, init]) => (init as RequestInit | undefined)?.method === 'POST')).toBe(false);
  });
});

describe('trip boarding passes', () => {
  it('shows the destination, passenger, dates and price on each pass', async () => {
    stubApi([
      {
        id: 5,
        departureDate: '2026-12-20',
        returnDate: '2026-12-30',
        price: 3500.5,
        status: 'CONFIRMED',
        passenger: ana,
        destination: lisbon,
      },
    ]);
    render(<TripsPage />);

    const pass = (await screen.findByRole('heading', { name: 'Lisbon' })).closest('article')!;
    expect(within(pass).getByText('Ana Souza')).toBeInTheDocument();
    expect(within(pass).getByText('20 Dec 2026')).toBeInTheDocument();
    expect(within(pass).getByText('10 nights')).toBeInTheDocument();
    expect(within(pass).getByText('3,500.50')).toBeInTheDocument();
    expect(within(pass).getByText('Confirmed')).toBeInTheDocument();
    expect(within(pass).getByRole('button', { name: 'Edit' })).toBeInTheDocument();
  });
});
