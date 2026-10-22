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
