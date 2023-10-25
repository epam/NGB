import {useCallback, useMemo, useState} from 'react';
import type {
  TableColumn,
  TableColumnConfiguration,
  TableColumnSorting,
  TableSorting,
  WithItemValue,
} from './types';
import type {ItemValue} from '../../model/types';
import type {FilterConfig} from '../../model/filtered-data';
import {isUndefined} from '../../model/utilities';
import {buildSorter} from '../../model/sorted-data';

export function getColumnConfig<Item>(
  column: TableColumn<Item>
): TableColumnConfiguration<Item> & WithItemValue<Item> {
  if (typeof column === 'string') {
    return {
      value: (item: Item) => item[column] as ItemValue,
      title: column,
      sortable: true,
      filter: column,
    }
  }
  if (typeof column === 'object') {
    if ('key' in column) {
      return {
        value: (item: Item) => item[column.key] as ItemValue,
        render: column.render,
        title: column.title ?? (column.key as string),
        sortable: column.sortable,
        filter: column.key,
      }
    }
    return {
      value: column.value,
      render: column.render,
      title: column.title,
      sortable: column.sortable,
      filter: !isUndefined(column.filter) ? column.filter : {
        value: column.value,
      }
    }
  }
  return {
    value: () => '',
    sortable: false,
    filter: false,
  };
}

export function getColumnFilterConfig<Item>(column: TableColumn<Item>): FilterConfig<Item> {
  return getColumnConfig(column).filter ?? false;
}

export function useSorting<Item>(): TableSorting<Item> {
  const [sorting, setSorting] = useState<TableColumnSorting<Item> | undefined>(undefined);
  const sorter = useMemo(() => {
    if (sorting) {
      const {
        value,
        sorter: columnSorter,
        sortable = true,
      } = getColumnConfig(sorting.column);
      if (!sortable) {
        return buildSorter(undefined);
      }
      if (columnSorter) {
        return columnSorter;
      }
      return buildSorter(value);
    }
    return buildSorter(undefined);
  }, [sorting]);
  const toggleSorting = useCallback((column: TableColumn<Item>) => {
    setSorting((current) => {
      if (current === undefined || current.column !== column) {
        return {
          column,
          ascending: true,
        };
      }
      if (!current.ascending) {
        return undefined;
      }
      return {
        column,
        ascending: false,
      };
    });
  }, [setSorting]);
  return {
    sorting,
    toggleSorting,
    sorter,
  };
}
