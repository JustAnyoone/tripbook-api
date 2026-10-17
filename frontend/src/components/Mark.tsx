export function Mark() {
  // Two stops joined by a dashed route
  return (
    <svg className="brand-mark" width="34" height="14" viewBox="0 0 34 14" aria-hidden="true">
      <circle cx="4" cy="7" r="3.2" fill="currentColor" />
      <line x1="9" y1="7" x2="25" y2="7" stroke="currentColor" strokeWidth="1.6" strokeDasharray="3 3" />
      <circle cx="30" cy="7" r="3.2" fill="none" stroke="currentColor" strokeWidth="1.8" />
    </svg>
  );
}
