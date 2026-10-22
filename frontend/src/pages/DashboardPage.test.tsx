import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { DashboardPage } from './DashboardPage';

const lisbon = { id: 1, city: 'Lisbon', country: 'Portugal', description: null };
const ana = { id: 1, name: 'Ana Souza', documentNumber: 'AB123456', email: 'a@x.com', birthDate: null };

function stub(body: unknown) {
  vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })));
}

function renderPage() {
  return render(
    <MemoryRouter>
      <DashboardPage />
    </MemoryRouter>,
  );
}

afterEach(() => vi.unstubAllGlobals());

describe('DashboardPage', () => {
  it('shows totals per currency, upcoming trips and top destinations', async () => {
    stub({
      statusCounts: [
        { status: 'CONFIRMED', trips: 2 },
        { status: 'CANCELLED', trips: 1 },
      ],
      totals: [
        { currency: 'BRL', trips: 1, total: 2000 },
        { currency: 'EUR', trips: 1, total: 5400 },
      ],
      upcoming: [{ id: 9, departureDate: '2026-12-01', returnDate: null, price: 5400, currency: 'EUR', status: 'CONFIRMED', passenger: ana, destination: lisbon }],
      topDestinations: [{ city: 'Lisbon', country: 'Portugal', trips: 1 }],
    });

    renderPage();

    expect(await screen.findByText('3 trips in total')).toBeInTheDocument();
    expect(screen.getByText('Booked in BRL')).toBeInTheDocument();
    expect(screen.getByText('Booked in EUR')).toBeInTheDocument();
    expect(screen.getByText('Next departures')).toBeInTheDocument();
    expect(screen.getAllByText('Lisbon').length).toBe(2);
    expect(screen.getByText('Pending').nextSibling).toHaveTextContent('0');
  });

  it('invites the user to book a first trip when there is nothing yet', async () => {
    stub({ statusCounts: [], totals: [], upcoming: [], topDestinations: [] });

    renderPage();

    expect(await screen.findByText(/Nothing booked yet/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'book the first trip' })).toHaveAttribute('href', '/trips');
  });

  it('shows the API message when loading fails', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify({ status: 500, message: 'Boom' }), { status: 500, headers: { 'Content-Type': 'application/json' } })));

    renderPage();

    expect(await screen.findByRole('alert')).toBeInTheDocument();
  });
});
