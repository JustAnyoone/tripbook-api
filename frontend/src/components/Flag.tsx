import '../styles/flags.css';
import { t } from '../i18n';
import { countryName, findCountry, localName } from '../lib/countries';

// A country's flag as an SVG image (emoji flags do not render on Windows). Unknown countries get a globe.
export function Flag({ country }: { country: string | null | undefined }) {
  const match = findCountry(country);
  if (!match) {
    return (
      <svg className="flag flag-unknown" width="20" height="15" viewBox="0 0 20 15" aria-hidden="true">
        <circle cx="10" cy="7.5" r="5.6" fill="none" stroke="currentColor" strokeWidth="1.2" />
        <path d="M4.4 7.5h11.2M10 1.9c-2.2 2.2-2.2 9 0 11.2M10 1.9c2.2 2.2 2.2 9 0 11.2" fill="none" stroke="currentColor" strokeWidth="1.1" />
      </svg>
    );
  }
  return <span className={`flag fi fi-${match.code}`} role="img" aria-label={t('country.flagOf', { name: localName(match) })} />;
}

// Flag followed by the country as it was typed
export function CountryLabel({ country }: { country: string }) {
  return (
    <span className="country-label">
      <Flag country={country} />
      <span>{countryName(country)}</span>
    </span>
  );
}
