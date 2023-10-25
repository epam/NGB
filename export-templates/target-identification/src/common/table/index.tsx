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
  return (
    <th
      key={`column-${index}`}
      className={classNames(
        className,
        commonCellClassName,
        'bg-slate-100',
        'select-none',
        {
          'hover:underline': sortable,
          'cursor-pointer': sortable,
        }
      )}
      onClick={() => sortable ? toggleSorting(column) : undefined}
      style={{minWidth: 200}}
    >
      <div className={classNames(
        'w-full',
        'flex',
        'items-center'
      )}>
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
    <th
      key={`column-filter-${index}`}
      className={classNames(
        commonCellClassName,
        'bg-slate-100',
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
    </th>
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
  const useFilters = showFilters && filters.some((f) => f.type !== FilterType.unknown);
  return (
    <div className={classNames('w-full', 'relative', className)} style={style}>
      <div className="static w-full overflow-auto">
        <table className={classNames('min-w-full', 'border-collapse', 'border')}>
          <thead>
          <tr>
            {
              columns.map((column, index) => renderTableColumn(
                column,
                index,
                sortingInfo,
                filterInfo,
              ))
            }
          </tr>
          {
            useFilters && (
              <tr>
                {
                  columns.map((column, index) => renderTableColumnFilter(
                    column,
                    index,
                    filterInfo,
                  ))
                }
              </tr>
            )
          }
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
