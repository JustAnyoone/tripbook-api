// The API sends dates as "yyyy-MM-dd". They are parsed by hand: new Date("2026-12-20")
// is read as UTC midnight and can show up as the previous day in time zones behind UTC.

export function parse(iso: string): Date {
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day);
}

// A local Date as "yyyy-MM-dd"
export function toIso(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

// Calendar arithmetic on ISO dates; Date handles the month and year rollovers
export function addDays(iso: string, days: number): string {
  const date = parse(iso);
  date.setDate(date.getDate() + days);
  return toIso(date);
}

// Moves by whole months and keeps the day inside the target month (31 Jan + 1 month = 28 Feb)
export function addMonths(iso: string, months: number): string {
  const date = parse(iso);
  const day = date.getDate();
  date.setDate(1);
  date.setMonth(date.getMonth() + months);
  const lastDay = new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
  date.setDate(Math.min(day, lastDay));
  return toIso(date);
}

const formatter = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });

export function formatDate(iso: string | null): string {
  return iso ? formatter.format(parse(iso)) : '';
}

// Whole nights between two dates, or null when there is no return date
export function nightsBetween(departure: string, returnDate: string | null): number | null {
  if (!returnDate) return null;
  const millisPerDay = 24 * 60 * 60 * 1000;
  return Math.round((parse(returnDate).getTime() - parse(departure).getTime()) / millisPerDay);
}

export function formatNights(nights: number): string {
  return nights === 1 ? '1 night' : `${nights} nights`;
}

// Each currency is written the way its own country writes it: R$ 3.500,50, $3,500.50, 3.500,50 €
const PRICE_LOCALES: Record<string, string> = { BRL: 'pt-BR', USD: 'en-US', EUR: 'de-DE' };

export function formatPrice(value: number, currency: string = 'BRL'): string {
  return new Intl.NumberFormat(PRICE_LOCALES[currency] ?? 'en-US', { style: 'currency', currency }).format(value);
}

// Today as "yyyy-MM-dd" in the user's own time zone (toISOString would give the UTC day)
export function todayIso(now: Date = new Date()): string {
  return toIso(now);
}

// True only for dates that exist: 2026-11-31 and 2027-02-29 are rejected
export function isRealDate(iso: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(iso)) return false;
  const [year, month, day] = iso.split('-').map(Number);
  const date = new Date(year, month - 1, day);
  return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day;
}

interface TripDates {
  departureDate: string;
  returnDate: string | null;
}

// Mirrors the API rules, so the user gets the message before the request is sent.
// When editing, a date left unchanged is accepted even if it has passed since the trip was created.
export function validateTripDates(dates: TripDates, today: string, initial?: TripDates): string | null {
  const { departureDate, returnDate } = dates;
  if (!isRealDate(departureDate)) return 'Departure date is not a valid date.';
  if (returnDate && !isRealDate(returnDate)) return 'Return date is not a valid date.';
  if (departureDate < today && departureDate !== initial?.departureDate) {
    return 'Departure date cannot be in the past.';
  }
  if (returnDate && returnDate < today && returnDate !== initial?.returnDate) {
    return 'Return date cannot be in the past.';
  }
  if (returnDate && returnDate < departureDate) return 'Return date cannot be before departure date.';
  return null;
}
