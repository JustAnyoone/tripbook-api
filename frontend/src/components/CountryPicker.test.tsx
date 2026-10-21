import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';
import { CountryPicker } from './CountryPicker';

function Harness({ initial = '' }: { initial?: string }) {
  const [value, setValue] = useState(initial);
  return (
    <>
      <label htmlFor="country">Country</label>
      <CountryPicker id="country" value={value} onChange={setValue} />
      <output data-testid="value">{value}</output>
    </>
  );
}

describe('CountryPicker', () => {
  it('filters the list while typing and shows the flag after choosing', async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await user.type(screen.getByLabelText('Country'), 'bras');
    await user.click(screen.getByRole('option', { name: /Brazil/ }));

    expect(screen.getByTestId('value')).toHaveTextContent('Brazil');
    expect(screen.getByLabelText('Country')).toHaveValue('Brazil');
    expect(screen.getAllByRole('img', { name: 'Flag of Brazil' }).length).toBeGreaterThan(0);
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
  });

  it('chooses with the keyboard', async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await user.type(screen.getByLabelText('Country'), 'portu{ArrowDown}{Enter}');

    expect(screen.getByTestId('value')).toHaveTextContent(/^(Portugal|Equatorial Guinea)$/);
  });

  it('rejects text that is not a country', async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await user.type(screen.getByLabelText('Country'), 'Atlantis');

    expect(screen.getByLabelText('Country')).toBeInvalid();
    expect(screen.getByText(/No country matches/)).toBeInTheDocument();
  });
});
