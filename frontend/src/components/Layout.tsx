import { NavLink, Outlet } from 'react-router-dom';

function Mark() {
  // Two stops joined by a dashed route
  return (
    <svg className="brand-mark" width="34" height="14" viewBox="0 0 34 14" aria-hidden="true">
      <circle cx="4" cy="7" r="3.2" fill="currentColor" />
      <line x1="9" y1="7" x2="25" y2="7" stroke="currentColor" strokeWidth="1.6" strokeDasharray="3 3" />
      <circle cx="30" cy="7" r="3.2" fill="none" stroke="currentColor" strokeWidth="1.8" />
    </svg>
  );
}

export function Layout() {
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
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
