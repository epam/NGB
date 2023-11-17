import {useCallback, useEffect, useMemo, useState} from "react";
import type {ItemProperty, ItemValue, ItemSimpleValue, ItemValueFn} from './types';
import {getSimpleValue, isNumber, isUndefined} from "./utilities";

export enum FilterType {
  number = 'number',
  list = 'list',
  unknown = 'unknown',
}

export type PropertyFilter<Item> = ItemProperty<Item>;

export type PropertyFilterConfig<Item> = {
  value: ItemValueFn<Item>;
  type?: FilterType;
}

export type FilterConfig<Item> = PropertyFilter<Item> | PropertyFilterConfig<Item> | false;

export type FilterInfoBase<Item, Type extends FilterType> = {
  config: FilterConfig<Item>;
  type: Type;
}

export type ListFilterValue = {
  values: ItemSimpleValue[];
}

export type ListFilterInfo<Item> = FilterInfoBase<Item, FilterType.list> & {
  list?: ItemSimpleValue[];
}

export type ListFilter<Item> = ListFilterValue & ListFilterInfo<Item>;

export type NumberFilterValue = {
  from?: number;
  to?: number;
}

export type NumberFilterInfo<Item> = FilterInfoBase<Item, FilterType.number> & {
  minimum: number;
  maximum: number;
}

export type NumberFilter<Item> = NumberFilterValue & NumberFilterInfo<Item>;

export type UnknownFilterInfo<Item> = FilterInfoBase<Item, FilterType.unknown>;
export type UnknownFilterValue = {};
export type UnknownFilter<Item = any> = UnknownFilterInfo<Item> & UnknownFilterValue;

export type Filter<Item> = ListFilter<Item> | NumberFilter<Item> | UnknownFilter<Item>;

function getValueFn<Item>(property: ItemProperty<Item> | ItemValueFn<Item>): ItemValueFn<Item> {
  return typeof property === 'function'
    ? property
    : ((item: Item): ItemValue => item[property] as ItemValue);
}

function getItemPropertyFilterType<Item>(data: Item[], property: ItemProperty<Item> | ItemValueFn<Item>): FilterType {
  const valueFn: ItemValueFn<Item> = getValueFn(property);
  const values = data.map((item) => valueFn(item)).filter((v) => !isUndefined(v));
  if (!values.some((item) => typeof item !== 'number')) {
    return FilterType.number;
  }
  return FilterType.list;
}

function getFilterValueFn<Item>(filter: FilterConfig<Item>): ItemValueFn<Item> {
  if (typeof filter === 'boolean') {
    return () => undefined;
  }
  if (typeof filter === 'string') {
    return getValueFn(filter);
  }
  return getValueFn(filter.value);
}

function getFilterSuggestedType<Item>(data: Item[], filter: FilterConfig<Item>): FilterType {
  if (typeof filter === 'boolean') {
    return FilterType.unknown;
  } else if (typeof filter === 'string') {
    return getItemPropertyFilterType(data, filter);
  }
  return filter.type ?? getItemPropertyFilterType(data, filter.value);
}

function buildFilter<Item>(data: Item[], filter: FilterConfig<Item>): Filter<Item> {
  let type: FilterType = getFilterSuggestedType(data, filter);
  const valueFn: ItemValueFn<Item> = getFilterValueFn(filter);
  const extractRealValue = (item: Item): ItemSimpleValue => getSimpleValue(valueFn(item));
  const list: ItemSimpleValue[] = [...new Set(data.map(extractRealValue))];
  if (list.some((n) => !isNumber(n))) {
    type = FilterType.list;
  }
  switch (type) {
    case FilterType.number:
      const numbers = list.map((n) => Number(n));
      let min = 0;
      let max = 1;
      for (const n of numbers) {
        if (min > n) {
          min = n;
        }
        if (max < n) {
          max = n;
        }
      }
      return {
        type,
        maximum: max,
        minimum: min,
        config: filter,
      };
    case FilterType.list:
      return {
        type,
        list,
        values: [],
        config: filter,
      };
    default:
      return {
        type: FilterType.unknown,
        config: filter,
      }
  }
}

export type Filters<Item> = {
  filteredData: Item[];
  filters: Filter<Item>[];
  clear: () => void;
  clearFilter: (filter: Filter<Item>) => void;
  setListFilterValues: (filter: ListFilter<Item>, values: ItemSimpleValue[]) => void;
  setNumberFilterFrom: (filter: NumberFilter<Item>, from?: number | undefined) => void;
  setNumberFilterTo: (filter: NumberFilter<Item>, to?: number | undefined) => void;
}

