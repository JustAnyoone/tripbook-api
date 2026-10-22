import { useState } from 'react';
import { messageOf } from '../api/client';
import { Dialog } from './Dialog';
import { ErrorBanner } from './ErrorBanner';

interface ConfirmDialogProps {
  title: string;
  message: string;
  confirmLabel: string;
  /** Colour of the confirm button; destructive actions use the default */
  tone?: 'danger' | 'primary';
  onConfirm: () => Promise<void>;
  onCancel: () => void;
}

// Used for deletes. When the API refuses (for example a destination that still has trips), its message is shown here.
export function ConfirmDialog({ title, message, confirmLabel, tone = 'danger', onConfirm, onCancel }: ConfirmDialogProps) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function confirm() {
    setBusy(true);
    setError(null);
    try {
      await onConfirm();
    } catch (failure) {
      setError(messageOf(failure));
      setBusy(false);
    }
  }

  return (
    <Dialog title={title} onClose={onCancel}>
      <p className="dialog-text">{message}</p>
      {error && <ErrorBanner message={error} />}
      <div className="dialog-actions">
        <button type="button" className="button button-quiet" onClick={onCancel}>
          Cancel
        </button>
        <button type="button" className={`button button-${tone}`} onClick={confirm} disabled={busy}>
          {confirmLabel}
        </button>
      </div>
    </Dialog>
  );
}
