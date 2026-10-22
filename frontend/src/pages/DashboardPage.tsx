import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { dashboardApi, messageOf } from '../api/client';
import type { Dashboard, TripStatus } from '../api/types';
import { CountryLabel } from '../components/Flag';
import { ErrorBanner } from '../components/ErrorBanner';
import { useRefresh } from '../components/RefreshContext';
import { plural, t } from '../i18n';
import { formatDate, formatPrice } from '../lib/dates';

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
  if (!data) {
    return (
      <section aria-busy="true">
        <header className="page-header">
          <h1>{t('dashboard.title')}</h1>
        </header>
        <p className="visually-hidden" role="status">{t('common.loading')}</p>
        <div className="stat-grid" aria-hidden="true">
          {[0, 1, 2, 3].map((index) => (
            <div className="stat skeleton skeleton-stat" key={index} />
          ))}
        </div>
        <div className="dashboard-columns" aria-hidden="true">
          <div className="panel skeleton skeleton-panel" />
          <div className="panel skeleton skeleton-panel" />
        </div>
      </section>
    );
  }

  const counts = new Map(data.statusCounts.map((row) => [row.status, row.trips]));
  const totalTrips = data.statusCounts.reduce((sum, row) => sum + row.trips, 0);
  const maxTrips = Math.max(...data.topDestinations.map((row) => row.trips), 1);

  if (totalTrips === 0) {
    return (
      <section>
        <header className="page-header">
          <h1>{t('dashboard.title')}</h1>
        </header>
        <p className="empty-note">
          {t('dashboard.emptyBefore')}
          <Link to="/trips">{t('dashboard.emptyLink')}</Link>
          {t('dashboard.emptyAfter')}
        </p>
      </section>
    );
  }

  return (
    <section>
      <header className="page-header">
        <div>
          <h1>{t('dashboard.title')}</h1>
          <p className="page-count">{plural('dashboard.total', totalTrips)}</p>
        </div>
      </header>

      <div className="stat-grid">
        {STATUS_ORDER.map((status) => (
          <div className={`stat stat-${status.toLowerCase()}`} key={status}>
            <span className="stat-label">{t(`status.${status}`)}</span>
            <strong className="stat-value tabular">{counts.get(status) ?? 0}</strong>
          </div>
        ))}
        {data.totals.map((row) => (
          <div className="stat" key={row.currency}>
            <span className="stat-label">{t('dashboard.bookedIn', { currency: row.currency })}</span>
            <strong className="stat-value tabular">{formatPrice(row.total, row.currency)}</strong>
            <span className="stat-note">{plural('dashboard.bookedNote', row.trips)}</span>
          </div>
        ))}
      </div>

      <div className="dashboard-columns">
        <div className="panel">
          <h2>{t('dashboard.next')}</h2>
          {data.upcoming.length === 0 ? (
            <p className="muted">{t('dashboard.noUpcoming')}</p>
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
          <h2>{t('dashboard.top')}</h2>
          {data.topDestinations.length === 0 ? (
            <p className="muted">{t('dashboard.noTrips')}</p>
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
                  <span className="tabular">{plural('dashboard.trips', row.trips)}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </section>
  );
}
