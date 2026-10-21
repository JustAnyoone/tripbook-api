import { useState } from 'react';
import { destinationsApi } from '../api/client';
import type { Destination, DestinationInput } from '../api/types';
import { Field, FormShell } from '../components/FormShell';
import { CountryPicker } from '../components/CountryPicker';
import { CountryLabel } from '../components/Flag';
import { findCountry } from '../lib/countries';
import { ListPage, type Column, type FormProps } from '../components/ListPage';

const columns: Column<Destination>[] = [
  { key: 'city', header: 'City', sortField: 'city', render: (d) => <strong>{d.city}</strong> },
  { key: 'country', header: 'Country', sortField: 'country', render: (d) => <CountryLabel country={d.country} /> },
  { key: 'description', header: 'Description', render: (d) => <span className="muted">{d.description ?? ''}</span> },
];

function DestinationForm({ initial, onSubmit, onCancel }: FormProps<Destination, DestinationInput>) {
  const [city, setCity] = useState(initial?.city ?? '');
  const [country, setCountry] = useState(initial?.country ?? '');
  const [description, setDescription] = useState(initial?.description ?? '');

  return (
    <FormShell
      submitLabel={initial ? 'Save changes' : 'Add destination'}
      onSubmit={() => onSubmit({ city: city.trim(), country: findCountry(country)?.name ?? country.trim(), description: description.trim() })}
      onCancel={onCancel}
    >
      <Field label="City">
        {(id) => <input id={id} value={city} onChange={(e) => setCity(e.target.value)} required />}
      </Field>
      <Field label="Country">
        {(id) => <CountryPicker id={id} value={country} onChange={setCountry} required />}
      </Field>
      <Field label="Description" hint="Optional">
        {(id) => <textarea id={id} rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />}
      </Field>
    </FormShell>
  );
}

export function DestinationsPage() {
  return (
    <ListPage<Destination, DestinationInput>
      title="Destinations"
      noun="destination"
      nounPlural="destinations"
      api={destinationsApi}
      filter={{ label: 'Search by country', param: 'country', placeholder: 'Portugal' }}
      columns={columns}
      renderForm={(props) => <DestinationForm {...props} />}
      describe={(d) => `${d.city}, ${d.country}`}
      emptyHint="Add the first one to start booking trips."
    />
  );
}
