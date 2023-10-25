import {useCallback, useMemo} from 'react';
import type {ItemValue, ItemValueFn, LinkValue} from './types';
import {isUndefined, isItemValue, isNumber} from "./utilities";

export type Sorter<Item> = (a: Item, b: Item) => number;

function defaultSorter(
  aValue: ItemValue,
  bValue: ItemValue,
): number {
  if (isUndefined(aValue) && isUndefined(bValue)) {
    return 0;
  }
  if (isUndefined(aValue)) {
    return 1;
  }
  if (isUndefined(bValue)) {
    return -1;
  }
  if (typeof aValue !== typeof bValue) {
    return 0;
  }
  if (typeof aValue === 'string') {
    if (isNumber(aValue) && isNumber(bValue)) {
      return (Number(aValue) - Number(bValue));
    }
    return (aValue as string).toUpperCase().localeCompare((bValue as string).toUpperCase());
  }
  if (typeof aValue === 'number') {
    return (aValue - (bValue as number));
  }
  if (typeof aValue === 'boolean') {
    return (Number(aValue) - Number(bValue));
  }
  const {value: aa} = aValue as LinkValue;
  const {value: bb} = bValue as LinkValue;
  return defaultSorter(aa, bb);
}

export function buildSorter<Item>(valueFn: ItemValueFn<Item> | undefined): Sorter<Item> {
  return function (a: Item, b: Item) {
    if (valueFn === undefined) {
      return 0;
    }
    const aValue = valueFn(a);
    const bValue = valueFn(b);
    if (!isItemValue(aValue) && !isItemValue(bValue)) {
      return 0;
    }
    if (!isItemValue(aValue)) {
      return -1;
    }
    if (!isItemValue(bValue)) {
      return 1;
    }
    return defaultSorter(aValue as ItemValue, bValue as ItemValue);
  }
}

function createSorter<Item>(sorter: Sorter<Item>, ascending: boolean): Sorter<Item> {
  return function (a: Item, b: Item) {
    return sorter(a, b) * (ascending ? 1 : -1);
  };
}

export function useSortedData<Item>(data: Item[], sorter: Sorter<Item>, ascending = true): Item[] {
  const sortCallback = useCallback<Sorter<Item>>(createSorter(sorter, ascending), [
    sorter,
    ascending,
  ]);
  return useMemo<Item[]>(() => data.slice().sort(sortCallback), [data, sortCallback]);
}
