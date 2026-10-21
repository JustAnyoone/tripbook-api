import { useEffect, useState, type ReactNode } from 'react';
import { destinationsApi, messageOf, passengersApi, tripsApi } from '../api/client';
import { CURRENCIES, TRIP_STATUSES, type Currency, type Destination, type Passenger, type Trip, type TripInput, type TripStatus } from '../api/types';
import { ErrorBanner } from '../components/ErrorBanner';
import { Field, FormShell } from '../components/FormShell';
import { DatePicker } from '../components/DatePicker';
import { ListPage, type FormProps, type SortOption } from '../components/ListPage';
import { formatDate, formatNights, formatPrice, nightsBetween, todayIso, validateTripDates } from '../lib/dates';

const CURRENCY_LABELS: Record<Currency, string> = {
  BRL: 'BRL, Brazilian real',
  USD: 'USD, US dollar',
  EUR: 'EUR, euro',
};

const STATUS_LABELS: Record<TripStatus, string> = {
  PENDING: 'Pending',
  CONFIRMED: 'Confirmed',
  CANCELLED: 'Cancelled',
};

function StatusBadge({ status }: { status: TripStatus }) {
  return <span className={`status status-${status.toLowerCase()}`}>{STATUS_LABELS[status]}</span>;
}

// Departure and return joined by a dashed route, with the length of the stay underneath
function Route({ trip }: { trip: Trip }) {
  const nights = nightsBetween(trip.departureDate, trip.returnDate);
  return (
    <div className="pass-route">
      <div>
        <span className="pass-small muted">Departs</span>
        <strong className="tabular">{formatDate(trip.departureDate)}</strong>
      </div>
      <span className="pass-route-line" aria-hidden="true">
        <svg width="18" height="18" viewBox="0 0 18 18">
          <path d="M16.2 8.1 2.6 3.2c-.5-.2-1 .3-.8.8l2 4.1-1.9.9v1.8l3.1-.4 1.5 3.6h1.6l-.4-3.7 6.1-.9 1.8 1.4c.4.3 1 .1 1.2-.4l.1-.4c.1-.4-.1-.7-.4-.9l-2.1-1.3.1-.6c0-.3-.1-.6-.5-.6Z" fill="currentColor" />
        </svg>
      </span>
      <div className="pass-route-end">
        <span className="pass-small muted">Returns</span>
        <strong className="tabular">{trip.returnDate ? formatDate(trip.returnDate) : 'Open return'}</strong>
      </div>
      <p className="pass-nights muted">{nights === null ? 'No return date' : formatNights(nights)}</p>
    </div>
  );
}

// One trip as a boarding pass: the coloured strip carries the status, the stub carries price and actions
function BoardingPass({ trip, actions }: { trip: Trip; actions: ReactNode }) {
  return (
    <article className={`pass pass-${trip.status.toLowerCase()}`}>
      <div className="pass-main">
        <p className="pass-passenger">
          <span className="pass-small muted">Passenger</span>
          <strong>{trip.passenger.name}</strong>
        </p>
        <h2 className="pass-destination">{trip.destination.city}</h2>
        <p className="pass-country muted">{trip.destination.country}</p>
        <Route trip={trip} />
      </div>
      <div className="pass-stub">
        <div>
          <span className="pass-small muted">Price</span>
          <strong className="pass-price tabular">{formatPrice(trip.price, trip.currency)}</strong>
        </div>
        <StatusBadge status={trip.status} />
        <span className="pass-barcode" aria-hidden="true" />
        <div className="pass-actions">{actions}</div>
      </div>
    </article>
  );
}

const SORT_OPTIONS: SortOption[] = [
  { value: 'departureDate,desc', label: 'Departure, latest first' },
  { value: 'departureDate,asc', label: 'Departure, earliest first' },
  { value: 'price,desc', label: 'Price, highest first' },
  { value: 'price,asc', label: 'Price, lowest first' },
];

// The selects offer the first 100 passengers and destinations, by name
const OPTION_LIMIT = 100;

function includeCurrent<T extends { id: number }>(items: T[], current: T | undefined): T[] {
  return current && !items.some((item) => item.id === current.id) ? [current, ...items] : items;
}

