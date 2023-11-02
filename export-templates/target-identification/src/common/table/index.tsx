import {useMemo} from 'react';
import classNames from 'classnames';
import {
  Bars3BottomLeftIcon,
  Bars4Icon,
  BarsArrowDownIcon,
  BarsArrowUpIcon,
} from '@heroicons/react/24/outline';
import {usePagedData} from '../../model/paged-data';
import Pagination from '../pagination';
import DisplayValue from '../display-value';
import {useSortedData} from '../../model/sorted-data';
import type {Filters} from '../../model/filtered-data';
import {FilterType, isFilterSpecified, useFilteredData} from '../../model/filtered-data';
import type {TableColumn, TableProps, TableSorting} from './types';
import {getColumnConfig, getColumnFilterConfig, useSorting} from './utilities';
import TableColumnFilter from './filter';

const commonCellClassName = classNames('text-left', 'border', 'px-2', 'py-1')

function renderTableColumn<Item>(
  column: TableColumn<Item>,
  index: number,
  sorting: TableSorting<Item>,
  filters: Filters<Item>,
  showFilters: boolean,
) {
  const {
    sorting: columnSorting,
    toggleSorting,
  } = sorting;
  const {
    filters: columnFilters = [],
  } = filters;
  const {
    title,
    className,
    sortable = true,
  } = getColumnConfig(column);
  const isFiltered = isFilterSpecified(columnFilters[index]);
  const isAscending = columnSorting && columnSorting.column === column && columnSorting.ascending;
  const isDescending = columnSorting && columnSorting.column === column && !columnSorting.ascending;
  const useFilters = showFilters && columnFilters.some((f) => f.type !== FilterType.unknown);
  return (
    <th
      key={`column-${index}`}
      className={classNames(
        className,
        commonCellClassName,
        'bg-slate-100',
        'select-none',
      )}
      style={{minWidth: 200}}
    >
      <div
        className={classNames(
          'w-full',
          'flex',
          'items-center',
          {
            'hover:underline': sortable,
            'cursor-pointer': sortable,
          }
        )}
        onClick={() => sortable ? toggleSorting(column) : undefined}
      >
        <span>
          {title}
        </span>
        {
          isFiltered && (
            <Bars3BottomLeftIcon
              className="w-3 h-3 text-sky-900 ml-1"
            />
          )
        }
        {
          isAscending && (
            <BarsArrowDownIcon className="w-4 h-4 ml-auto" />
          )
        }
        {
          isDescending && (
            <BarsArrowUpIcon className="w-4 h-4 ml-auto" />
          )
        }
        {
          !isAscending && !isDescending && sortable && (
            <Bars4Icon className="w-4 h-4  ml-auto opacity-25" />
          )
        }
      </div>
      {
        useFilters
          ? (
            (typeof column === "object" && column.hasOwnProperty('showFilter') && !column.showFilter)
              ? null
              : <div>{renderTableColumnFilter(column, index, filters)}</div>
          ) : null
      }
    </th>
  );
}

function renderTableColumnFilter<Item>(
  column: TableColumn<Item>,
  index: number,
  filters: Filters<Item>,
) {
  const filter = filters?.filters[index];
  return (
    <div
      key={`column-filter-${index}`}
      className={classNames(
        'text-left',
        'px-2',
        'py-1',
        'bg-slate-100',
        'w-full',
      )}
    >
      <TableColumnFilter
        className={classNames(
          'w-full',
          'items-center',
          'text-xs',
          'font-normal',
        )}
        filter={filter}
        filters={filters}
      />
    </div>
  );
}

function renderTableCell<Item>(item: Item, column: TableColumn<Item>, columnIndex: number, itemIndex: number) {
  const {
    value,
    className,
    render,
  } = getColumnConfig(column);
  if (typeof render === 'function') {
    return (
      <td
        key={`cell-${itemIndex}-}${columnIndex}`}
        className={classNames(className, commonCellClassName)}
      >
        {render(item)}
      </td>
    );
  }
  const aValue = value(item);
  return (
    <td
      key={`cell-${itemIndex}-}${columnIndex}`}
      className={classNames(className, commonCellClassName)}
    >
      <DisplayValue value={aValue} />
    </td>
  );
}

function renderTableDataRow<Item>(item: Item, columns: TableColumn<Item>[], itemIndex: number) {
  return (
    <tr key={`item-${itemIndex}`}>
      {
        columns.map((column, columnIndex) => renderTableCell(
          item,
          column,
          columnIndex,
          itemIndex,
        ))
      }
    </tr>
  );
}

export default function Table<Item>(props: TableProps<Item>) {
  const {
    data: source,
    columns,
    className,
    style,
    showFilters = true,
  } = props;
  const config = useMemo(() => columns.map(getColumnFilterConfig), [columns]);
  const filterInfo = useFilteredData(source, config);
  const {
    filteredData,
    filters,
  } = filterInfo;
  const sortingInfo = useSorting<Item>();
  const {
    sorting,
    sorter,
  } = sortingInfo;
  const sortedData = useSortedData(filteredData, sorter, sorting?.ascending);
  const pagedData = usePagedData(sortedData);
  const {
    data,
    page,
    pagesCount
  } = pagedData;
  const firstElementIndex = page * pagesCount;
  return (
    <div className={classNames('w-full', 'relative', className)} style={style}>
      <div className="static w-full overflow-auto">
        <table className={classNames('min-w-full', 'border-collapse', 'border')}>
          <thead>
          <tr className="capitalize">
            {
              columns.map((column, index) => renderTableColumn(
                column,
                index,
                sortingInfo,
                filterInfo,
                showFilters,
              ))
            }
          </tr>
          </thead>
          <tbody>
          {
            data.map((item, index) => renderTableDataRow(
              item,
              columns,
              firstElementIndex + index,
            ))
          }
          </tbody>
        </table>
      </div>
      {
        sortedData.length === 0 && (
          <div className="w-full text-center my-1 text-slate-500">
            No data
          </div>
        )
      }
      <Pagination state={pagedData} />
    </div>
  );
}
