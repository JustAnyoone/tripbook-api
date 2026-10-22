import { useCallback, useEffect, useId, useLayoutEffect, useRef, useState, type CSSProperties, type KeyboardEvent } from 'react';
import { addDays, addMonths, formatDate, parse, todayIso } from '../lib/dates';

interface DatePickerProps {
  id: string;
  /** "yyyy-MM-dd", or an empty string when nothing is chosen */
  value: string;
  onChange: (value: string) => void;
  /** Earliest day that can be chosen, "yyyy-MM-dd" */
  min?: string;
  /** Latest day that can be chosen, "yyyy-MM-dd" */
  max?: string;
  /** Shows month and year selects in the header, for dates far from today such as a birth date */
  jump?: boolean;
  placeholder?: string;
  /** Shows a Clear button; for optional dates */
  clearable?: boolean;
}

const WEEKDAYS = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'];
const MONTH_NAMES = Array.from({ length: 12 }, (_, month) =>
  new Intl.DateTimeFormat('en-US', { month: 'long' }).format(new Date(2000, month, 1)),
);
const monthTitle = new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' });
const dayLabel = new Intl.DateTimeFormat('en-US', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });

function firstOfMonth(iso: string): string {
  return `${iso.slice(0, 7)}-01`;
}

// Always six rows of seven days, so the popover keeps its height when the month changes
function monthGrid(monthStart: string): string[] {
  const first = parse(monthStart);
  const start = addDays(monthStart, -first.getDay());
  return Array.from({ length: 42 }, (_, index) => addDays(start, index));
}

