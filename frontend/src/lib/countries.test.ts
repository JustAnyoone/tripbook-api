import { describe, expect, it } from 'vitest';
import { COUNTRIES, findCountry, searchCountries } from './countries';

describe('countries', () => {
  it('knows the country by English or Portuguese name, ignoring case and accents', () => {
    expect(findCountry('Brazil')?.code).toBe('br');
    expect(findCountry(' brasil ')?.code).toBe('br');
    expect(findCountry('JAPAO')?.code).toBe('jp');
    expect(findCountry('USA')?.code).toBe('us');
  });

  it('returns nothing for unknown text', () => {
    expect(findCountry('Atlantis')).toBeUndefined();
    expect(findCountry('')).toBeUndefined();
    expect(findCountry(null)).toBeUndefined();
  });

  it('lists names that start with the query before names that only contain it', () => {
    const names = searchCountries('bra').map((country) => country.name);
    expect(names[0]).toBe('Brazil');
  });

  it('offers every country when the query is empty', () => {
    expect(searchCountries('')).toHaveLength(COUNTRIES.length);
    expect(searchCountries('zzzz')).toEqual([]);
  });
});
