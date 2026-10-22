import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { t } from '../i18n';
import { useAuth } from './AuthContext';

// Wraps the private routes: signed-out visitors go to the login page and come back afterwards.
export function RequireAuth() {
  const { status } = useAuth();
  const location = useLocation();

  if (status === 'checking') return <p className="auth-checking">{t('common.checkingSession')}</p>;
  if (status === 'signed-out') return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return <Outlet />;
}
