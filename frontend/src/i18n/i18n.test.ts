import { afterEach, describe, expect, it } from 'vitest';
import { en } from './en';
import { getLang, plural, setLang, t } from './index';
import { pt } from './pt';
import { translateApiMessage } from './apiMessages';

afterEach(() => {
  setLang('en');
  localStorage.clear();
});

describe('i18n', () => {
  it('has a Portuguese text for every English one, with the same placeholders', () => {
    const placeholders = (text: string) => (text.match(/\{\w+\}/g) ?? []).sort().join(',');
    for (const key of Object.keys(en) as (keyof typeof en)[]) {
      expect(pt[key], key).toBeTruthy();
      expect(placeholders(pt[key]), key).toBe(placeholders(en[key]));
    }
  });

  it('switches the language, remembers it and marks the page language', () => {
    expect(t('nav.trips')).toBe('Trips');

    setLang('pt');

    expect(getLang()).toBe('pt');
    expect(t('nav.trips')).toBe('Viagens');
    expect(localStorage.getItem('tripbook.lang')).toBe('pt');
    expect(document.documentElement.lang).toBe('pt-BR');
  });

  it('fills placeholders and picks singular or plural', () => {
    expect(plural('trips.count', 1)).toBe('1 trip');
    expect(plural('trips.count', 3)).toBe('3 trips');
    setLang('pt');
    expect(plural('trips.nights', 1)).toBe('1 noite');
    expect(plural('trips.nights', 2)).toBe('2 noites');
    expect(t('pagination.range', { first: 1, last: 10, total: 42 })).toBe('1–10 de 42');
  });
});

describe('translateApiMessage', () => {
  it('translates the messages the API sends, including ones with values', () => {
    expect(translateApiMessage('Invalid email or password', 'pt')).toBe('E-mail ou senha inválidos');
    expect(translateApiMessage('Trip not found: 7', 'pt')).toContain('Viagem');
    expect(translateApiMessage('Destination already exists: Lisbon, Portugal', 'pt')).toBe('Este destino já existe: Lisbon, Portugal');
  });

  it('translates validation messages that arrive joined together', () => {
    expect(translateApiMessage('name is required, email is required', 'pt')).toBe('O nome é obrigatório, o e-mail é obrigatório');
  });

  it('keeps unknown messages and English as they are', () => {
    expect(translateApiMessage('Something new', 'pt')).toBe('Something new');
    expect(translateApiMessage('Invalid email or password', 'en')).toBe('Invalid email or password');
  });
});
