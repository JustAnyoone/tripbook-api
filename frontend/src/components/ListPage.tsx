import { useEffect, useState, type ReactNode } from 'react';
import type { Resource } from '../api/client';
import { useDebounced } from '../hooks/useDebounced';
import { useList } from '../hooks/useList';
import { ConfirmDialog } from './ConfirmDialog';
import { Dialog } from './Dialog';
import { ErrorBanner } from './ErrorBanner';
import { Pagination } from './Pagination';
import { useRefresh } from './RefreshContext';

export interface Column<T> {
  key: string;
  header: string;
  /** Server-side sort field. Columns without it are not sortable. */
  sortField?: string;
  align?: 'end';
  render: (item: T) => ReactNode;
}

export interface FilterConfig {
  label: string;
  /** Query parameter the API reads, such as "country" */
  param: string;
  placeholder?: string;
  /** When present the filter is a select instead of a text box */
  options?: { value: string; label: string }[];
}

export interface FormProps<T, I> {
  initial: T | null;
  onSubmit: (input: I) => Promise<void>;
  onCancel: () => void;
}

export interface SortOption {
  /** "field,direction", for example "departureDate,desc" */
  value: string;
  label: string;
}

interface ListPageProps<T extends { id: number }, I> {
  title: string;
  /** Singular name used in buttons and messages, for example "destination" */
  noun: string;
  nounPlural: string;
  api: Resource<T, I>;
  filter: FilterConfig;
  /** Table columns; not needed when `renderCard` is used */
  columns?: Column<T>[];
  /** When set, rows render as cards instead of table rows; `actions` holds the Edit and Delete buttons */
  renderCard?: (item: T, actions: ReactNode) => ReactNode;
  /** Card layouts have no column headers to click, so sorting is offered as a select */
  sortOptions?: SortOption[];
  renderForm: (props: FormProps<T, I>) => ReactNode;
  describe: (item: T) => string;
  /** Extra sentence for the delete confirmation, for example what else gets removed */
  deleteNote?: string;
  emptyHint: string;
  /** Extra buttons next to the Add button, for example an export */
  headerActions?: ReactNode;
}

interface Sort {
  field: string;
  direction: 'asc' | 'desc';
}

