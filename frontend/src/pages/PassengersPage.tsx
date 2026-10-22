import { useState } from 'react';
import { passengersApi } from '../api/client';
import type { Passenger, PassengerInput } from '../api/types';
import { t } from '../i18n';
import { addDays, formatDate, todayIso } from '../lib/dates';
import { DatePicker } from '../components/DatePicker';
import { Field, FormShell } from '../components/FormShell';
import { ListPage, type Column, type FormProps } from '../components/ListPage';

const columns = (): Column<Passenger>[] => [
  { key: 'name', header: t('passengers.name'), sortField: 'name', render: (p) => <strong>{p.name}</strong> },
  { key: 'documentNumber', header: t('passengers.document'), sortField: 'documentNumber', render: (p) => <span className="tabular">{p.documentNumber}</span> },
  { key: 'email', header: t('passengers.email'), sortField: 'email', render: (p) => p.email },
  { key: 'birthDate', header: t('passengers.born'), render: (p) => <span className="tabular">{formatDate(p.birthDate)}</span> },
];

function PassengerForm({ initial, onSubmit, onCancel }: FormProps<Passenger, PassengerInput>) {
  const [name, setName] = useState(initial?.name ?? '');
  const [documentNumber, setDocumentNumber] = useState(initial?.documentNumber ?? '');
  const [email, setEmail] = useState(initial?.email ?? '');
  const [birthDate, setBirthDate] = useState(initial?.birthDate ?? '');

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