function TripForm({ initial, onSubmit, onCancel }: FormProps<Trip, TripInput>) {
  const [passengers, setPassengers] = useState<Passenger[] | null>(null);
  const [destinations, setDestinations] = useState<Destination[] | null>(null);
  const [truncated, setTruncated] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [passengerId, setPassengerId] = useState(initial ? String(initial.passenger.id) : '');
  const [destinationId, setDestinationId] = useState(initial ? String(initial.destination.id) : '');
  const [departureDate, setDepartureDate] = useState(initial?.departureDate ?? '');
  const [returnDate, setReturnDate] = useState(initial?.returnDate ?? '');
  const [price, setPrice] = useState(initial ? String(initial.price) : '');
  const [currency, setCurrency] = useState<Currency>(initial?.currency ?? 'BRL');
  const [status, setStatus] = useState<TripStatus>(initial?.status ?? 'PENDING');

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      passengersApi.list({ page: 0, size: OPTION_LIMIT, sort: 'name,asc' }),
      destinationsApi.list({ page: 0, size: OPTION_LIMIT, sort: 'city,asc' }),
    ])
      .then(([passengerPage, destinationPage]) => {
        if (cancelled) return;
        setPassengers(includeCurrent(passengerPage.content, initial?.passenger));
        setDestinations(includeCurrent(destinationPage.content, initial?.destination));
        setTruncated(passengerPage.totalElements > OPTION_LIMIT || destinationPage.totalElements > OPTION_LIMIT);
      })
      .catch((error: unknown) => {
        if (!cancelled) setLoadError(messageOf(error));
      });
    return () => {
      cancelled = true;
    };
  }, [initial]);

  if (loadError) {
    return (
      <>
        <ErrorBanner message={loadError} />
        <div className="dialog-actions">
          <button type="button" className="button button-quiet" onClick={onCancel}>
            Close
          </button>
        </div>
      </>
    );
  }

  if (!passengers || !destinations) return <p className="dialog-text">Loading passengers and destinations…</p>;

  const today = todayIso();
  // Dates are picked from the calendar, never typed. Nothing before today can be chosen, and the
  // return day cannot come before the departure day. (An unchanged date of an old trip stays valid.)
  const minReturn = departureDate && departureDate > today ? departureDate : today;

  function chooseDeparture(value: string) {
    setDepartureDate(value);
    // A return that now falls before the new departure would be invalid, so ask for it again
    if (returnDate && value && returnDate < value) setReturnDate('');
  }

  const missingData = passengers.length === 0 || destinations.length === 0;

  return (
    <FormShell
      submitLabel={initial ? 'Save changes' : 'Add trip'}
      onSubmit={async () => {
        if (!departureDate) throw new Error('Choose a departure date.');
        const problem = validateTripDates({ departureDate, returnDate: returnDate || null }, today, initial ?? undefined);
        if (problem) throw new Error(problem);
        await onSubmit({
          departureDate,
          returnDate: returnDate || null,
          price: Number(price),
          currency,
          status,
          passengerId: Number(passengerId),
          destinationId: Number(destinationId),
        });
      }}
      onCancel={onCancel}
    >
      {missingData && <p className="field-hint">A trip needs at least one passenger and one destination. Add them first.</p>}
      <Field label="Passenger">
        {(id) => (
          <select id={id} value={passengerId} onChange={(e) => setPassengerId(e.target.value)} required>
            <option value="">Choose a passenger</option>
            {passengers.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Destination" hint={truncated ? `Showing the first ${OPTION_LIMIT} of each list.` : undefined}>
        {(id) => (
          <select id={id} value={destinationId} onChange={(e) => setDestinationId(e.target.value)} required>
            <option value="">Choose a destination</option>
            {destinations.map((d) => (
              <option key={d.id} value={d.id}>
                {d.city}, {d.country}
              </option>
            ))}
          </select>
        )}
      </Field>
      <div className="field-row">
        <Field label="Departure date">
          {(id) => <DatePicker id={id} value={departureDate} min={today} onChange={chooseDeparture} />}
        </Field>
        <Field label="Return date" hint="Optional">
          {(id) => <DatePicker id={id} value={returnDate} min={minReturn} onChange={setReturnDate} clearable placeholder="No return date" />}
        </Field>
      </div>
      <div className="field-row">
        <Field label="Currency">
          {(id) => (
            <select id={id} value={currency} onChange={(e) => setCurrency(e.target.value as Currency)}>
              {CURRENCIES.map((value) => (
                <option key={value} value={value}>
                  {CURRENCY_LABELS[value]}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Price">
          {(id) => <input id={id} type="number" min="0.01" step="0.01" value={price} onChange={(e) => setPrice(e.target.value)} required />}
        </Field>
      </div>
      <Field label="Status">
        {(id) => (
          <select id={id} value={status} onChange={(e) => setStatus(e.target.value as TripStatus)}>
            {TRIP_STATUSES.map((value) => (
              <option key={value} value={value}>
                {STATUS_LABELS[value]}
              </option>
            ))}
          </select>
        )}
      </Field>
    </FormShell>
  );
}

export function TripsPage() {
  return (
    <ListPage<Trip, TripInput>
      title="Trips"
      noun="trip"
      nounPlural="trips"
      api={tripsApi}
      filter={{
        label: 'Status',
        param: 'status',
        options: TRIP_STATUSES.map((value) => ({ value, label: STATUS_LABELS[value] })),
      }}
      renderCard={(trip, actions) => <BoardingPass trip={trip} actions={actions} />}
      sortOptions={SORT_OPTIONS}
      renderForm={(props) => <TripForm {...props} />}
      describe={(t) => `the trip of ${t.passenger.name} to ${t.destination.city}`}
      emptyHint="Book the first one once you have a passenger and a destination."
    />
  );
}
