import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { t, useLang } from '../i18n';
import { AccountAvatar } from './AccountAvatar';
import { LanguageToggle } from './LanguageToggle';
import { DashboardIcon, DestinationsIcon, MapIcon, PassengersIcon, TripsIcon } from './icons';
import { RefreshProvider } from './RefreshContext';
import { SampleDataButton } from './SampleDataButton';
import { Mark } from './Mark';
import { ThemeToggle } from './ThemeToggle';

export function Layout() {
  useLang();
  const { user, logout } = useAuth();
  return (
    <RefreshProvider>
    <div className="shell">
      <header className="topbar">
        <div className="brand">
          <Mark />
          <span className="brand-name">TripBook</span>
        </div>
        <nav aria-label={t('nav.main')} className="topnav">
          <ul className="nav">
            <li>
              <NavLink to="/dashboard">
                <DashboardIcon />
                {t('nav.dashboard')}
              </NavLink>
            </li>
            <li>
              <NavLink to="/trips">
                <TripsIcon />
                {t('nav.trips')}
              </NavLink>
            </li>
            <li>
              <NavLink to="/passengers">
                <PassengersIcon />
                {t('nav.passengers')}
              </NavLink>
            </li>
            <li>
              <NavLink to="/destinations">
                <DestinationsIcon />
                {t('nav.destinations')}
              </NavLink>
            </li>
            <li>
              <NavLink to="/map">
                <MapIcon />
                {t('nav.map')}
              </NavLink>
            </li>
          </ul>
        </nav>
        <div className="account">
          <SampleDataButton />
          <LanguageToggle />
          <ThemeToggle />
          <AccountAvatar />
          <span className="account-name" title={user?.email}>
            {user?.name}
          </span>
          <button type="button" className="button button-outline" onClick={logout}>
            {t('common.signOut')}
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
