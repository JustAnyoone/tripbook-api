import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { DatePicker } from './DatePicker';

function Harness({
  initial = '',
  min,
  max,
  jump = false,
  clearable = false,
}: {
  initial?: string;
  min?: string;
  max?: string;
  jump?: boolean;
  clearable?: boolean;
}) {
  const [value, setValue] = useState(initial);
  return (
    <>
      <label htmlFor="when">When</label>
      <DatePicker id="when" value={value} onChange={setValue} min={min} max={max} jump={jump} clearable={clearable} />
      <output data-testid="value">{value}</output>
    </>
  );
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] });
  vi.setSystemTime(new Date(2026, 9, 19, 12, 0, 0));
});
afterEach(() => vi.useRealTimers());

describe('DatePicker', () => {
  it('shows a placeholder and opens a calendar on the current month', async () => {
    const user = userEvent.setup();
    render(<Harness min="2026-10-19" />);

    expect(screen.getByLabelText('When')).toHaveTextContent('Select a date');
    await user.click(screen.getByLabelText('When'));

    expect(screen.getByRole('grid', { name: 'October 2026' })).toBeInTheDocument();
  });

  it('chooses a day with a click, shows it formatted and closes', async () => {
    const user = userEvent.setup();
    render(<Harness min="2026-10-19" />);

    await user.click(screen.getByLabelText('When'));
    await user.click(screen.getByRole('button', { name: 'Friday, October 23, 2026' }));

    expect(screen.getByTestId('value')).toHaveTextContent('2026-10-23');
    expect(screen.getByLabelText('When')).toHaveTextContent('23 Oct 2026');
    expect(screen.queryByRole('grid')).not.toBeInTheDocument();
  });

  it('does not choose a day before the minimum', async () => {
    const user = userEvent.setup();
    render(<Harness min="2026-10-19" />);

    await user.click(screen.getByLabelText('When'));
    await user.click(screen.getByRole('button', { name: 'Saturday, October 17, 2026' }));

    expect(screen.getByTestId('value')).toHaveTextContent('');
    expect(screen.getByRole('grid')).toBeInTheDocument();
  });

  it('moves between months and can step forward but not before the minimum', async () => {
    const user = userEvent.setup();
    render(<Harness min="2026-10-19" />);

    await user.click(screen.getByLabelText('When'));
    expect(screen.getByRole('button', { name: 'Previous month' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Next month' }));

    expect(screen.getByRole('grid', { name: 'November 2026' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Previous month' })).toBeEnabled();
  });

  it('moves the focus with the arrow keys and chooses with Enter', async () => {
    const user = userEvent.setup();
    render(<Harness min="2026-10-19" />);

    await user.click(screen.getByLabelText('When'));
    expect(screen.getByRole('button', { name: 'Monday, October 19, 2026' })).toHaveFocus();
    await user.keyboard('{ArrowRight}{ArrowDown}');
    expect(screen.getByRole('button', { name: 'Tuesday, October 27, 2026' })).toHaveFocus();
    await user.keyboard('{Enter}');

    expect(screen.getByTestId('value')).toHaveTextContent('2026-10-27');
  });

  it('keeps the focus on the first allowed day when an arrow key would leave the range', async () => {
    const user = userEvent.setup();
    render(<Harness min="2026-10-19" />);

    await user.click(screen.getByLabelText('When'));
    await user.keyboard('{ArrowLeft}');

    expect(screen.getByRole('button', { name: 'Monday, October 19, 2026' })).toHaveFocus();
  });

  it('closes on Escape and returns the focus to the field', async () => {
    const user = userEvent.setup();
    render(<Harness min="2026-10-19" />);

    await user.click(screen.getByLabelText('When'));
    await user.keyboard('{Escape}');

    expect(screen.queryByRole('grid')).not.toBeInTheDocument();
    expect(screen.getByLabelText('When')).toHaveFocus();
  });

  it('picks today with the Today button', async () => {
    const user = userEvent.setup();
    render(<Harness min="2026-10-19" />);

    await user.click(screen.getByLabelText('When'));
    await user.click(screen.getByRole('button', { name: 'Today' }));

    expect(screen.getByTestId('value')).toHaveTextContent('2026-10-19');
  });

  it('clears an optional date', async () => {
    const user = userEvent.setup();
    render(<Harness initial="2026-10-23" min="2026-10-19" clearable />);

    await user.click(screen.getByLabelText('When'));
    await user.click(screen.getByRole('button', { name: 'Clear' }));

    expect(screen.getByTestId('value')).toHaveTextContent('');
  });

  it('does not allow days after max and opens on the latest allowed day', async () => {
    const user = userEvent.setup();
    render(<Harness max="2026-10-18" />);

    await user.click(screen.getByLabelText('When'));

    expect(screen.getByRole('button', { name: 'Saturday, October 17, 2026' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Monday, October 19, 2026' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next month' })).toBeDisabled();
  });

  it('jumps to another year and month with the selects', async () => {
    const user = userEvent.setup();
    render(<Harness max="2026-10-18" jump />);

    await user.click(screen.getByLabelText('When'));
    await user.selectOptions(screen.getByLabelText('Year'), '1990');
    await user.selectOptions(screen.getByLabelText('Month'), 'March');
    await user.click(screen.getByRole('button', { name: 'Thursday, March 15, 1990' }));

    expect(screen.getByTestId('value')).toHaveTextContent('1990-03-15');
  });
});
