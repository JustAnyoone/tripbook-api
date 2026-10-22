import type { Trip } from '../api/types';
import { t } from '../i18n';

// RFC 4180: a field with a comma, quote or line break is wrapped in quotes and its quotes are doubled.
// A leading = + - @ is prefixed with an apostrophe so a spreadsheet does not run it as a formula.
export function csvField(value: string | number | null | undefined): string {
  let text = value === null || value === undefined ? '' : String(value);
  if (/^[=+\-@]/.test(text) && typeof value === 'string') text = `'${text}`;
  return /[",\r\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

const header = () => [
  t('csv.passenger'),
  t('csv.document'),
  t('csv.city'),
  t('csv.country'),
  t('csv.departure'),
  t('csv.return'),
  t('csv.price'),
  t('csv.currency'),
  t('csv.status'),
];

export function tripsToCsv(trips: Trip[]): string {
  const rows = trips.map((trip) => [
    trip.passenger.name,
    trip.passenger.documentNumber,
    trip.destination.city,
    trip.destination.country,
    trip.departureDate,
    trip.returnDate,
    trip.price.toFixed(2),
    trip.currency,
    trip.status,
  ]);
  // The byte order mark makes Excel read the file as UTF-8, so accents survive
  return '﻿' + [header(), ...rows].map((row) => row.map(csvField).join(',')).join('\r\n') + '\r\n';
}

export function downloadFile(name: string, content: string, type: string) {
  const url = URL.createObjectURL(new Blob([content], { type }));
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
