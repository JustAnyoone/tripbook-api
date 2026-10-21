import type { ReactNode } from 'react';

// Small line icons that take the colour of the surrounding text. They are decorative:
// the text next to them carries the meaning, so they are hidden from screen readers.
function Icon({ children, size = 18 }: { children: ReactNode; size?: number }) {
  return (
    <svg
      className="icon"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {children}
    </svg>
  );
}

// Boarding pass with a tear-off stub
export function TripsIcon() {
  return (
    <Icon>
      <path d="M3 8.5A1.5 1.5 0 0 1 4.5 7h15A1.5 1.5 0 0 1 21 8.5V10a2 2 0 0 0 0 4v1.5a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 15.5V14a2 2 0 0 0 0-4Z" />
      <path d="M15 7v2M15 11v2M15 15v2" />
    </Icon>
  );
}

export function PassengersIcon() {
  return (
    <Icon>
      <circle cx="12" cy="8" r="3.5" />
      <path d="M5 20c.6-3.6 3.3-5.5 7-5.5s6.4 1.9 7 5.5" />
    </Icon>
  );
}

// Map pin
export function DestinationsIcon() {
  return (
    <Icon>
      <path d="M12 21s-6.5-5.6-6.5-11a6.5 6.5 0 0 1 13 0c0 5.4-6.5 11-6.5 11Z" />
      <circle cx="12" cy="10" r="2.4" />
    </Icon>
  );
}

export function SparklesIcon() {
  return (
    <Icon>
      <path d="M10 4l1.6 4.4L16 10l-4.4 1.6L10 16l-1.6-4.4L4 10l4.4-1.6Z" />
      <path d="M18 4v4M16 6h4M18 15v4M16 17h4" />
    </Icon>
  );
}

// An airplane pointing right: the Material "flight" outline, turned a quarter
export function PlaneIcon({ size = 20 }: { size?: number }) {
  return (
    <svg className="icon icon-plane" width={size} height={size} viewBox="0 0 24 24" aria-hidden="true">
      <path
        transform="rotate(90 12 12)"
        fill="currentColor"
        d="M21 16v-2l-8-5V3.5a1.5 1.5 0 0 0-3 0V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5Z"
      />
    </svg>
  );
}
