import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { DashboardIcon, DestinationsIcon, PassengersIcon, TripsIcon } from './icons';
import { RefreshProvider } from './RefreshContext';
import { SampleDataButton } from './SampleDataButton';
import { Mark } from './Mark';
import { ThemeToggle } from './ThemeToggle';

export function Layout() {
  const { user, logout } = useAuth();
  return (
    <RefreshProvider>
    <div className="shell">
      <header className="topbar">
        <div className="brand">
          <Mark />
          <span className="brand-name">TripBook</span>
        </div>
        <nav aria-label="Main" className="topnav">
          <ul className="nav">
            <li>
              <NavLink to="/dashboard">
                <DashboardIcon />
                Dashboard
              </NavLink>
            </li>
            <li>
              <NavLink to="/trips">
                <TripsIcon />
                Trips
              </NavLink>
            </li>
            <li>
              <NavLink to="/passengers">
                <PassengersIcon />
                Passengers
              </NavLink>
            </li>
            <li>
              <NavLink to="/destinations">
                <DestinationsIcon />
                Destinations
              </NavLink>
            </li>
          </ul>
        </nav>
        <div className="account">
          <SampleDataButton />
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
    </RefreshProvider>
  );
}
