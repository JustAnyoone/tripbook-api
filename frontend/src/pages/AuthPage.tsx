import { useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { messageOf } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ErrorBanner } from '../components/ErrorBanner';
import { Field } from '../components/FormShell';
import { Mark } from '../components/Mark';
import { ThemeToggle } from '../components/ThemeToggle';

interface AuthPageProps {
  mode: 'login' | 'register';
}

export function AuthPage({ mode }: AuthPageProps) {
  const { status, login, register } = useAuth();
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
        <ThemeToggle />
      </div>
      <div className="auth-card">
        <div className="auth-strip" aria-hidden="true">
          <Mark />
          <span className="brand-name">TripBook</span>
        </div>
        <div className="auth-body">
          <h1>{isRegister ? 'Create your account' : 'Sign in'}</h1>
          <p className="auth-lead">
            {isRegister ? 'One account to manage passengers, destinations and trips.' : 'Welcome back. Your trips are waiting.'}
          </p>

          <form onSubmit={handleSubmit} className="form-fields">
            {isRegister && (
              <Field label="Name">
                {(id) => (
                  <input id={id} value={name} onChange={(e) => setName(e.target.value)} autoComplete="name" required />
                )}
              </Field>
            )}
            <Field label="Email">
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
            <Field label="Password" hint={isRegister ? '8 to 72 characters.' : undefined}>
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
              {isRegister ? 'Create account' : 'Sign in'}
            </button>
          </form>

          <p className="auth-switch">
            {isRegister ? (
              <>
                Already have an account? <Link to="/login" state={location.state}>Sign in</Link>
              </>
            ) : (
              <>
                New here? <Link to="/register" state={location.state}>Create an account</Link>
              </>
            )}
          </p>
        </div>
        <span className="auth-barcode" aria-hidden="true" />
      </div>
    </main>
  );
}
