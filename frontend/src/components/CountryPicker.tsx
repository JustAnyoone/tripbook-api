import { useEffect, useId, useLayoutEffect, useRef, useState, type CSSProperties, type KeyboardEvent } from 'react';
import { findCountry, searchCountries, type Country } from '../lib/countries';
import { Flag } from './Flag';

interface CountryPickerProps {
  id: string;
  /** What the field shows; a known country name once the user picks one */
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
}

// Search box with a list of countries and their flags. The user can type "bra" and pick Brazil,
// or scroll the list. The text can be edited freely; the form decides whether it names a country.
export function CountryPicker({ id, value, onChange, required }: CountryPickerProps) {
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(0);
  const [position, setPosition] = useState<CSSProperties>({});
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLUListElement>(null);
  const listId = useId();

  // While the box shows an exact country there is nothing to narrow down: show the whole list
  const matches: Country[] = findCountry(value) ? searchCountries('') : searchCountries(value);
  const current = Math.min(active, Math.max(matches.length - 1, 0));

  useLayoutEffect(() => {
    if (!open || !inputRef.current) return;
    const rect = inputRef.current.getBoundingClientRect();
    const height = 232;
    const below = window.innerHeight - rect.bottom;
    const top = below >= height + 8 || rect.top < height + 8 ? rect.bottom + 4 : rect.top - height - 4;
    setPosition({ top, left: rect.left, width: rect.width });
  }, [open]);

  useEffect(() => {
    if (!open) return;
    listRef.current?.querySelector<HTMLElement>('[aria-selected="true"]')?.scrollIntoView?.({ block: 'nearest' });
  }, [open, current]);

  // Native form validation: only a name from the list can be submitted
  useEffect(() => {
    inputRef.current?.setCustomValidity(!value || findCountry(value) ? '' : 'Choose a country from the list');
  }, [value]);

  function choose(country: Country) {
    onChange(country.name);
    setOpen(false);
  }

  function onKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      if (!open) {
        setOpen(true);
        return;
      }
      const step = event.key === 'ArrowDown' ? 1 : -1;
      setActive((current + step + matches.length) % Math.max(matches.length, 1));
    } else if (event.key === 'Enter' && open && matches[current]) {
      event.preventDefault();
      choose(matches[current]);
    } else if (event.key === 'Escape' && open) {
      // Close only the list; the dialog around the form stays open
      event.stopPropagation();
      setOpen(false);
    }
  }

  return (
    <div className="combobox">
      <span className="combobox-flag">
        <Flag country={value} />
      </span>
      <input
        ref={inputRef}
        id={id}
        value={value}
        role="combobox"
        aria-expanded={open}
        aria-controls={listId}
        aria-autocomplete="list"
        aria-activedescendant={open && matches[current] ? `${listId}-${matches[current].code}` : undefined}
        autoComplete="off"
        placeholder="Start typing a country"
        required={required}
        onChange={(event) => {
          onChange(event.target.value);
          setActive(0);
          setOpen(true);
        }}
        onFocus={() => setOpen(true)}
        onBlur={() => setOpen(false)}
        onKeyDown={onKeyDown}
      />
      {open && (
        <ul ref={listRef} id={listId} role="listbox" className="combobox-list" style={position}>
          {matches.length === 0 && <li className="combobox-empty">No country matches “{value}”.</li>}
          {matches.map((country, index) => (
            <li
              key={country.code}
              id={`${listId}-${country.code}`}
              role="option"
              aria-selected={index === current}
              className={index === current ? 'is-active' : undefined}
              // mousedown, not click: the input loses focus first and would close the list
              onMouseDown={(event) => {
                event.preventDefault();
                choose(country);
              }}
              onMouseEnter={() => setActive(index)}
            >
              <Flag country={country.name} />
              <span>{country.name}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