// One page for every resource: a paginated, filterable, sortable table plus the create, edit and delete flows.
export function ListPage<T extends { id: number }, I>({
  title,
  noun,
  nounPlural,
  api,
  filter,
  columns = [],
  renderCard,
  sortOptions,
  renderForm,
  describe,
  deleteNote,
  emptyHint,
  headerActions,
}: ListPageProps<T, I>) {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [filterInput, setFilterInput] = useState('');
  const [sort, setSort] = useState<Sort | null>(null);
  const [editing, setEditing] = useState<T | 'new' | null>(null);
  const [deleting, setDeleting] = useState<T | null>(null);

  const { version: dataVersion } = useRefresh();
  const filterValue = useDebounced(filterInput, 300);

  const { data, loading, error, reload } = useList(
    () =>
      api.list({
        page,
        size,
        sort: sort ? `${sort.field},${sort.direction}` : undefined,
        filter: { [filter.param]: filterValue },
      }),
    [api, page, size, sort, filterValue, filter.param, dataVersion],
  );

  // After deleting the last row of the last page, step back instead of showing an empty page
  useEffect(() => {
    if (data && data.totalPages > 0 && page >= data.totalPages) setPage(data.totalPages - 1);
  }, [data, page]);

  function changeFilter(value: string) {
    setFilterInput(value);
    setPage(0);
  }

  function toggleSort(field: string) {
    setPage(0);
    setSort((current) => {
      if (current?.field !== field) return { field, direction: 'asc' };
      return current.direction === 'asc' ? { field, direction: 'desc' } : null;
    });
  }

  async function save(input: I) {
    if (editing === 'new' || editing === null) await api.create(input);
    else await api.update(editing.id, input);
    setEditing(null);
    reload();
  }

  async function confirmDelete() {
    if (!deleting) return;
    await api.remove(deleting.id);
    setDeleting(null);
    reload();
  }

  function chooseSort(value: string) {
    setPage(0);
    if (!value) return setSort(null);
    const [field, direction] = value.split(',');
    setSort({ field, direction: direction === 'desc' ? 'desc' : 'asc' });
  }

  const actionsFor = (item: T) => (
    <>
      <button type="button" className="button button-quiet" onClick={() => setEditing(item)}>
        Edit
      </button>
      <button type="button" className="button button-quiet button-danger-text" onClick={() => setDeleting(item)}>
        Delete
      </button>
    </>
  );

  const hasFilter = filterValue.trim().length > 0;
  const total = data?.totalElements ?? 0;
  const rows = data?.content ?? [];

  return (
    <section>
      <header className="page-header">
        <div>
          <h1>{title}</h1>
          <p className="page-count" aria-live="polite">
            {data ? `${total} ${total === 1 ? noun : nounPlural}${hasFilter ? ' found' : ''}` : ' '}
          </p>
        </div>
        <div className="page-actions">
          {headerActions}
          <button type="button" className="button button-primary" onClick={() => setEditing('new')}>
            Add {noun}
          </button>
        </div>
      </header>

      <div className="toolbar">
        <div className="field toolbar-field">
          <label htmlFor="list-filter">{filter.label}</label>
          {filter.options ? (
            <select id="list-filter" value={filterInput} onChange={(event) => changeFilter(event.target.value)}>
              <option value="">All</option>
              {filter.options.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          ) : (
            <input
              id="list-filter"
              type="search"
              value={filterInput}
              placeholder={filter.placeholder}
              onChange={(event) => changeFilter(event.target.value)}
            />
          )}
        </div>
        {renderCard && sortOptions && (
          <div className="field toolbar-field">
            <label htmlFor="list-sort">Sort by</label>
            <select id="list-sort" value={sort ? `${sort.field},${sort.direction}` : ''} onChange={(event) => chooseSort(event.target.value)}>
              <option value="">Default order</option>
              {sortOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {error && <ErrorBanner message={error} onRetry={reload} />}

      <div className={renderCard ? 'card-list-wrap' : 'table-wrap'} aria-busy={loading}>
        {renderCard ? (
          <ul className={`card-list${loading ? ' is-loading' : ''}`}>
            {rows.map((item) => (
              <li key={item.id}>{renderCard(item, actionsFor(item))}</li>
            ))}
          </ul>
        ) : (
          <table>
            <thead>
              <tr>
                {columns.map((column) => {
                  const active = sort?.field === column.sortField;
                  const ariaSort = !column.sortField ? undefined : active ? (sort?.direction === 'asc' ? 'ascending' : 'descending') : 'none';
                  return (
                    <th key={column.key} scope="col" aria-sort={ariaSort} className={column.align === 'end' ? 'cell-end' : undefined}>
                      {column.sortField ? (
                        <button type="button" className="sort-button" onClick={() => toggleSort(column.sortField!)}>
                          {column.header}
                          <span className="sort-indicator" aria-hidden="true">
                            {active ? (sort?.direction === 'asc' ? '▲' : '▼') : ''}
                          </span>
                        </button>
                      ) : (
                        column.header
                      )}
                    </th>
                  );
                })}
                <th scope="col">
                  <span className="visually-hidden">Actions</span>
                </th>
              </tr>
            </thead>
            <tbody className={loading ? 'is-loading' : undefined}>
              {rows.map((item) => (
                <tr key={item.id}>
                  {columns.map((column) => (
                    <td key={column.key} className={column.align === 'end' ? 'cell-end' : undefined}>
                      {column.render(item)}
                    </td>
                  ))}
                  <td className="cell-actions">{actionsFor(item)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {!data && loading && <p className="table-message">Loading {nounPlural}…</p>}
        {data && rows.length === 0 && !loading && (
          <div className="table-message">
            {hasFilter ? (
              <p>
                No {nounPlural} match “{filterValue.trim()}”.
              </p>
            ) : (
              <>
                <p>No {nounPlural} yet. {emptyHint}</p>
                <button type="button" className="button button-primary" onClick={() => setEditing('new')}>
                  Add {noun}
                </button>
              </>
            )}
          </div>
        )}
      </div>

      {data && data.totalElements > 0 && (
        <Pagination
          page={data.page}
          size={data.size}
          totalElements={data.totalElements}
          totalPages={data.totalPages}
          onPageChange={setPage}
          onSizeChange={(next) => {
            setSize(next);
            setPage(0);
          }}
        />
      )}

      {editing !== null && (
        <Dialog title={editing === 'new' ? `Add ${noun}` : `Edit ${noun}`} onClose={() => setEditing(null)}>
          {renderForm({
            initial: editing === 'new' ? null : editing,
            onSubmit: save,
            onCancel: () => setEditing(null),
          })}
        </Dialog>
      )}

      {deleting && (
        <ConfirmDialog
          title={`Delete ${noun}`}
          message={`Delete ${describe(deleting)}? ${deleteNote ? deleteNote + ' ' : ''}This cannot be undone.`}
          confirmLabel={`Delete ${noun}`}
          onConfirm={confirmDelete}
          onCancel={() => setDeleting(null)}
        />
      )}
    </section>
  );
}
