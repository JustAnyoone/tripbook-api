import { useSyncExternalStore } from 'react';
import { en, type MessageKey } from './en';
import { pt } from './pt';

export type Lang = 'en' | 'pt';
export type { MessageKey };

const STORAGE_KEY = 'tripbook.lang';

// The saved choice wins; otherwise Portuguese browsers get Portuguese and everyone else English
function detect(): Lang {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === 'en' || saved === 'pt') return saved;
  } catch {
    // Storage blocked: fall through to the browser language
  }
  return typeof navigator !== 'undefined' && navigator.language?.toLowerCase().startsWith('pt') ? 'pt' : 'en';
}

let current: Lang = detect();
const listeners = new Set<() => void>();

function applyToDocument(lang: Lang) {
  if (typeof document !== 'undefined') document.documentElement.lang = lang === 'pt' ? 'pt-BR' : 'en';
}
applyToDocument(current);

export function getLang(): Lang {
  return current;
}

export function setLang(lang: Lang) {
  if (lang === current) return;
  current = lang;
  try {
    localStorage.setItem(STORAGE_KEY, lang);
  } catch {
    // The choice then lasts until the page is closed
  }
  applyToDocument(lang);
  listeners.forEach((listener) => listener());
}

// Components call this so they render again when the language changes
export function useLang(): Lang {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    getLang,
    getLang,
  );
}

// BCP 47 tag for Intl (dates, month names, country names)
export function locale(): string {
  return current === 'pt' ? 'pt-BR' : 'en-GB';
}

export function t(key: MessageKey, params?: Record<string, string | number>): string {
  const text: string = (current === 'pt' ? pt : en)[key] ?? en[key];
  return params ? text.replace(/\{(\w+)\}/g, (_, name: string) => String(params[name] ?? '')) : text;
}

type PluralBase<K> = K extends `${infer Base}.one` ? Base : never;

// Picks the singular or plural text of a key pair such as "trips.count.one" / "trips.count.other"
export function plural(base: PluralBase<MessageKey>, n: number, params: Record<string, string | number> = {}): string {
  return t(`${base}.${n === 1 ? 'one' : 'other'}` as MessageKey, { n, ...params });
}
