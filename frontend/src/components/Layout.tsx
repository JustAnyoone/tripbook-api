import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Mark } from './Mark';

export function Layout() {
  const { user, logout } = useAuth();
  return (
    <div className="shell">
      <aside className="rail">
        <div className="brand">
          <Mark />
          <span className="brand-name">TripBook</span>
        </div>
        <nav aria-label="Main">
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
          <span className="account-name" title={user?.email}>
            {user?.name}
          </span>
          <button type="button" className="button button-rail" onClick={logout}>
            Sign out
          </button>
        </div>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
