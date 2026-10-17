import { useState, type FormEvent, type ReactNode } from 'react';
import { messageOf } from '../api/client';
import { ErrorBanner } from './ErrorBanner';

interface FormShellProps {
  submitLabel: string;
  onSubmit: () => Promise<void>;
  onCancel: () => void;
  children: ReactNode;
}

// Shared frame of the create and edit forms: submit state, the API's error message and the buttons.
export function FormShell({ submitLabel, onSubmit, onCancel, children }: FormShellProps) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await onSubmit();
    } catch (failure) {
      setError(messageOf(failure));
      setBusy(false);
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <div className="form-fields">{children}</div>
      {error && <ErrorBanner message={error} />}
      <div className="dialog-actions">
        <button type="button" className="button button-quiet" onClick={onCancel}>
          Cancel
        </button>
        <button type="submit" className="button button-primary" disabled={busy}>
          {submitLabel}
        </button>
      </div>
    </form>
  );
}

interface FieldProps {
  label: string;
  hint?: string;
  children: (id: string) => ReactNode;
}

export function Field({ label, hint, children }: FieldProps) {
  const id = `field-${label.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`;
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {children(id)}
      {hint && <p className="field-hint">{hint}</p>}
    </div>
  );
}
