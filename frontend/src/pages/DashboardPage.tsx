import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { dashboardApi, messageOf } from '../api/client';
import type { Dashboard, TripStatus } from '../api/types';
import { CountryLabel } from '../components/Flag';
import { ErrorBanner } from '../components/ErrorBanner';
import { useRefresh } from '../components/RefreshContext';
import { formatDate, formatPrice } from '../lib/dates';

const STATUS_LABELS: Record<TripStatus, string> = { PENDING: 'Pending', CONFIRMED: 'Confirmed', CANCELLED: 'Cancelled' };
const STATUS_ORDER: TripStatus[] = ['CONFIRMED', 'PENDING', 'CANCELLED'];

// First screen after signing in: how much is booked, what is coming up and where the trips go
export function DashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { version } = useRefresh();

  useEffect(() => {
    let cancelled = false;
    dashboardApi
      .summary()
      .then((summary) => {
        if (!cancelled) setData(summary);
      })
      .catch((failure: unknown) => {
        if (!cancelled) setError(messageOf(failure));
      });
    return () => {
      cancelled = true;
    };
  }, [version]);

  if (error) return <ErrorBanner message={error} />;
  if (!data) return <p className="page-loading" role="status">Loading</p>;

  const counts = new Map(data.statusCounts.map((row) => [row.status, row.trips]));
  const totalTrips = data.statusCounts.reduce((sum, row) => sum + row.trips, 0);
  const maxTrips = Math.max(...data.topDestinations.map((row) => row.trips), 1);

  if (totalTrips === 0) {
    return (
      <section>
        <header className="page-header">
          <h1>Dashboard</h1>
        </header>
        <p className="empty-note">
          Nothing booked yet. Add a passenger and a destination, then <Link to="/trips">book the first trip</Link>, or use
          Sample data at the top to see how it looks.
        </p>
      </section>
    );
  }

  return (
    <section>
      <header className="page-header">
        <div>
          <h1>Dashboard</h1>
          <p className="page-count">{totalTrips} {totalTrips === 1 ? 'trip' : 'trips'} in total</p>
        </div>
      </header>

      <div className="stat-grid">
        {STATUS_ORDER.map((status) => (
          <div className={`stat stat-${status.toLowerCase()}`} key={status}>
            <span className="stat-label">{STATUS_LABELS[status]}</span>
            <strong className="stat-value tabular">{counts.get(status) ?? 0}</strong>
          </div>
        ))}
        {data.totals.map((row) => (
          <div className="stat" key={row.currency}>
            <span className="stat-label">Booked in {row.currency}</span>
            <strong className="stat-value tabular">{formatPrice(row.total, row.currency)}</strong>
            <span className="stat-note">{row.trips} {row.trips === 1 ? 'trip' : 'trips'}, cancelled not counted</span>
          </div>
        ))}
      </div>

      <div className="dashboard-columns">
        <div className="panel">
          <h2>Next departures</h2>
          {data.upcoming.length === 0 ? (
            <p className="muted">No upcoming trips.</p>
          ) : (
            <ul className="panel-list">
              {data.upcoming.map((trip) => (
                <li key={trip.id}>
                  <div>
                    <strong>{trip.destination.city}</strong>
                    <CountryLabel country={trip.destination.country} />
                  </div>
                  <div className="panel-aside">
                    <span className="tabular">{formatDate(trip.departureDate)}</span>
                    <span className="muted">{trip.passenger.name}</span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="panel">
          <h2>Most visited destinations</h2>
          {data.topDestinations.length === 0 ? (
            <p className="muted">No trips yet.</p>
          ) : (
            <ul className="panel-list">
              {data.topDestinations.map((row) => (
                <li key={`${row.city}-${row.country}`}>
                  <div className="bar-row">
                    <div>
                      <strong>{row.city}</strong>
                      <CountryLabel country={row.country} />
                    </div>
                    <div className="bar" aria-hidden="true">
                      <span style={{ width: `${(row.trips / maxTrips) * 100}%` }} />
                    </div>
                  </div>
                  <span className="tabular">{row.trips} {row.trips === 1 ? 'trip' : 'trips'}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </section>
  );
}
