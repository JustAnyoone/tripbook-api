// The API sends dates as "yyyy-MM-dd". They are parsed by hand: new Date("2026-12-20")
// is read as UTC midnight and can show up as the previous day in time zones behind UTC.

function parse(iso: string): Date {
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day);
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

const priceFormatter = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export function formatPrice(value: number): string {
  return priceFormatter.format(value);
}

// Today as "yyyy-MM-dd" in the user's own time zone (toISOString would give the UTC day)
export function todayIso(now: Date = new Date()): string {
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
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
