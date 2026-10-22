import { useEffect, useState, type ReactNode } from 'react';
import { destinationsApi, messageOf, passengersApi, tripsApi } from '../api/client';
import { CURRENCIES, TRIP_STATUSES, type Currency, type Destination, type Passenger, type Trip, type TripInput, type TripStatus } from '../api/types';
import { ErrorBanner } from '../components/ErrorBanner';
import { Field, FormShell } from '../components/FormShell';
import { DownloadIcon, PlaneIcon } from '../components/icons';
import { CountryLabel } from '../components/Flag';
import { DatePicker } from '../components/DatePicker';
import { ListPage, type FormProps, type SortOption } from '../components/ListPage';
import { t } from '../i18n';
import { countryName } from '../lib/countries';
import { downloadFile, tripsToCsv } from '../lib/csv';
import { formatDate, formatNights, formatPrice, nightsBetween, todayIso, validateTripDates } from '../lib/dates';


function StatusBadge({ status }: { status: TripStatus }) {
  return <span className={`status status-${status.toLowerCase()}`}>{t(`status.${status}`)}</span>;
}

// Departure and return joined by a dashed route, with the length of the stay underneath
function Route({ trip }: { trip: Trip }) {
  const nights = nightsBetween(trip.departureDate, trip.returnDate);
  return (
    <div className="pass-route">
      <div>
        <span className="pass-small muted">{t('trips.departs')}</span>
        <strong className="tabular">{formatDate(trip.departureDate)}</strong>
      </div>
      <span className="pass-route-line" aria-hidden="true">
        <PlaneIcon />
      </span>
      <div className="pass-route-end">
        <span className="pass-small muted">{t('trips.returns')}</span>
        <strong className="tabular">{trip.returnDate ? formatDate(trip.returnDate) : t('trips.openReturn')}</strong>
      </div>
      <p className="pass-nights muted">{nights === null ? t('trips.noReturn') : formatNights(nights)}</p>
    </div>
  );
}

// One trip as a boarding pass: the coloured strip carries the status, the stub carries price and actions
function BoardingPass({ trip, actions }: { trip: Trip; actions: ReactNode }) {
  return (
    <article className={`pass pass-${trip.status.toLowerCase()}`}>
      <div className="pass-main">
        <p className="pass-passenger">
          <span className="pass-small muted">{t('trips.passenger')}</span>
          <strong>{trip.passenger.name}</strong>
        </p>
        <h2 className="pass-destination">{trip.destination.city}</h2>
        <p className="pass-country muted">
          <CountryLabel country={trip.destination.country} />
        </p>
        <Route trip={trip} />
      </div>
      <div className="pass-stub">
        <div>
          <span className="pass-small muted">{t('trips.price')}</span>
          <strong className="pass-price tabular">{formatPrice(trip.price, trip.currency)}</strong>
        </div>
        <StatusBadge status={trip.status} />
        <span className="pass-barcode" aria-hidden="true" />
        <div className="pass-actions">{actions}</div>
      </div>
    </article>
  );
}

const sortOptions = (): SortOption[] => [
  { value: 'departureDate,desc', label: t('trips.sort.departureDesc') },
  { value: 'departureDate,asc', label: t('trips.sort.departureAsc') },
  // The API groups prices by currency first: 100 EUR and 100 BRL are not the same amount
  { value: 'price,desc', label: t('trips.sort.priceDesc') },
  { value: 'price,asc', label: t('trips.sort.priceAsc') },
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
            {t('common.close')}
          </button>
        </div>
      </>
    );
  }

  if (!passengers || !destinations) return <p className="dialog-text">{t('trips.formLoading')}</p>;

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
      submitLabel={initial ? t('common.saveChanges') : t('trips.add')}
      onSubmit={async () => {
        if (!departureDate) throw new Error(t('trips.chooseDeparture'));
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
      {missingData && <p className="field-hint">{t('trips.needData')}</p>}
      <Field label={t('trips.passengerField')}>
        {(id) => (
          <select id={id} value={passengerId} onChange={(e) => setPassengerId(e.target.value)} required>
            <option value="">{t('trips.choosePassenger')}</option>
            {passengers.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label={t('trips.destinationField')} hint={truncated ? t('trips.firstOnly', { limit: OPTION_LIMIT }) : undefined}>
        {(id) => (
          <select id={id} value={destinationId} onChange={(e) => setDestinationId(e.target.value)} required>
            <option value="">{t('trips.chooseDestination')}</option>
            {destinations.map((d) => (
              <option key={d.id} value={d.id}>
                {d.city}, {countryName(d.country)}
              </option>
            ))}
          </select>
        )}
      </Field>
      <div className="field-row">
        <Field label={t('trips.departureDate')}>
          {(id) => <DatePicker id={id} value={departureDate} min={today} onChange={chooseDeparture} />}
        </Field>
        <Field label={t('trips.returnDate')} hint={t('common.optional')}>
          {(id) => <DatePicker id={id} value={returnDate} min={minReturn} onChange={setReturnDate} clearable placeholder={t('trips.noReturnPlaceholder')} />}
        </Field>
      </div>
      <div className="field-row">
        <Field label={t('trips.currencyField')}>
          {(id) => (
            <select id={id} value={currency} onChange={(e) => setCurrency(e.target.value as Currency)}>
              {CURRENCIES.map((value) => (
                <option key={value} value={value}>
                  {t(`currency.${value}`)}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label={t('trips.priceField')}>
          {(id) => <input id={id} type="number" min="0.01" step="0.01" value={price} onChange={(e) => setPrice(e.target.value)} required />}
        </Field>
      </div>
      <Field label={t('trips.statusField')}>
        {(id) => (
          <select id={id} value={status} onChange={(e) => setStatus(e.target.value as TripStatus)}>
            {TRIP_STATUSES.map((value) => (
              <option key={value} value={value}>
                {t(`status.${value}`)}
              </option>
            ))}
          </select>
        )}
      </Field>
    </FormShell>
  );
}

// Downloads every trip of the account as a spreadsheet-friendly CSV file
function ExportButton() {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function exportTrips() {
    setBusy(true);
    setError(null);
    try {
      const page = await tripsApi.list({ page: 0, size: 2000, sort: 'departureDate,asc' });
      downloadFile(`tripbook-trips-${todayIso()}.csv`, tripsToCsv(page.content), 'text/csv;charset=utf-8');
    } catch (failure) {
      setError(messageOf(failure));
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <button type="button" className="button button-outline button-with-icon" onClick={exportTrips} disabled={busy}>
        <DownloadIcon />
        {t('trips.export')}
      </button>
      {error && (
        <span className="muted" role="alert">
          {error}
        </span>
      )}
    </>
  );
}

export function TripsPage() {
  return (
    <ListPage<Trip, TripInput>
      resource="trips"
      api={tripsApi}
      filter={{
        label: t('trips.statusField'),
        param: 'status',
        options: TRIP_STATUSES.map((value) => ({ value, label: t(`status.${value}`) })),
      }}
      renderCard={(trip, actions) => <BoardingPass trip={trip} actions={actions} />}
      sortOptions={sortOptions()}
      headerActions={<ExportButton />}
      renderForm={(props) => <TripForm {...props} />}
      deleteParams={(trip) => ({ passenger: trip.passenger.name, city: trip.destination.city })}
    />
  );
}
