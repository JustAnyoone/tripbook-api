import { useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { t } from '../i18n';
import { messageOf } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ErrorBanner } from '../components/ErrorBanner';
import { Field } from '../components/FormShell';
import { Mark } from '../components/Mark';
import { LanguageToggle } from '../components/LanguageToggle';
import { ThemeToggle } from '../components/ThemeToggle';

interface AuthPageProps {
  mode: 'login' | 'register';
}

export function AuthPage({ mode }: AuthPageProps) {
  const { status, sessionExpired, login, register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isRegister = mode === 'register';
  const destination = (location.state as { from?: string } | null)?.from ?? '/dashboard';

  if (status === 'signed-in') return <Navigate to={destination} replace />;

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      if (isRegister) await register({ name, email, password });
      else await login({ email, password });
      navigate(destination, { replace: true });
    } catch (failure) {
      setError(messageOf(failure));
      setBusy(false);
    }
  }

  return (
    <main className="auth">
      <div className="auth-theme">
        <LanguageToggle />
        <ThemeToggle />
      </div>
      <div className="auth-card">
        <div className="auth-strip" aria-hidden="true">
          <Mark />
          <span className="brand-name">TripBook</span>
        </div>
        <div className="auth-body">
          <h1>{isRegister ? t('auth.createTitle') : t('auth.signIn')}</h1>
          <p className="auth-lead">
            {isRegister ? t('auth.createLead') : t('auth.signInLead')}
          </p>

          {sessionExpired && !error && (
            <p className="notice" role="status">
              {t('auth.sessionExpired')}
            </p>
          )}

          <form onSubmit={handleSubmit} className="form-fields">
            {isRegister && (
              <Field label={t('auth.name')}>
                {(id) => (
                  <input id={id} value={name} onChange={(e) => setName(e.target.value)} autoComplete="name" required />
                )}
              </Field>
            )}
            <Field label={t('auth.email')}>
              {(id) => (
                <input
                  id={id}
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  autoComplete="email"
                  required
                />
              )}
            </Field>
            <Field label={t('auth.password')} hint={isRegister ? t('auth.passwordHint') : undefined}>
              {(id) => (
                <input
                  id={id}
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete={isRegister ? 'new-password' : 'current-password'}
                  minLength={isRegister ? 8 : undefined}
                  maxLength={72}
                  required
                />
              )}
            </Field>
            {error && <ErrorBanner message={error} />}
            <button type="submit" className="button button-primary auth-submit" disabled={busy}>
              {isRegister ? t('auth.createAccount') : t('auth.signIn')}
            </button>
          </form>

          <p className="auth-switch">
            {isRegister ? (
              <>
                {t('auth.haveAccount')} <Link to="/login" state={location.state}>{t('auth.signIn')}</Link>
              </>
            ) : (
              <>
                {t('auth.newHere')} <Link to="/register" state={location.state}>{t('auth.createLink')}</Link>
              </>
            )}
          </p>
        </div>
        <span className="auth-barcode" aria-hidden="true" />
      </div>
    </main>
  );
}
