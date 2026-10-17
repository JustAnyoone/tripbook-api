import { describe, expect, it } from 'vitest';
import { formatDate, formatNights, formatPrice, nightsBetween } from './dates';

describe('dates', () => {
  it('formats the day that was sent, whatever the time zone', () => {
    expect(formatDate('2026-12-20')).toBe('20 Dec 2026');
    expect(formatDate('2026-01-01')).toBe('1 Jan 2026');
  });

  it('shows nothing for a missing date', () => {
    expect(formatDate(null)).toBe('');
  });

  it('counts the nights between departure and return', () => {
    expect(nightsBetween('2026-12-20', '2026-12-30')).toBe(10);
    expect(nightsBetween('2026-12-30', '2027-01-02')).toBe(3);
  });

  it('has no nights without a return date', () => {
    expect(nightsBetween('2026-12-20', null)).toBeNull();
  });

  it('uses the singular for one night', () => {
    expect(formatNights(1)).toBe('1 night');
    expect(formatNights(3)).toBe('3 nights');
  });

  it('formats prices with two decimals', () => {
    expect(formatPrice(3500.5)).toBe('3,500.50');
  });
});
