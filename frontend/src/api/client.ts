import type {
  Destination,
  DestinationInput,
  ListParams,
  Page,
  Passenger,
  PassengerInput,
  Trip,
  TripInput,
} from './types';

const BASE_URL: string = import.meta.env.VITE_API_URL ?? '';

// Every failure the user can see ends up as an ApiError with a readable message.
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : 'Something went wrong. Try again.';
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(BASE_URL + path, {
      ...init,
      headers: init?.body ? { 'Content-Type': 'application/json' } : undefined,
    });
  } catch {
    throw new ApiError(0, 'Cannot reach the TripBook API. Check that the server is running.');
  }

  if (!response.ok) {
    // The API answers errors as { status, message, timestamp }
    let message = `The request failed (HTTP ${response.status}).`;
    try {
      const body: unknown = await response.json();
      if (typeof body === 'object' && body !== null && 'message' in body) {
        const apiMessage = (body as { message: unknown }).message;
        if (typeof apiMessage === 'string' && apiMessage.length > 0) message = apiMessage;
      }
    } catch {
      // The body was not JSON; keep the generic message
    }
    throw new ApiError(response.status, message);
  }

  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

export function buildQuery({ page, size, sort, filter }: ListParams): string {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (sort) query.set('sort', sort);
  for (const [name, value] of Object.entries(filter ?? {})) {
    if (value.trim()) query.set(name, value.trim());
  }
  return query.toString();
}

export interface Resource<T, I> {
  list(params: ListParams): Promise<Page<T>>;
  create(input: I): Promise<T>;
  update(id: number, input: I): Promise<T>;
  remove(id: number): Promise<void>;
}

function makeResource<T, I>(path: string): Resource<T, I> {
  return {
    list: (params) => request<Page<T>>(`${path}?${buildQuery(params)}`),
    create: (input) => request<T>(path, { method: 'POST', body: JSON.stringify(input) }),
    update: (id, input) => request<T>(`${path}/${id}`, { method: 'PUT', body: JSON.stringify(input) }),
    remove: (id) => request<void>(`${path}/${id}`, { method: 'DELETE' }),
  };
}

export const destinationsApi = makeResource<Destination, DestinationInput>('/api/destinations');
export const passengersApi = makeResource<Passenger, PassengerInput>('/api/passengers');
export const tripsApi = makeResource<Trip, TripInput>('/api/trips');
