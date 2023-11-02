import type {ReactNode} from 'react';
import type {ItemValue, ItemValueFn, KeyOfType} from '../../model/types';
import type {Sorter} from '../../model/sorted-data';
import type {FilterConfig} from '../../model/filtered-data';
import type {CommonProps} from '../types';
import {Filter, Filters} from "../../model/filtered-data";

export type TableColumnSimple<Item> = KeyOfType<Item, ItemValue>;

export type TableColumnConfiguration<Item> = {
  title?: ReactNode;
  render?: (item: Item) => ReactNode;
  className?: string;
  sortable?: boolean;
  sorter?: Sorter<Item>;
  filter?: FilterConfig<Item>;
  showFilter?: boolean;
}

export type TableColumnConfig<Item> = TableColumnConfiguration<Item> & {
  key: KeyOfType<Item, ItemValue>;
}

export type WithItemValue<Item> = {
  value: ItemValueFn<Item>;
}

export type ComputedTableColumnConfig<Item> = TableColumnConfiguration<Item> & WithItemValue<Item>

export type TableColumn<Item> = TableColumnSimple<Item> | TableColumnConfig<Item> | ComputedTableColumnConfig<Item>;

export type TableColumnSorting<Item> = {
  column: TableColumn<Item>;
  ascending: boolean;
}

export type TableSorting<Item> = {
  sorting: TableColumnSorting<Item>;
  sorter: Sorter<Item>;
  toggleSorting: (column: TableColumn<Item>) => void;
};

export type TableProps<Item> = CommonProps & {
  data: Item[];
  columns: TableColumn<Item>[];
  pageSize?: number;
  showFilters?: boolean;
}

export type TableFilterProps<Item> = CommonProps & {
  filter: Filter<Item> | undefined;
  filters: Filters<Item>;
}
