import { useState } from 'react';
import { messageOf } from '../api/client';
import { loadSampleData } from '../lib/seed';
import { ConfirmDialog } from './ConfirmDialog';
import { Dialog } from './Dialog';
import { SparklesIcon } from './icons';
import { useRefresh } from './RefreshContext';

// Fills the account with a few destinations, passengers and trips so the app can be explored right away
export function SampleDataButton() {
  const [asking, setAsking] = useState(false);
  const [summary, setSummary] = useState<string | null>(null);
  const { refresh } = useRefresh();

  async function load() {
    try {
      const { destinations, passengers, trips } = await loadSampleData();
      setSummary(
        destinations + passengers + trips === 0
          ? 'The sample data is already here, nothing new was added.'
          : `Added ${destinations} destinations, ${passengers} passengers and ${trips} trips.`,
      );
    } catch (failure) {
      // Whatever was created before the failure stays; show it so the lists are not stale
      refresh();
      throw new Error(messageOf(failure));
    }
    refresh();
    setAsking(false);
  }

  return (
    <>
      <button type="button" className="button button-outline button-with-icon" onClick={() => setAsking(true)}>
        <SparklesIcon />
        Sample data
      </button>
      {asking && (
        <ConfirmDialog
          title="Load sample data"
          message="Adds a few example destinations, passengers and trips to your account. Running it again does not create duplicates."
          confirmLabel="Add sample data"
          tone="primary"
          onConfirm={load}
          onCancel={() => setAsking(false)}
        />
      )}
      {summary && (
        <Dialog title="Sample data" onClose={() => setSummary(null)}>
          <p className="dialog-text" role="status">
            {summary}
          </p>
          <div className="dialog-actions">
            <button type="button" className="button button-primary" onClick={() => setSummary(null)}>
              Done
            </button>
          </div>
        </Dialog>
      )}
    </>
  );
}
