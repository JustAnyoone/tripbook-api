import { useEffect, useState } from 'react';
import { destinationsApi, messageOf, passengersApi, tripsApi } from '../api/client';
import { TRIP_STATUSES, type Destination, type Passenger, type Trip, type TripInput, type TripStatus } from '../api/types';
import { ErrorBanner } from '../components/ErrorBanner';
import { Field, FormShell } from '../components/FormShell';
import { ListPage, type Column, type FormProps } from '../components/ListPage';
import { formatDate, formatNights, formatPrice, nightsBetween, todayIso, validateTripDates } from '../lib/dates';

const STATUS_LABELS: Record<TripStatus, string> = {
  PENDING: 'Pending',
  CONFIRMED: 'Confirmed',
  CANCELLED: 'Cancelled',
};

// Departure, return and length of the stay on one line: the thing people scan a trip list for
function Itinerary({ trip }: { trip: Trip }) {
  const nights = nightsBetween(trip.departureDate, trip.returnDate);
  return (
    <div className="itinerary">
      <span className="tabular">{formatDate(trip.departureDate)}</span>
      <span className="itinerary-line" aria-hidden="true" />
      <span className="tabular">{trip.returnDate ? formatDate(trip.returnDate) : 'Open return'}</span>
      <span className="itinerary-nights muted">{nights === null ? 'No return date' : formatNights(nights)}</span>
    </div>
  );
}

function StatusBadge({ status }: { status: TripStatus }) {
  return <span className={`status status-${status.toLowerCase()}`}>{STATUS_LABELS[status]}</span>;
}

const columns: Column<Trip>[] = [
  { key: 'itinerary', header: 'Itinerary', sortField: 'departureDate', render: (t) => <Itinerary trip={t} /> },
  {
    key: 'passenger',
    header: 'Passenger',
    render: (t) => (
      <>
        <strong>{t.passenger.name}</strong>
        <span className="cell-sub muted">{t.passenger.email}</span>
      </>
    ),
  },
  {
    key: 'destination',
    header: 'Destination',
    render: (t) => (
      <>
        <strong>{t.destination.city}</strong>
        <span className="cell-sub muted">{t.destination.country}</span>
      </>
    ),
  },
  { key: 'price', header: 'Price', sortField: 'price', align: 'end', render: (t) => <span className="tabular">{formatPrice(t.price)}</span> },
  { key: 'status', header: 'Status', sortField: 'status', render: (t) => <StatusBadge status={t.status} /> },
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
  // An unchanged date of an existing trip may already be in the past; any new date may not
  const minDeparture = departureDate === initial?.departureDate ? undefined : today;
  const minReturn = [departureDate, returnDate === initial?.returnDate ? undefined : today]
    .filter((value): value is string => Boolean(value))
    .sort()
    .at(-1);

  const missingData = passengers.length === 0 || destinations.length === 0;

  return (
    <FormShell
      submitLabel={initial ? 'Save changes' : 'Add trip'}
      onSubmit={async () => {
        const problem = validateTripDates({ departureDate, returnDate: returnDate || null }, today, initial ?? undefined);
        if (problem) throw new Error(problem);
        await onSubmit({
          departureDate,
          returnDate: returnDate || null,
          price: Number(price),
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
          {(id) => <input id={id} type="date" value={departureDate} min={minDeparture} onChange={(e) => setDepartureDate(e.target.value)} required />}
        </Field>
        <Field label="Return date" hint="Optional">
          {(id) => <input id={id} type="date" value={returnDate} min={minReturn} onChange={(e) => setReturnDate(e.target.value)} />}
        </Field>
      </div>
      <div className="field-row">
        <Field label="Price">
          {(id) => <input id={id} type="number" min="0.01" step="0.01" value={price} onChange={(e) => setPrice(e.target.value)} required />}
        </Field>
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
      </div>
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
      columns={columns}
      renderForm={(props) => <TripForm {...props} />}
      describe={(t) => `the trip of ${t.passenger.name} to ${t.destination.city}`}
      emptyHint="Book the first one once you have a passenger and a destination."
    />
  );
}