function setListFilterSelectedValues<Item>(filter: ListFilter<Item>, values: ItemSimpleValue[]): ListFilter<Item> {
  const {
    values: current,
    ...rest
  } = filter;
  return {
    ...rest,
    values,
  };
}

function setNumberFilterFromValue<Item>(filter: NumberFilter<Item>, from: number | undefined): NumberFilter<Item> {
  const {
    from: current,
    ...rest
  } = filter;
  return {
    ...rest,
    from,
  };
}

function setNumberFilterToValue<Item>(filter: NumberFilter<Item>, to: number | undefined): NumberFilter<Item> {
  const {
    to: current,
    ...rest
  } = filter;
  return {
    ...rest,
    to,
  };
}

function clearFilterValues<Item>(filter: Filter<Item>): Filter<Item> {
  switch (filter.type) {
    case FilterType.list:
      return setListFilterSelectedValues(filter, []);
    case FilterType.number:
      return setNumberFilterFromValue(
        setNumberFilterToValue(filter, undefined),
        undefined,
      );
    default:
      return filter;
  }
}

function filterDataItemUsingNumberFilter<Item>(item: Item, filter: NumberFilter<Item>): boolean {
  const valueFn = getFilterValueFn(filter.config);
  const value = valueFn(item);
  if (typeof value !== 'number') {
    return true;
  }
  const {
    from,
    to,
  } = filter;
  return (isUndefined(from) || from <= value) &&
    (isUndefined(to) || to >= value);
}

function filterDataItemUsingListFilter<Item>(item: Item, filter: ListFilter<Item>): boolean {
  const valueFn = getFilterValueFn(filter.config);
  const value = getSimpleValue(valueFn(item));
  const {
    values
  } = filter;
  return values.length === 0 || values.includes(value);
}

function filterDataItem<Item>(item: Item, filter: Filter<Item>): boolean {
  switch (filter.type) {
    case FilterType.number:
      return filterDataItemUsingNumberFilter(item, filter);
    case FilterType.list:
      return filterDataItemUsingListFilter(item, filter);
    default:
      return true;
  }
}

function filterData<Item>(data: Item[], filters: Filter<Item>[]): Item[] {
  let result = data.slice();
  for (let f = 0; f < filters.length; f += 1) {
    const filter = filters[f];
    result = result.filter((item) => filterDataItem(item, filter));
  }
  return result;
}

export function isFilterSpecified<Item>(filter: Filter<Item> | undefined): boolean {
  if (!filter) {
    return false;
  }
  switch (filter.type) {
    case FilterType.list:
      return filter.values && filter.values.length > 0;
    case FilterType.number:
      return !isUndefined(filter.from) || !isUndefined(filter.to);
    default:
      return false;
  }
}

export function useFilteredData<Item>(data: Item[], filtersConfig: FilterConfig<Item>[]): Filters<Item> {
  const [filters, setFilters] = useState<Filter<Item>[]>([]);
  const builtFilters = useMemo<Filter<Item>[]>(
    () => filtersConfig.map((filter) => buildFilter(data, filter)),
    [data, filtersConfig],
  );
  useEffect(() => {
    setFilters(builtFilters);
  }, [builtFilters, setFilters]);
  const setListFilterValues = useCallback((filter: ListFilter<Item>, values: ItemSimpleValue[]) => {
    setFilters((current) => current.map((f) => f === filter
      ? setListFilterSelectedValues(f, values)
      : f));
  }, [setFilters]);
  const setNumberFilterFrom = useCallback((filter: NumberFilter<Item>, from?: number | undefined) => {
    setFilters((current) => current.map((f) => f === filter
      ? setNumberFilterFromValue(f, from)
      : f))
  }, [filters, setFilters]);
  const setNumberFilterTo = useCallback((filter: NumberFilter<Item>, to?: number | undefined) => {
    setFilters((current) => current.map((f) => f === filter
      ? setNumberFilterToValue(f, to)
      : f))
  }, [setFilters]);
  const clearFilter = useCallback((filter: Filter<Item>) => {
    setFilters((current) => current.map((f) => f === filter ? clearFilterValues(f) : f))
  }, [setFilters]);
  const clear = useCallback(() => {
    setFilters((current) => current.map(clearFilterValues));
  }, [setFilters]);
  const merged = useMemo(
    () => builtFilters
      .map((f) => filters.find((ff) => ff.config === f.config) ?? f),
    [builtFilters, filters],
  )
  return useMemo(() => ({
    filteredData: filterData(data, merged),
    filters: merged,
    setListFilterValues,
    setNumberFilterFrom,
    setNumberFilterTo,
    clearFilter,
    clear,
  }), [
    data,
    merged,
    setListFilterValues,
    setNumberFilterFrom,
    setNumberFilterTo,
    clearFilter,
    clear,
  ]);
}
