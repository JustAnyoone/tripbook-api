import { describe, expect, it } from 'vitest';
import { addDays, addMonths, formatDate, formatNights, formatPrice, isRealDate, nightsBetween, todayIso, validateTripDates } from './dates';

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

  it('writes each currency the way its own country does', () => {
    // Intl separates the symbol with a non-breaking space; normalise it so the expectations read plainly
    const plain = (text: string) => text.replace(/\u00a0/g, ' ');
    expect(plain(formatPrice(3500.5, 'BRL'))).toBe('R$ 3.500,50');
    expect(plain(formatPrice(3500.5, 'USD'))).toBe('$3,500.50');
    expect(plain(formatPrice(3500.5, 'EUR'))).toBe('3.500,50 €');
    expect(plain(formatPrice(3500.5))).toBe('R$ 3.500,50');
  });
});

describe('date rules', () => {
  const today = '2026-10-17';

  it('formats today in the local time zone', () => {
    expect(todayIso(new Date(2026, 9, 17, 23, 59))).toBe('2026-10-17');
    expect(todayIso(new Date(2026, 0, 5, 0, 1))).toBe('2026-01-05');
  });

  it('recognises dates that do not exist', () => {
    expect(isRealDate('2026-11-30')).toBe(true);
    expect(isRealDate('2028-02-29')).toBe(true);
    expect(isRealDate('2026-11-31')).toBe(false);
    expect(isRealDate('2026-02-30')).toBe(false);
    expect(isRealDate('2027-02-29')).toBe(false);
    expect(isRealDate('2026-13-01')).toBe(false);
    expect(isRealDate('')).toBe(false);
    expect(isRealDate('20/10/2026')).toBe(false);
  });

  it('accepts today and later dates', () => {
    expect(validateTripDates({ departureDate: today, returnDate: today }, today)).toBeNull();
    expect(validateTripDates({ departureDate: '2026-12-20', returnDate: null }, today)).toBeNull();
  });

  it('rejects a departure that does not exist, or is in the past', () => {
    expect(validateTripDates({ departureDate: '2026-11-31', returnDate: null }, today)).toBe('Departure date is not a valid date.');
    expect(validateTripDates({ departureDate: '2026-10-16', returnDate: null }, today)).toBe('Departure date cannot be in the past.');
  });

  it('rejects a return that does not exist, is in the past, or is before the departure', () => {
    expect(validateTripDates({ departureDate: '2026-12-20', returnDate: '2026-02-30' }, today)).toBe('Return date is not a valid date.');
    expect(validateTripDates({ departureDate: '2026-12-20', returnDate: '2026-10-16' }, today)).toBe('Return date cannot be in the past.');
    expect(validateTripDates({ departureDate: '2026-12-20', returnDate: '2026-12-10' }, today)).toBe(
      'Return date cannot be before departure date.',
    );
  });

  it('lets an existing trip keep dates that have passed, but not move to a past date', () => {
    const initial = { departureDate: '2026-09-01', returnDate: '2026-09-10' };
    expect(validateTripDates(initial, today, initial)).toBeNull();
    expect(validateTripDates({ departureDate: '2026-09-02', returnDate: '2026-09-10' }, today, initial)).toBe(
      'Departure date cannot be in the past.',
    );
  });

  it('adds days across month and year boundaries', () => {
    expect(addDays('2026-10-31', 1)).toBe('2026-11-01');
    expect(addDays('2026-12-31', 1)).toBe('2027-01-01');
    expect(addDays('2026-03-01', -1)).toBe('2026-02-28');
  });

  it('adds months and keeps the day inside the shorter month', () => {
    expect(addMonths('2026-01-31', 1)).toBe('2026-02-28');
    expect(addMonths('2026-12-15', 1)).toBe('2027-01-15');
    expect(addMonths('2026-03-31', -1)).toBe('2026-02-28');
  });
});
