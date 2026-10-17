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
}

export interface PassengerInput {
  name: string;
  documentNumber: string;
  email: string;
  birthDate: string | null;
}

export type TripStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED';

export const TRIP_STATUSES: TripStatus[] = ['PENDING', 'CONFIRMED', 'CANCELLED'];

export interface Trip {
  id: number;
  departureDate: string;
  returnDate: string | null;
  price: number;
  status: TripStatus;
  passenger: Passenger;
  destination: Destination;
}

export interface TripInput {
  departureDate: string;
  returnDate: string | null;
  price: number;
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
