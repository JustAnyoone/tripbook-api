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

  const [existingDestinations, existingPassengers] = await Promise.all([all(destinationsApi.list), all(passengersApi.list)]);
  const destinations = new Map<string, Destination>();
  for (const existing of existingDestinations) destinations.set(key(existing.city, existing.country), existing);
  // Created in parallel: the three groups are independent of each other until the trips
  const missingDestinations = DESTINATIONS.filter((input) => !destinations.has(key(input.city, input.country)));
  for (const created of await Promise.all(missingDestinations.map((input) => destinationsApi.create(input)))) {
    destinations.set(key(created.city, created.country), created);
    result.destinations++;
  }

  const passengers = new Map<string, Passenger>();
  for (const existing of existingPassengers) passengers.set(existing.documentNumber.toLowerCase(), existing);
  const missingPassengers = PASSENGERS.filter((input) => !passengers.has(input.documentNumber.toLowerCase()));
  for (const created of await Promise.all(missingPassengers.map((input) => passengersApi.create(input)))) {
    passengers.set(created.documentNumber.toLowerCase(), created);
    result.passengers++;
  }

  const trips = new Set((await all(tripsApi.list)).map((t) => key(String(t.passenger.id), String(t.destination.id), t.price.toString(), t.currency)));
  const today = todayIso();
  const inputs: TripInput[] = [];
  for (const plan of TRIPS) {
    const passenger = passengers.get(plan.passenger.toLowerCase());
    const destination = [...destinations.values()].find((d) => d.city === plan.destination);
    if (!passenger || !destination) continue;
    if (trips.has(key(String(passenger.id), String(destination.id), plan.price.toString(), plan.currency))) continue;
    const departureDate = addDays(today, plan.departureInDays);
    inputs.push({
      departureDate,
      returnDate: plan.stayDays === null ? null : addDays(departureDate, plan.stayDays),
      price: plan.price,
      currency: plan.currency,
      status: plan.status,
      passengerId: passenger.id,
      destinationId: destination.id,
    });
  }
  await Promise.all(inputs.map((input) => tripsApi.create(input)));
  result.trips = inputs.length;
  return result;
}
