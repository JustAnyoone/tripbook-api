import { getToken } from '../auth/token';
import type {
  AuthResult,
  Credentials,
  Dashboard,
  Destination,
  DestinationInput,
  ListParams,
  Page,
  Passenger,
  PassengerInput,
  Registration,
  Trip,
  TripInput,
  User,
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

// The auth provider registers a callback here: when a signed-in user's token is rejected
// (expired or invalid), it clears the session and sends the user back to the login page.
let onUnauthorized: (() => void) | null = null;

export function setUnauthorizedHandler(handler: (() => void) | null) {
  onUnauthorized = handler;
}

export function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : 'Something went wrong. Try again.';
}

interface RequestOptions {
  /** Login and register answer 401 for wrong credentials: that is a message, not an expired session. */
  publicEndpoint?: boolean;
}

async function request<T>(path: string, init?: RequestInit, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {};
  if (init?.body) headers['Content-Type'] = 'application/json';
  const token = getToken();
  if (token && !options.publicEndpoint) headers.Authorization = `Bearer ${token}`;

  let response: Response;
  try {
    response = await fetch(BASE_URL + path, {
      ...init,
      headers: Object.keys(headers).length > 0 ? headers : undefined,
    });
  } catch {
    throw new ApiError(0, 'Cannot reach the TripBook API. Check that the server is running.');
  }

  if (response.status === 401 && token && !options.publicEndpoint) onUnauthorized?.();

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

export const dashboardApi = {
  summary: () => request<Dashboard>('/api/dashboard'),
};

export const authApi = {
  login: (credentials: Credentials) =>
    request<AuthResult>('/api/auth/login', { method: 'POST', body: JSON.stringify(credentials) }, { publicEndpoint: true }),
  register: (registration: Registration) =>
    request<AuthResult>('/api/auth/register', { method: 'POST', body: JSON.stringify(registration) }, { publicEndpoint: true }),
  me: () => request<User>('/api/auth/me'),
};
