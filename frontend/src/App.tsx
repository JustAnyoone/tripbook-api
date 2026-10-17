import { Navigate, Route, Routes } from 'react-router-dom';
import { RequireAuth } from './auth/RequireAuth';
import { Layout } from './components/Layout';
import { AuthPage } from './pages/AuthPage';
import { DestinationsPage } from './pages/DestinationsPage';
import { PassengersPage } from './pages/PassengersPage';
import { TripsPage } from './pages/TripsPage';

export function App() {
  return (
    <Routes>
      <Route path="login" element={<AuthPage mode="login" />} />
      <Route path="register" element={<AuthPage mode="register" />} />
      <Route element={<RequireAuth />}>
        <Route element={<Layout />}>
          <Route index element={<Navigate to="/trips" replace />} />
          <Route path="trips" element={<TripsPage />} />
          <Route path="passengers" element={<PassengersPage />} />
          <Route path="destinations" element={<DestinationsPage />} />
          <Route path="*" element={<Navigate to="/trips" replace />} />
        </Route>
      </Route>
    </Routes>
  );
}
