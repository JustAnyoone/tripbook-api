import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Mark } from './Mark';
import { ThemeToggle } from './ThemeToggle';

export function Layout() {
  const { user, logout } = useAuth();
  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand">
          <Mark />
          <span className="brand-name">TripBook</span>
        </div>
        <nav aria-label="Main" className="topnav">
          <ul className="nav">
            <li>
              <NavLink to="/trips">Trips</NavLink>
            </li>
            <li>
              <NavLink to="/passengers">Passengers</NavLink>
            </li>
            <li>
              <NavLink to="/destinations">Destinations</NavLink>
            </li>
          </ul>
        </nav>
        <div className="account">
          <ThemeToggle />
          <span className="account-name" title={user?.email}>
            {user?.name}
          </span>
          <button type="button" className="button button-outline" onClick={logout}>
            Sign out
          </button>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
