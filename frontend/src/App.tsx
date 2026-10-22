import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { t, useLang } from './i18n';
import { RequireAuth } from './auth/RequireAuth';
import { Layout } from './components/Layout';
import { AuthPage } from './pages/AuthPage';

// Each page is its own chunk, so the first screen downloads only the code it needs
const DashboardPage = lazy(() => import('./pages/DashboardPage').then((m) => ({ default: m.DashboardPage })));
const TripsPage = lazy(() => import('./pages/TripsPage').then((m) => ({ default: m.TripsPage })));
const PassengersPage = lazy(() => import('./pages/PassengersPage').then((m) => ({ default: m.PassengersPage })));
const DestinationsPage = lazy(() => import('./pages/DestinationsPage').then((m) => ({ default: m.DestinationsPage })));

export function App() {
  // Every text is read when it renders. Keying the routes on the language makes the whole screen render again
  // when it changes (an open form starts over, which is acceptable for a switch that is rarely used).
  const lang = useLang();
  return (
    <Suspense fallback={<p className="page-loading" role="status">{t('common.loading')}</p>}>
    <Routes key={lang}>
      <Route path="login" element={<AuthPage mode="login" />} />
      <Route path="register" element={<AuthPage mode="register" />} />
      <Route element={<RequireAuth />}>
        <Route element={<Layout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="trips" element={<TripsPage />} />
          <Route path="passengers" element={<PassengersPage />} />
          <Route path="destinations" element={<DestinationsPage />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Route>
      </Route>
    </Routes>
    </Suspense>
  );
}
