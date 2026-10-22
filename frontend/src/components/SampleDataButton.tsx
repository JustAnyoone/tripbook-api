import { useState } from 'react';
import { messageOf } from '../api/client';
import { t } from '../i18n';
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
          ? t('sample.nothingNew')
          : t('sample.added', { destinations, passengers, trips }),
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
        {t('sample.button')}
      </button>
      {asking && (
        <ConfirmDialog
          title={t('sample.title')}
          message={t('sample.message')}
          confirmLabel={t('sample.confirm')}
          tone="primary"
          onConfirm={load}
          onCancel={() => setAsking(false)}
        />
      )}
      {summary && (
        <Dialog title={t('sample.button')} onClose={() => setSummary(null)}>
          <p className="dialog-text" role="status">
            {summary}
          </p>
          <div className="dialog-actions">
            <button type="button" className="button button-primary" onClick={() => setSummary(null)}>
              {t('common.done')}
            </button>
          </div>
        </Dialog>
      )}
    </>
  );
}
