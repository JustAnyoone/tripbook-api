import { destinationsApi, passengersApi, tripsApi } from '../api/client';
import type { Currency, Destination, DestinationInput, Passenger, PassengerInput, TripInput, TripStatus } from '../api/types';
import { addDays, todayIso } from './dates';

const DESTINATIONS: DestinationInput[] = [
  { city: 'Rio de Janeiro', country: 'Brazil', description: 'Beaches, Sugarloaf Mountain and Christ the Redeemer.' },
  { city: 'Lisbon', country: 'Portugal', description: 'Hilltop viewpoints, trams and custard tarts.' },
  { city: 'Tokyo', country: 'Japan', description: 'Neon districts, quiet temples and great food.' },
  { city: 'New York', country: 'United States', description: 'Museums, Central Park and Broadway.' },
  { city: 'Paris', country: 'France', description: 'Cafes, art and the Seine at sunset.' },
  { city: 'Buenos Aires', country: 'Argentina', description: 'Tango, steak and wide European-style avenues.' },
];

const PASSENGERS: PassengerInput[] = [
  { name: 'Ana Souza', documentNumber: 'DEMO0001', email: 'ana.souza@example.com', birthDate: '1992-03-14' },
  { name: 'Bruno Lima', documentNumber: 'DEMO0002', email: 'bruno.lima@example.com', birthDate: '1988-11-02' },
  { name: 'Carla Mendes', documentNumber: 'DEMO0003', email: 'carla.mendes@example.com', birthDate: '1995-07-23' },
  { name: 'Diego Costa', documentNumber: 'DEMO0004', email: 'diego.costa@example.com', birthDate: null },
];

interface TripPlan {
  passenger: string; // document number
  destination: string; // city
  departureInDays: number;
  stayDays: number | null;
  price: number;
  currency: Currency;
  status: TripStatus;
}

// Dates are relative to today so the sample always has upcoming and past trips
const TRIPS: TripPlan[] = [
  { passenger: 'DEMO0001', destination: 'Lisbon', departureInDays: 30, stayDays: 10, price: 5400, currency: 'EUR', status: 'CONFIRMED' },
  { passenger: 'DEMO0001', destination: 'Rio de Janeiro', departureInDays: -40, stayDays: 7, price: 2100, currency: 'BRL', status: 'CONFIRMED' },
  { passenger: 'DEMO0002', destination: 'Tokyo', departureInDays: 75, stayDays: 14, price: 9800, currency: 'USD', status: 'PENDING' },
  { passenger: 'DEMO0002', destination: 'New York', departureInDays: 15, stayDays: 6, price: 3200, currency: 'USD', status: 'CONFIRMED' },
  { passenger: 'DEMO0003', destination: 'Paris', departureInDays: 120, stayDays: null, price: 4700, currency: 'EUR', status: 'PENDING' },
  { passenger: 'DEMO0004', destination: 'Buenos Aires', departureInDays: -15, stayDays: 5, price: 1800, currency: 'BRL', status: 'CANCELLED' },
];

export interface SeedResult {
  destinations: number;
  passengers: number;
  trips: number;
}

// Everything is loaded in one request: a sample never needs more than a few hundred rows to compare against
async function all<T>(list: (params: { page: number; size: number }) => Promise<{ content: T[] }>): Promise<T[]> {
  return (await list({ page: 0, size: 500 })).content;
}

const key = (...parts: string[]) => parts.map((part) => part.trim().toLowerCase()).join('|');

// Creates the sample data through the normal API, skipping what already exists, so running it twice
// adds nothing. Returns how many records were created.
export async function loadSampleData(): Promise<SeedResult> {
  const result: SeedResult = { destinations: 0, passengers: 0, trips: 0 };

  const destinations = new Map<string, Destination>();
  for (const existing of await all(destinationsApi.list)) destinations.set(key(existing.city, existing.country), existing);
  for (const input of DESTINATIONS) {
    const id = key(input.city, input.country);
    if (!destinations.has(id)) {
      destinations.set(id, await destinationsApi.create(input));
      result.destinations++;
    }
  }

  const passengers = new Map<string, Passenger>();
  for (const existing of await all(passengersApi.list)) passengers.set(existing.documentNumber.toLowerCase(), existing);
  for (const input of PASSENGERS) {
    const id = input.documentNumber.toLowerCase();
    if (!passengers.has(id)) {
      passengers.set(id, await passengersApi.create(input));
      result.passengers++;
    }
  }

  const trips = new Set((await all(tripsApi.list)).map((t) => key(String(t.passenger.id), String(t.destination.id), t.price.toString(), t.currency)));
  const today = todayIso();
  for (const plan of TRIPS) {
    const passenger = passengers.get(plan.passenger.toLowerCase());
    const destination = [...destinations.values()].find((d) => d.city === plan.destination);
    if (!passenger || !destination) continue;
    const id = key(String(passenger.id), String(destination.id), plan.price.toString(), plan.currency);
    if (trips.has(id)) continue;
    const departureDate = addDays(today, plan.departureInDays);
    await tripsApi.create({
      departureDate,
      returnDate: plan.stayDays === null ? null : addDays(departureDate, plan.stayDays),
      price: plan.price,
      currency: plan.currency,
      status: plan.status,
      passengerId: passenger.id,
      destinationId: destination.id,
    } satisfies TripInput);
    trips.add(id);
    result.trips++;
  }
  return result;
}
