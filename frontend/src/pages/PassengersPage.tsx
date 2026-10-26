import { useRef, useState } from 'react';
import { passengersApi, photoApi } from '../api/client';
import type { Passenger, PassengerInput } from '../api/types';
import { t } from '../i18n';
import { addDays, formatDate, todayIso } from '../lib/dates';
import { DatePicker } from '../components/DatePicker';
import { Field, FormShell } from '../components/FormShell';
import { ListPage, type Column, type FormProps } from '../components/ListPage';

// Avatar shown in the name column
function Avatar({ passenger }: { passenger: Passenger }) {
  if (passenger.photoUrl) {
    return (
      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
        <img
          src={passenger.photoUrl}
          alt={passenger.name}
          style={{ width: 28, height: 28, borderRadius: '50%', objectFit: 'cover', flexShrink: 0 }}
        />
        <strong>{passenger.name}</strong>
      </span>
    );
  }
  return <strong>{passenger.name}</strong>;
}

const columns = (): Column<Passenger>[] => [
  { key: 'name', header: t('passengers.name'), sortField: 'name', render: (p) => <Avatar passenger={p} /> },
  { key: 'documentNumber', header: t('passengers.document'), sortField: 'documentNumber', render: (p) => <span className="tabular">{p.documentNumber}</span> },
  { key: 'email', header: t('passengers.email'), sortField: 'email', render: (p) => p.email },
  { key: 'birthDate', header: t('passengers.born'), render: (p) => <span className="tabular">{formatDate(p.birthDate)}</span> },
];

function PassengerForm({ initial, onSubmit, onCancel }: FormProps<Passenger, PassengerInput>) {
  const [name, setName] = useState(initial?.name ?? '');
  const [documentNumber, setDocumentNumber] = useState(initial?.documentNumber ?? '');
  const [email, setEmail] = useState(initial?.email ?? '');
  const [birthDate, setBirthDate] = useState(initial?.birthDate ?? '');
  const [photoUrl, setPhotoUrl] = useState(initial?.photoUrl ?? null);
  const [uploading, setUploading] = useState(false);
  const [photoError, setPhotoError] = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  async function handlePhotoChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file || !initial) return;
    setUploading(true);
    setPhotoError(null);
    try {
      const result = await photoApi.upload(initial.id, file);
      setPhotoUrl(result.photoUrl);
    } catch (err) {
      setPhotoError(err instanceof Error ? err.message : t('error.generic'));
    } finally {
      setUploading(false);
    }
  }

  async function handleDeletePhoto() {
    if (!initial) return;
    try {
      await photoApi.remove(initial.id);
      setPhotoUrl(null);
    } catch (err) {
      setPhotoError(err instanceof Error ? err.message : t('error.generic'));
    }
  }

  return (
    <FormShell
      submitLabel={initial ? t('common.saveChanges') : t('passengers.add')}
      onSubmit={() =>
        onSubmit({
          name: name.trim(),
          documentNumber: documentNumber.trim(),
          email: email.trim(),
          birthDate: birthDate || null,
        })
      }
      onCancel={onCancel}
    >
      {/* Photo upload — only available when editing an existing passenger */}
      {initial && (
        <Field label={t('passengers.photo')} hint={t('passengers.photoHint')}>
          {() => (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
              {photoUrl && (
                <img
                  src={photoUrl}
                  alt={name}
                  style={{ width: 48, height: 48, borderRadius: '50%', objectFit: 'cover' }}
                />
              )}
              <button
                type="button"
                className="button button-outline"
                onClick={() => fileRef.current?.click()}
                disabled={uploading}
              >
                {uploading ? t('passengers.uploading') : t('passengers.uploadPhoto')}
              </button>
              {photoUrl && (
                <button type="button" className="button button-outline" onClick={handleDeletePhoto}>
                  {t('passengers.deletePhoto')}
                </button>
              )}
              <input
                ref={fileRef}
                type="file"
                accept="image/*"
                style={{ display: 'none' }}
                onChange={handlePhotoChange}
              />
              {photoError && <span style={{ color: 'var(--color-error, red)', fontSize: '0.85em' }}>{photoError}</span>}
            </div>
          )}
        </Field>
      )}
      <Field label={t('passengers.name')}>
        {(id) => <input id={id} value={name} onChange={(e) => setName(e.target.value)} required />}
      </Field>
      <Field label={t('passengers.documentNumber')} hint={t('passengers.documentHint')}>
        {(id) => (
          <input
            id={id}
            value={documentNumber}
            onChange={(e) => setDocumentNumber(e.target.value)}
            pattern="[A-Za-z0-9]{6,20}"
            title={t('passengers.documentTitle')}
            required
          />
        )}
      </Field>
      <Field label={t('passengers.email')}>
        {(id) => <input id={id} type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />}
      </Field>
      <Field label={t('passengers.birthDate')} hint={t('common.optional')}>
        {(id) => (
          <DatePicker
            id={id}
            value={birthDate}
            onChange={setBirthDate}
            max={addDays(todayIso(), -1)}
            jump
            clearable
            placeholder={t('passengers.birthPlaceholder')}
          />
        )}
      </Field>
    </FormShell>
  );
}

export function PassengersPage() {
  return (
    <ListPage<Passenger, PassengerInput>
      resource="passengers"
      api={passengersApi}
      filter={{ label: t('passengers.searchLabel'), param: 'name', placeholder: t('passengers.searchPlaceholder') }}
      columns={columns()}
      renderForm={(props) => <PassengerForm {...props} />}
      deleteParams={(p) => ({ name: p.name })}
    />
  );
}
