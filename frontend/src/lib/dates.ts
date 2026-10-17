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
