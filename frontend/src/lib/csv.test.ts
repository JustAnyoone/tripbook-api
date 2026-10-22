import { describe, expect, it } from 'vitest';
import type { Trip } from '../api/types';
import { csvField, tripsToCsv } from './csv';

describe('csv', () => {
  it('quotes fields that contain commas, quotes or line breaks', () => {
    expect(csvField('plain')).toBe('plain');
    expect(csvField('Rio, Brazil')).toBe('"Rio, Brazil"');
    expect(csvField('say "hi"')).toBe('"say ""hi"""');
    expect(csvField(null)).toBe('');
  });

  it('defuses text that a spreadsheet would run as a formula', () => {
    expect(csvField('=SUM(A1)')).toBe("'=SUM(A1)");
    expect(csvField(-5)).toBe('-5');
  });

  it('writes a header and one row per trip', () => {
    const trip = {
      id: 1,
      departureDate: '2026-12-20',
      returnDate: null,
      price: 3500.5,
      currency: 'EUR',
      status: 'CONFIRMED',
      passenger: { id: 1, name: 'Ana, Souza', documentNumber: 'AB123456', email: 'a@x.com', birthDate: null },
      destination: { id: 1, city: 'Lisbon', country: 'Portugal', description: null },
    } as Trip;

    const lines = tripsToCsv([trip]).replace('﻿', '').trim().split('\r\n');

    expect(lines[0]).toBe('Passenger,Document,City,Country,Departure,Return,Price,Currency,Status');
    expect(lines[1]).toBe('"Ana, Souza",AB123456,Lisbon,Portugal,2026-12-20,,3500.50,EUR,CONFIRMED');
  });
});
