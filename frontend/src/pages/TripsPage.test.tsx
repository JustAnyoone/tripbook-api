import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { todayIso } from '../lib/dates';
import { TripsPage } from './TripsPage';

const emptyPage = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true };
const ana = { id: 1, name: 'Ana Souza', documentNumber: 'AB123456', email: 'ana@email.com', birthDate: null };
const lisbon = { id: 1, city: 'Lisbon', country: 'Portugal', description: null };

function page(content: unknown[]) {
  return { ...emptyPage, content, totalElements: content.length, totalPages: content.length ? 1 : 0 };
}

let fetchMock: ReturnType<typeof vi.fn>;

beforeEach(() => {
  fetchMock = vi.fn((url: string) => {
    const body = String(url).includes('/api/passengers')
      ? page([ana])
      : String(url).includes('/api/destinations')
        ? page([lisbon])
        : emptyPage;
    return Promise.resolve(new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } }));
  });
  vi.stubGlobal('fetch', fetchMock);
});
afterEach(() => vi.unstubAllGlobals());

describe('trip form dates', () => {
  it('does not offer days before today, and the return cannot precede the departure', async () => {
    const user = userEvent.setup();
    render(<TripsPage />);

    await user.click((await screen.findAllByRole('button', { name: 'Add trip' }))[0]);
    const departure = await screen.findByLabelText('Departure date');
    const returning = screen.getByLabelText('Return date');

    expect(departure).toHaveAttribute('min', todayIso());
    expect(returning).toHaveAttribute('min', todayIso());

    await user.type(departure, '2099-03-10');
    expect(returning).toHaveAttribute('min', '2099-03-10');
  });

  it('does not send the form when the departure is in the past', async () => {
    const user = userEvent.setup();
    render(<TripsPage />);

    await user.click((await screen.findAllByRole('button', { name: 'Add trip' }))[0]);
    await user.selectOptions(await screen.findByLabelText('Passenger'), '1');
    await user.selectOptions(screen.getByLabelText('Destination'), '1');
    await user.type(screen.getByLabelText('Departure date'), '2020-01-10');
    await user.type(screen.getByLabelText('Price'), '100');
    const form = screen.getByLabelText('Departure date').closest('form')!;
    form.noValidate = true; // skip the browser's own check to reach the app's check
    await user.click(within(form).getByRole('button', { name: 'Add trip' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Departure date cannot be in the past.');
    expect(fetchMock.mock.calls.some(([, init]) => (init as RequestInit | undefined)?.method === 'POST')).toBe(false);
  });
});
