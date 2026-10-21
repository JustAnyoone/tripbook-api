import { afterEach, describe, expect, it, vi } from 'vitest';
import { loadSampleData } from './seed';

// A tiny in-memory API so the seed can be run twice
function stubBackend() {
  const db = { destinations: [] as any[], passengers: [] as any[], trips: [] as any[] };
  let next = 1;
  const page = (content: unknown[]) => ({ content, page: 0, size: 500, totalElements: content.length, totalPages: 1, last: true });
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: string, init?: RequestInit) => {
      const path = String(url).split('?')[0].replace('/api/', '');
      const ok = (body: unknown) => new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } });
      if ((init?.method ?? 'GET') === 'GET') return ok(page(db[path as keyof typeof db]));
      const body = JSON.parse(String(init?.body));
      if (path === 'trips') {
        const trip = {
          id: next++,
          ...body,
          passenger: db.passengers.find((p) => p.id === body.passengerId),
          destination: db.destinations.find((d) => d.id === body.destinationId),
        };
        db.trips.push(trip);
        return ok(trip);
      }
      const row = { id: next++, ...body };
      db[path as 'destinations' | 'passengers'].push(row);
      return ok(row);
    }),
  );
  return db;
}

afterEach(() => vi.unstubAllGlobals());

describe('loadSampleData', () => {
  it('creates destinations, passengers and trips that reference each other', async () => {
    const db = stubBackend();

    const result = await loadSampleData();

    expect(result).toEqual({ destinations: 6, passengers: 4, trips: 6 });
    expect(db.trips.every((trip) => trip.passenger && trip.destination)).toBe(true);
    expect(db.trips.some((trip) => trip.returnDate === null)).toBe(true);
  });

  it('adds nothing the second time', async () => {
    const db = stubBackend();
    await loadSampleData();

    const second = await loadSampleData();

    expect(second).toEqual({ destinations: 0, passengers: 0, trips: 0 });
    expect(db.trips).toHaveLength(6);
  });
});
