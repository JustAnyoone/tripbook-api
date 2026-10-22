import { useState } from 'react';
import { destinationsApi } from '../api/client';
import type { Destination, DestinationInput } from '../api/types';
import { Field, FormShell } from '../components/FormShell';
import { CountryPicker } from '../components/CountryPicker';
import { CountryLabel } from '../components/Flag';
import { t } from '../i18n';
import { findCountry } from '../lib/countries';
import { ListPage, type Column, type FormProps } from '../components/ListPage';

const columns = (): Column<Destination>[] => [
  { key: 'city', header: t('destinations.city'), sortField: 'city', render: (d) => <strong>{d.city}</strong> },
  { key: 'country', header: t('destinations.country'), sortField: 'country', render: (d) => <CountryLabel country={d.country} /> },
  { key: 'description', header: t('destinations.description'), render: (d) => <span className="muted">{d.description ?? ''}</span> },
];

function DestinationForm({ initial, onSubmit, onCancel }: FormProps<Destination, DestinationInput>) {
  const [city, setCity] = useState(initial?.city ?? '');
  const [country, setCountry] = useState(initial?.country ?? '');
  const [description, setDescription] = useState(initial?.description ?? '');

  return (
    <FormShell
      submitLabel={initial ? t('common.saveChanges') : t('destinations.add')}
      onSubmit={() => onSubmit({ city: city.trim(), country: findCountry(country)?.name ?? country.trim(), description: description.trim() })}
      onCancel={onCancel}
    >
      <Field label={t('destinations.city')}>
        {(id) => <input id={id} value={city} onChange={(e) => setCity(e.target.value)} required />}
      </Field>
      <Field label={t('destinations.country')}>
        {(id) => <CountryPicker id={id} value={country} onChange={setCountry} required />}
      </Field>
      <Field label={t('destinations.description')} hint={t('common.optional')}>
        {(id) => <textarea id={id} rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />}
      </Field>
    </FormShell>
  );
}

export function DestinationsPage() {
  return (
    <ListPage<Destination, DestinationInput>
      resource="destinations"
      api={destinationsApi}
      filter={{ label: t('destinations.searchLabel'), param: 'country', placeholder: t('destinations.searchPlaceholder') }}
      columns={columns()}
      renderForm={(props) => <DestinationForm {...props} />}
      deleteParams={(d) => ({ name: `${d.city}, ${d.country}` })}
    />
  );
}
