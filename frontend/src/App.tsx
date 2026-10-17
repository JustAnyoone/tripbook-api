import { Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { DestinationsPage } from './pages/DestinationsPage';
import { PassengersPage } from './pages/PassengersPage';
import { TripsPage } from './pages/TripsPage';

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/trips" replace />} />
        <Route path="trips" element={<TripsPage />} />
        <Route path="passengers" element={<PassengersPage />} />
        <Route path="destinations" element={<DestinationsPage />} />
        <Route path="*" element={<Navigate to="/trips" replace />} />
      </Route>
    </Routes>
  );
}
