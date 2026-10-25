// Shapes returned and accepted by the TripBook API (see the Swagger UI of the back-end).

export interface Destination {
  id: number;
  city: string;
  country: string;
  description: string | null;
}

export interface DestinationInput {
  city: string;
  country: string;
  description: string;
}

export interface Passenger {
  id: number;
  name: string;
  documentNumber: string;
  email: string;
  birthDate: string | null;
  photoUrl: string | null;
}

export interface TripShare {
  id: number;
  tripId: number;
  owner: User;
  guest: User;
  sharedAt: string;
}

export interface PassengerInput {
  name: string;
  documentNumber: string;
  email: string;
  birthDate: string | null;
}

export type Currency = 'BRL' | 'USD' | 'EUR';

export const CURRENCIES: Currency[] = ['BRL', 'USD', 'EUR'];

export type TripStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED';

export const TRIP_STATUSES: TripStatus[] = ['PENDING', 'CONFIRMED', 'CANCELLED'];

export interface Trip {
  id: number;
  departureDate: string;
  returnDate: string | null;
  price: number;
  currency: Currency;
  status: TripStatus;
  passenger: Passenger;
  destination: Destination;
}

export interface TripInput {
  departureDate: string;
  returnDate: string | null;
  price: number;
  currency: Currency;
  status: TripStatus;
  passengerId: number;
  destinationId: number;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ListParams {
  page: number;
  size: number;
  /** Spring format: "field,asc" or "field,desc". Omitted means the server default. */
  sort?: string;
  /** Query filters such as { country: "port" }. Blank values are dropped. */
  filter?: Record<string, string>;
}

export interface User {
  id: number;
  name: string;
  email: string;
}

export interface AuthResult {
  token: string;
  tokenType: string;
  /** Seconds until the token expires */
  expiresIn: number;
  user: User;
}

export interface Credentials {
  email: string;
  password: string;
}

export interface Registration extends Credentials {
  name: string;
}

export interface Dashboard {
  statusCounts: { status: TripStatus; trips: number }[];
  /** One entry per currency: amounts in different currencies are never added together */
  totals: { currency: Currency; trips: number; total: number }[];
  upcoming: Trip[];
  topDestinations: { city: string; country: string; trips: number }[];
}
