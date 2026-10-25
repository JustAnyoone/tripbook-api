import { getLang } from '../i18n';
import { COUNTRY_CODES } from './countryCodes';

export interface Country {
  /** ISO 3166-1 alpha-2, lower case: "br" */
  code: string;
  /** English name, the value stored in the destination: "Brazil" */
  name: string;
  /** Everything the search box matches against: English and Portuguese names, normalised */
  search: string;
}

// Plain text so that "Brasil", "brazil" and "BRAZIL " all find the same country, with or without accents
export function normalize(text: string): string {
  return text
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/\s+/g, ' ')
    .trim();
}

// Names people type that Intl does not use
const ALIASES: Record<string, string> = {
  usa: 'us',
  'united states of america': 'us',
  eua: 'us',
  uk: 'gb',
  'great britain': 'gb',
  england: 'gb',
  'reino unido': 'gb',
  holland: 'nl',
  russia: 'ru',
  turkey: 'tr',
  'czech republic': 'cz',
  'ivory coast': 'ci',
};

const english = new Intl.DisplayNames(['en'], { type: 'region' });
const portuguese = new Intl.DisplayNames(['pt-BR'], { type: 'region' });

export const COUNTRIES: Country[] = COUNTRY_CODES.map((code) => {
  const name = english.of(code.toUpperCase()) ?? code.toUpperCase();
  const local = portuguese.of(code.toUpperCase()) ?? name;
  return { code, name, search: `${normalize(name)}|${normalize(local)}` };
}).sort((a, b) => a.name.localeCompare(b.name, 'en'));

const BY_NAME = new Map<string, Country>();
for (const country of COUNTRIES) {
  for (const part of country.search.split('|')) BY_NAME.set(part, country);
}
for (const [alias, code] of Object.entries(ALIASES)) {
  const country = COUNTRIES.find((candidate) => candidate.code === code);
  if (country) BY_NAME.set(alias, country);
}

// The country a piece of text names, or undefined when it is not one we know
export function findCountry(text: string | null | undefined): Country | undefined {
  return text ? BY_NAME.get(normalize(text)) : undefined;
}

const displayNames = { en: english, pt: portuguese };

// The country's name in the language of the interface. The stored value stays the English name.
export function localName(country: Country): string {
  return displayNames[getLang()].of(country.code.toUpperCase()) ?? country.name;
}

// The name to show for stored text: the translated one when it is a known country, the text otherwise
export function countryName(text: string): string {
  const match = findCountry(text);
  return match ? localName(match) : text;
}