// Notion-style date field: a button that opens a month calendar. Dates are picked, never typed,
// so an impossible date such as 31 Nov cannot be entered and days before `min` cannot be chosen.
export function DatePicker({ id, value, onChange, min, max, jump = false, placeholder = 'Select a date', clearable = false }: DatePickerProps) {
  const [open, setOpen] = useState(false);
  const [focused, setFocused] = useState('');
  const [position, setPosition] = useState<CSSProperties>({});
  const triggerRef = useRef<HTMLButtonElement>(null);
  const popoverRef = useRef<HTMLDivElement>(null);
  // Keyboard focus follows the highlighted day, except while the user works the month/year selects
  const followFocus = useRef(true);
  const popoverId = useId();
  const today = todayIso();

  const closePopover = useCallback((returnFocus: boolean) => {
    setOpen(false);
    if (returnFocus) triggerRef.current?.focus();
  }, []);

  function openPopover() {
    // Start on the chosen day, or on the day closest to today that can be picked
    followFocus.current = true;
    setFocused(clamp(value || today));
    setOpen(true);
  }

  // Place the popover under the field, or above it when there is no room below.
  // Fixed positioning keeps it from being clipped by the scrolling dialog.
  useLayoutEffect(() => {
    if (!open || !triggerRef.current) return;
    const rect = triggerRef.current.getBoundingClientRect();
    const height = popoverRef.current?.offsetHeight ?? 330;
    const below = window.innerHeight - rect.bottom;
    const top = below >= height + 8 || rect.top < height + 8 ? rect.bottom + 6 : rect.top - height - 6;
    const width = 288;
    const left = Math.max(8, Math.min(rect.left, window.innerWidth - width - 8));
    setPosition({ top, left, width });
  }, [open]);

  // Keep keyboard focus on the highlighted day
  useEffect(() => {
    if (!open || !followFocus.current) return;
    popoverRef.current?.querySelector<HTMLElement>(`[data-date="${focused}"]`)?.focus();
  }, [open, focused]);

  // Close on a click outside, or when the page scrolls under a fixed popover
  useEffect(() => {
    if (!open) return;
    function onPointerDown(event: MouseEvent) {
      const target = event.target as Node;
      if (!popoverRef.current?.contains(target) && !triggerRef.current?.contains(target)) closePopover(false);
    }
    document.addEventListener('mousedown', onPointerDown);
    return () => document.removeEventListener('mousedown', onPointerDown);
  }, [open, closePopover]);

  function isDisabled(iso: string) {
    return Boolean((min && iso < min) || (max && iso > max));
  }

  // Never park the keyboard focus on a day that cannot be chosen
  function clamp(iso: string) {
    if (min && iso < min) return min;
    if (max && iso > max) return max;
    return iso;
  }

  function move(iso: string) {
    if (isDisabled(iso)) return;
    followFocus.current = true;
    setFocused(iso);
  }

  function onGridKeyDown(event: KeyboardEvent) {
    const steps: Record<string, () => string> = {
      ArrowLeft: () => addDays(focused, -1),
      ArrowRight: () => addDays(focused, 1),
      ArrowUp: () => addDays(focused, -7),
      ArrowDown: () => addDays(focused, 7),
      PageUp: () => clamp(addMonths(focused, -1)),
      PageDown: () => clamp(addMonths(focused, 1)),
      Home: () => addDays(focused, -parse(focused).getDay()),
      End: () => addDays(focused, 6 - parse(focused).getDay()),
    };
    const step = steps[event.key];
    if (step) {
      event.preventDefault();
      move(step());
    }
  }

  function onPopoverKeyDown(event: KeyboardEvent) {
    if (event.key === 'Escape') {
      // Close only the calendar; the surrounding dialog must stay open
      event.stopPropagation();
      closePopover(true);
    }
  }

  function choose(iso: string) {
    if (isDisabled(iso)) return;
    onChange(iso);
    closePopover(true);
  }

  const monthStart = firstOfMonth(focused || value || today);
  const days = open ? monthGrid(monthStart) : [];
  const previousMonthEnd = addDays(monthStart, -1);
  const canGoBack = !min || previousMonthEnd >= min;
  const canGoForward = !max || addMonths(monthStart, 1) <= max;
  const shownYear = Number(monthStart.slice(0, 4));
  const shownMonth = Number(monthStart.slice(5, 7)) - 1;
  const firstYear = min ? Number(min.slice(0, 4)) : Math.min(shownYear, Number(today.slice(0, 4))) - 100;
  const lastYear = max ? Number(max.slice(0, 4)) : Math.max(shownYear, Number(today.slice(0, 4))) + 10;
  const years = Array.from({ length: Math.max(lastYear - firstYear + 1, 1) }, (_, index) => lastYear - index);

  function jumpTo(year: number, month: number) {
    followFocus.current = false;
    setFocused(clamp(`${String(year).padStart(4, '0')}-${String(month + 1).padStart(2, '0')}-01`));
  }

  return (
    <div className="datepicker">
      <button
        ref={triggerRef}
        id={id}
        type="button"
        className="datepicker-trigger"
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-controls={open ? popoverId : undefined}
        onClick={() => (open ? closePopover(false) : openPopover())}
      >
        <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden="true" className="datepicker-icon">
          <rect x="2" y="3" width="12" height="11" rx="2" fill="none" stroke="currentColor" strokeWidth="1.4" />
          <path d="M2 6.5h12M5.5 1.8v2.6M10.5 1.8v2.6" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
        </svg>
        <span className={value ? undefined : 'datepicker-placeholder'}>{value ? formatDate(value) : placeholder}</span>
      </button>

      {open && (
        <div
          ref={popoverRef}
          id={popoverId}
          className="datepicker-popover"
          role="dialog"
          aria-label="Choose a date"
          style={position}
          onKeyDown={onPopoverKeyDown}
        >
          <div className="datepicker-header">
            {jump ? (
              <div className="datepicker-jump">
                <select
                  aria-label="Month"
                  value={shownMonth}
                  onChange={(event) => jumpTo(shownYear, Number(event.target.value))}
                >
                  {MONTH_NAMES.map((name, month) => (
                    <option key={name} value={month}>
                      {name}
                    </option>
                  ))}
                </select>
                <select aria-label="Year" value={shownYear} onChange={(event) => jumpTo(Number(event.target.value), shownMonth)}>
                  {years.map((year) => (
                    <option key={year} value={year}>
                      {year}
                    </option>
                  ))}
                </select>
              </div>
            ) : (
              <strong aria-live="polite">{monthTitle.format(parse(monthStart))}</strong>
            )}
            <div className="datepicker-nav">
              <button
                type="button"
                className="datepicker-nav-button"
                aria-label="Previous month"
                disabled={!canGoBack}
                onClick={() => move(clamp(addMonths(monthStart, -1)))}
              >
                ‹
              </button>
              <button
                type="button"
                className="datepicker-nav-button"
                aria-label="Next month"
                disabled={!canGoForward}
                onClick={() => move(clamp(addMonths(monthStart, 1)))}
              >
                ›
              </button>
            </div>
          </div>

          <div role="grid" aria-label={monthTitle.format(parse(monthStart))} onKeyDown={onGridKeyDown}>
            <div role="row" className="datepicker-row">
              {WEEKDAYS.map((weekday) => (
                <span key={weekday} role="columnheader" className="datepicker-weekday">
                  {weekday}
                </span>
              ))}
            </div>
            {Array.from({ length: 6 }, (_, week) => (
              <div role="row" className="datepicker-row" key={week}>
                {days.slice(week * 7, week * 7 + 7).map((iso) => {
                  const outside = iso.slice(0, 7) !== monthStart.slice(0, 7);
                  const classes = ['datepicker-day'];
                  if (outside) classes.push('is-outside');
                  if (iso === today) classes.push('is-today');
                  if (iso === value) classes.push('is-selected');
                  return (
                    <span role="gridcell" key={iso} aria-selected={iso === value}>
                      <button
                        type="button"
                        className={classes.join(' ')}
                        data-date={iso}
                        tabIndex={iso === focused ? 0 : -1}
                        disabled={isDisabled(iso)}
                        aria-label={dayLabel.format(parse(iso))}
                        aria-current={iso === today ? 'date' : undefined}
                        onClick={() => choose(iso)}
                      >
                        {parse(iso).getDate()}
                      </button>
                    </span>
                  );
                })}
              </div>
            ))}
          </div>

          <div className="datepicker-footer">
            <button type="button" className="datepicker-link" disabled={isDisabled(today)} onClick={() => choose(today)}>
              Today
            </button>
            {clearable && value && (
              <button
                type="button"
                className="datepicker-link"
                onClick={() => {
                  onChange('');
                  closePopover(true);
                }}
              >
                Clear
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

