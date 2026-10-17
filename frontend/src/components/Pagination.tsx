interface PaginationProps {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  onSizeChange: (size: number) => void;
}

const PAGE_SIZES = [10, 20, 50];

export function Pagination({ page, size, totalElements, totalPages, onPageChange, onSizeChange }: PaginationProps) {
  const first = totalElements === 0 ? 0 : page * size + 1;
  const last = Math.min((page + 1) * size, totalElements);

  return (
    <nav className="pagination" aria-label="Pagination">
      <p className="pagination-range">
        {first}–{last} of {totalElements}
      </p>
      <label className="pagination-size">
        Rows per page
        <select value={size} onChange={(event) => onSizeChange(Number(event.target.value))}>
          {PAGE_SIZES.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
      </label>
      <div className="pagination-buttons">
        <button type="button" className="button button-quiet" disabled={page === 0} onClick={() => onPageChange(page - 1)}>
          Previous
        </button>
        <button
          type="button"
          className="button button-quiet"
          disabled={page + 1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
        >
          Next
        </button>
      </div>
    </nav>
  );
}