// Approximate center coordinates for a country given its English name (as stored in Destination).
// Returns null for unknown countries.
const COUNTRY_CENTERS: Record<string, { lat: number; lng: number }> = {
  'Afghanistan': { lat: 33.9, lng: 67.7 }, 'Albania': { lat: 41.2, lng: 20.2 },
  'Algeria': { lat: 28.0, lng: 1.7 }, 'Argentina': { lat: -38.4, lng: -63.6 },
  'Australia': { lat: -25.3, lng: 133.8 }, 'Austria': { lat: 47.5, lng: 14.6 },
  'Belgium': { lat: 50.5, lng: 4.5 }, 'Bolivia': { lat: -16.3, lng: -63.6 },
  'Brazil': { lat: -14.2, lng: -51.9 }, 'Canada': { lat: 56.1, lng: -106.3 },
  'Chile': { lat: -35.7, lng: -71.5 }, 'China': { lat: 35.9, lng: 104.2 },
  'Colombia': { lat: 4.6, lng: -74.3 }, 'Croatia': { lat: 45.1, lng: 15.2 },
  'Cuba': { lat: 21.5, lng: -79.0 }, 'Czech Republic': { lat: 49.8, lng: 15.5 },
  'Denmark': { lat: 56.3, lng: 9.5 }, 'Ecuador': { lat: -1.8, lng: -78.2 },
  'Egypt': { lat: 26.8, lng: 30.8 }, 'Ethiopia': { lat: 9.1, lng: 40.5 },
  'Finland': { lat: 61.9, lng: 25.7 }, 'France': { lat: 46.2, lng: 2.2 },
  'Germany': { lat: 51.2, lng: 10.5 }, 'Ghana': { lat: 7.9, lng: -1.0 },
  'Greece': { lat: 39.1, lng: 21.8 }, 'Hungary': { lat: 47.2, lng: 19.5 },
  'India': { lat: 20.6, lng: 79.1 }, 'Indonesia': { lat: -0.8, lng: 113.9 },
  'Iran': { lat: 32.4, lng: 53.7 }, 'Iraq': { lat: 33.2, lng: 43.7 },
  'Ireland': { lat: 53.4, lng: -8.2 }, 'Israel': { lat: 31.0, lng: 34.9 },
  'Italy': { lat: 41.9, lng: 12.6 }, 'Japan': { lat: 36.2, lng: 138.3 },
  'Jordan': { lat: 30.6, lng: 36.2 }, 'Kenya': { lat: -0.0, lng: 37.9 },
  'South Korea': { lat: 35.9, lng: 127.8 }, 'Mexico': { lat: 23.6, lng: -102.6 },
  'Morocco': { lat: 31.8, lng: -7.1 }, 'Netherlands': { lat: 52.1, lng: 5.3 },
  'New Zealand': { lat: -40.9, lng: 174.9 }, 'Nigeria': { lat: 9.1, lng: 8.7 },
  'Norway': { lat: 60.5, lng: 8.5 }, 'Pakistan': { lat: 30.4, lng: 69.3 },
  'Panama': { lat: 8.5, lng: -80.8 }, 'Paraguay': { lat: -23.4, lng: -58.4 },
  'Peru': { lat: -9.2, lng: -75.0 }, 'Philippines': { lat: 12.9, lng: 121.8 },
  'Poland': { lat: 51.9, lng: 19.1 }, 'Portugal': { lat: 39.4, lng: -8.2 },
  'Romania': { lat: 45.9, lng: 24.97 }, 'Russia': { lat: 61.5, lng: 105.3 },
  'Saudi Arabia': { lat: 23.9, lng: 45.1 }, 'South Africa': { lat: -30.6, lng: 22.9 },
  'Spain': { lat: 40.5, lng: -3.7 }, 'Sweden': { lat: 60.1, lng: 18.6 },
  'Switzerland': { lat: 46.8, lng: 8.2 }, 'Thailand': { lat: 15.9, lng: 100.9 },
  'Turkey': { lat: 38.9, lng: 35.2 }, 'Ukraine': { lat: 48.4, lng: 31.2 },
  'United Arab Emirates': { lat: 24.0, lng: 54.0 }, 'United Kingdom': { lat: 55.4, lng: -3.4 },
  'United States': { lat: 37.1, lng: -95.7 }, 'Uruguay': { lat: -32.5, lng: -55.8 },
  'Venezuela': { lat: 6.4, lng: -66.6 }, 'Vietnam': { lat: 14.1, lng: 108.3 },
};

export function getCountryCoords(countryName: string): { lat: number; lng: number } | null {
  return COUNTRY_CENTERS[countryName] ?? null;
}

// Countries whose name starts with, or contains, the query. Names that start with it come first.
export function searchCountries(query: string): Country[] {
  const needle = normalize(query);
  // The list reads alphabetically in the language of the interface
  const byLocalName = (a: Country, b: Country) => localName(a).localeCompare(localName(b), getLang() === 'pt' ? 'pt-BR' : 'en');
  if (!needle) return [...COUNTRIES].sort(byLocalName);
  const starts: Country[] = [];
  const contains: Country[] = [];
  for (const country of COUNTRIES) {
    const names = country.search.split('|');
    if (names.some((name) => name.startsWith(needle))) starts.push(country);
    else if (names.some((name) => name.includes(needle))) contains.push(country);
  }
  return [...starts.sort(byLocalName), ...contains.sort(byLocalName)];
}
