import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, buildQuery, destinationsApi } from './client';

function jsonResponse(status: number, body: unknown) {
  return Promise.resolve(
    new Response(body === null ? null : JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    }),
  );
}

afterEach(() => vi.unstubAllGlobals());

describe('buildQuery', () => {
  it('sends page, size and sort', () => {
    expect(buildQuery({ page: 2, size: 20, sort: 'city,desc' })).toBe('page=2&size=20&sort=city%2Cdesc');
  });

  it('keeps filters with a value and drops blank ones', () => {
    const query = buildQuery({ page: 0, size: 10, filter: { country: ' port ', name: '   ' } });
    expect(query).toBe('page=0&size=10&country=port');
  });
});

describe('API client', () => {
  it('turns the API error body into the message of an ApiError', async () => {
    vi.stubGlobal('fetch', vi.fn(() => jsonResponse(409, { status: 409, message: 'Cannot delete destination: it has trips associated with it' })));

    const failure = destinationsApi.remove(1);

    await expect(failure).rejects.toBeInstanceOf(ApiError);
    await expect(failure).rejects.toMatchObject({
      status: 409,
      message: 'Cannot delete destination: it has trips associated with it',
    });
  });

  it('falls back to a generic message when the error body is not JSON', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(new Response('<html>Bad gateway</html>', { status: 502 }))));

    await expect(destinationsApi.list({ page: 0, size: 10 })).rejects.toMatchObject({
      status: 502,
      message: 'The request failed (HTTP 502).',
    });
  });

  it('explains a network failure in plain words', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new TypeError('Failed to fetch'))));

    await expect(destinationsApi.list({ page: 0, size: 10 })).rejects.toMatchObject({
      status: 0,
      message: 'Cannot reach the TripBook API. Check that the server is running.',
    });
  });

  it('returns nothing for a 204 response', async () => {
    vi.stubGlobal('fetch', vi.fn(() => jsonResponse(204, null)));

    await expect(destinationsApi.remove(1)).resolves.toBeUndefined();
  });

  it('posts JSON to create a destination', async () => {
    const fetchMock = vi.fn(() => jsonResponse(201, { id: 1, city: 'Lisbon', country: 'Portugal', description: '' }));
    vi.stubGlobal('fetch', fetchMock);

    await destinationsApi.create({ city: 'Lisbon', country: 'Portugal', description: '' });

    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe('/api/destinations');
    expect(init.method).toBe('POST');
    expect(JSON.parse(init.body as string)).toEqual({ city: 'Lisbon', country: 'Portugal', description: '' });
  });
});
