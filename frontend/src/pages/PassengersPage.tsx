import { useState } from 'react';
import { passengersApi } from '../api/client';
import type { Passenger, PassengerInput } from '../api/types';
import { formatDate } from '../lib/dates';
import { Field, FormShell } from '../components/FormShell';
import { ListPage, type Column, type FormProps } from '../components/ListPage';

const columns: Column<Passenger>[] = [
  { key: 'name', header: 'Name', sortField: 'name', render: (p) => <strong>{p.name}</strong> },
  { key: 'documentNumber', header: 'Document', sortField: 'documentNumber', render: (p) => <span className="tabular">{p.documentNumber}</span> },
  { key: 'email', header: 'Email', sortField: 'email', render: (p) => p.email },
  { key: 'birthDate', header: 'Born', render: (p) => <span className="tabular">{formatDate(p.birthDate)}</span> },
];

function PassengerForm({ initial, onSubmit, onCancel }: FormProps<Passenger, PassengerInput>) {
  const [name, setName] = useState(initial?.name ?? '');
  const [documentNumber, setDocumentNumber] = useState(initial?.documentNumber ?? '');
  const [email, setEmail] = useState(initial?.email ?? '');
  const [birthDate, setBirthDate] = useState(initial?.birthDate ?? '');

  return (
    <FormShell
      submitLabel={initial ? 'Save changes' : 'Add passenger'}
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
      <Field label="Name">
        {(id) => <input id={id} value={name} onChange={(e) => setName(e.target.value)} required />}
      </Field>
      <Field label="Document number" hint="6 to 20 letters or digits. Each passenger needs a different one.">
        {(id) => (
          <input
            id={id}
            value={documentNumber}
            onChange={(e) => setDocumentNumber(e.target.value)}
            pattern="[A-Za-z0-9]{6,20}"
            title="6 to 20 letters or digits"
            required
          />
        )}
      </Field>
      <Field label="Email">
        {(id) => <input id={id} type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />}
      </Field>
      <Field label="Birth date" hint="Optional">
        {(id) => <input id={id} type="date" value={birthDate} onChange={(e) => setBirthDate(e.target.value)} />}
      </Field>
    </FormShell>
  );
}

export function PassengersPage() {
  return (
    <ListPage<Passenger, PassengerInput>
      title="Passengers"
      noun="passenger"
      nounPlural="passengers"
      api={passengersApi}
      filter={{ label: 'Search by name', param: 'name', placeholder: 'Ana' }}
      columns={columns}
      renderForm={(props) => <PassengerForm {...props} />}
      describe={(p) => p.name}
      deleteNote="Their trips are deleted too."
      emptyHint="Add a passenger before booking a trip."
    />
  );
}
