import {ItemSimpleValue, ItemValue, LinkValue} from "./types";

export function isUndefined(value: unknown): boolean {
  return value === undefined || value === null;
}

export function isNumber(value: unknown): boolean {
  return !Number.isNaN(Number(value));
}

export function isLinkValue(value: unknown): value is LinkValue {
  return (typeof value === 'object' && ('value' in value) && typeof (value as any).value === 'string');
}

export function isItemValue(value: unknown): value is ItemValue {
  return isUndefined(value) ||
    typeof value === 'string' ||
    typeof value === 'number' ||
    typeof value === 'boolean' ||
    isLinkValue(value);
}

export function getSimpleValue(value: ItemValue): ItemSimpleValue {
  if (isLinkValue(value)) {
    return value.value;
  }
  return value;
}

export function asArray<Item>(value: Item | Item[] | undefined): Item[] {
  if (isUndefined(value)) {
    return [];
  }
  if (Array.isArray(value)) {
    return value;
  }
  return [value];
}
