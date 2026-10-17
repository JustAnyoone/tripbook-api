import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Pagination } from './Pagination';

function setup(page: number, totalElements = 25) {
  const onPageChange = vi.fn();
  const onSizeChange = vi.fn();
  render(
    <Pagination
      page={page}
      size={10}
      totalElements={totalElements}
      totalPages={Math.ceil(totalElements / 10)}
      onPageChange={onPageChange}
      onSizeChange={onSizeChange}
    />,
  );
  return { onPageChange, onSizeChange };
}

describe('Pagination', () => {
  it('shows which rows are on screen', () => {
    setup(1);
    expect(screen.getByText('11–20 of 25')).toBeInTheDocument();
  });

  it('cuts the range at the last row', () => {
    setup(2);
    expect(screen.getByText('21–25 of 25')).toBeInTheDocument();
  });

  it('cannot go back from the first page or forward from the last', () => {
    setup(0, 10);
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
  });

  it('asks for the next page', async () => {
    const { onPageChange } = setup(0);
    await userEvent.click(screen.getByRole('button', { name: 'Next' }));
    expect(onPageChange).toHaveBeenCalledWith(1);
  });

  it('reports a new page size', async () => {
    const { onSizeChange } = setup(0);
    await userEvent.selectOptions(screen.getByLabelText('Rows per page'), '50');
    expect(onSizeChange).toHaveBeenCalledWith(50);
  });
});
