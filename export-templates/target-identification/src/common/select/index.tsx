import React, {
  Children,
  useCallback,
  useEffect,
  useState,
  useRef,
} from 'react';
import type {
  ReactNode,
  TargetedEvent,
} from 'react';
import {XMarkIcon, ChevronDownIcon, CheckIcon} from '@heroicons/react/24/outline';
import classNames from 'classnames';
import type {CommonParentProps} from '../types';
import {useMemo} from 'react';
import {asArray, isLinkValue, isUndefined} from '../../model/utilities';

type SelectCommonProps<Item> = CommonParentProps & {
  filterProps?: (item: Item, filter: string) => boolean;
  bordered?: boolean;
};

type SelectSingleProps<Item> = SelectCommonProps<Item> & {
  multiple?: false;
  allowClear?: boolean;
  value: Item | undefined;
  onChange: (value: Item | undefined) => void;
}

type SelectMultipleProps<Item> = SelectCommonProps<Item> & {
  multiple: true;
  values: Item[] | undefined;
  onChange: (values: Item[]) => void;
}

export type SelectProps<Item> = SelectSingleProps<Item> |
  SelectMultipleProps<Item>;

export type SelectOptionProps<Item> = CommonParentProps & {
  key: string;
  value: Item;
}

export type SelectOptionPropsExtended<Item> = SelectOptionProps<Item> & {
  selected?: boolean;
  hovered?: boolean;
  onClick?: () => void;
  onHover?: () => void;
  index: number;
}

function Option<Item>(props: SelectOptionProps<Item>) {
  const {
    selected,
    hovered,
    onClick,
    onHover,
    index,
  } = props as SelectOptionPropsExtended<Item>;
  const onMouseDown = useCallback((event: MouseEvent): void => {
    if (event.button === 0) {
      event.preventDefault();
      event.stopPropagation();
      if (typeof onClick === 'function') {
        onClick();
      }
    }
  }, [onClick]);
  return (
    <div
      className={classNames(
        'px-1',
        'py-1',
        'h-6',
        'flex',
        'hover:bg-slate-100',
        {
          'bg-slate-100': selected,
          'bg-slate-200': hovered,
        },
        'cursor-pointer'
      )}
      onMouseDown={onMouseDown}
      onMouseOver={onHover}
      data-index={index}
    >
      <p className="truncate">{props.children}</p>
      {selected && (
        <CheckIcon className="ml-auto w-4 h-4" />
      )}
    </div>
  );
}

function getChildSelectOptionProps<Item>(aChild: ReactNode): SelectOptionProps<Item> | undefined {
  if (
    typeof aChild === 'object' &&
    'props' in aChild &&
    aChild.props &&
    'value' in aChild.props
  ) {
    return aChild.props as SelectOptionProps<Item>;
  }
  return undefined;
}

function getSelectOptionProps<Item>(children: ReactNode, item: Item): SelectOptionProps<Item> | undefined {
  let result: SelectOptionProps<Item> | undefined;
  Children.forEach(children, (aChild) => {
    const props = getChildSelectOptionProps(aChild);
    if (props && props.value === item) {
      result = props as SelectOptionProps<Item>;
    }
  });
  return result;
}

function getItems<Item>(children: ReactNode): Item[] {
  return Children.toArray(children)
    .map((child) => getChildSelectOptionProps<Item>(child))
    .filter((Boolean))
    .map((props) => props.value);
}

type OptionsProps<Item> = {
  selected: Item[];
  hovered?: Item | undefined;
  onClick: (item: Item) => void;
  onHover: (item: Item) => void;
}

function getOptionsItems<Item>(
  children: ReactNode,
  items: Item[],
  options: OptionsProps<Item>
): ReactNode[] {
  return Children.toArray(children)
    .map((child) => getChildSelectOptionProps<Item>(child))
    .filter((Boolean))
    .filter((props) => items.includes(props.value))
    .map((props) => React.createElement(
      Option, {
        ...props,
        selected: options.selected.includes(props.value),
        hovered: props.value && options.hovered === props.value,
        onClick: () => options.onClick(props.value),
        onHover: () => options.onHover(props.value),
        index: items.indexOf(props.value),
      } as SelectOptionPropsExtended<Item>));
}

type SelectInputProps<Item> = CommonParentProps & {
  filter: string;
  values: Item[];
  open: boolean;
  removable: boolean;
  hovered: Item | undefined;
  onChangeFilter: (filter: string) => void;
  onClick?: () => void;
  onBlur?: () => void;
  onRemove: (item: Item) => void;
  onRemoveAll: () => void;
  onEnter: () => void;
  onDownKey?: () => void;
  onUpKey?: () => void;
}

function SelectSingleInput<Item>(props: SelectInputProps<Item>) {
  const {
    className,
    style,
    children,
    removable,
    values,
    filter,
    open,
    onClick,
    onBlur,
    onRemove,
    onRemoveAll,
    onChangeFilter,
    onEnter,
    onDownKey,
    onUpKey,
  } = props;
  const value = values.length > 0 ? values[0] : undefined;
  const input = useRef<HTMLInputElement | null>(null);
  useEffect(() => {
    if (input.current && open) {
      input.current.focus();
    } else if (input.current) {
      input.current.blur();
    }
  }, [input, open]);
  const displayValue = useMemo(() => {
    const p = getSelectOptionProps(children, value);
    if (p) {
      return p.children;
    }
    return undefined;
  }, [children, value, onRemove]);
  const onKey = useCallback((event: KeyboardEvent) => {
    switch ((event.key ?? '').toLowerCase()) {
      case 'escape':
        (event.target as HTMLInputElement).blur();
        break;
      case 'enter':
        onEnter();
        break;
      case 'arrowup':
        if (typeof onUpKey === 'function') {
          event.preventDefault();
          event.stopPropagation();
          onUpKey();
        }
        break;
      case 'arrowdown':
        if (typeof onDownKey === 'function') {
          event.preventDefault();
          event.stopPropagation();
          onDownKey();
        }
        break;
      default:
        break;
    }
  }, [
    onBlur,
    onEnter,
    onDownKey,
    onUpKey,
    values,
    filter
  ]);
  const onInputChange = useCallback((event: TargetedEvent<HTMLInputElement, Event>) => {
    onChangeFilter((event.target as HTMLInputElement).value);
  }, [onChangeFilter]);
  return (
    <div className={classNames('flex items-center justify-start', className)} style={style}>
      <div
        className={
          classNames(
            'absolute',
            'select-none',
            'w-full',
            'outline-0',
            'border-0',
            'h-full',
            'px-1',
            'flex',
            'items-center',
            'z-0',
            {
              'opacity-50': open,
              hidden: filter.length > 0
            }
          )
        }
      >
        <p className="truncate">{displayValue}</p>
      </div>
      <div className="flex-1 z-10 basis-1/2">
        <input
          className="w-full outline-0 border-0 h-6 px-1 bg-transparent"
          ref={input}
          onClick={onClick}
          onBlur={onBlur}
          onKeyDown={onKey}
          value={filter}
          onChange={onInputChange}
          style={{minWidth: '50%'}}
        />
      </div>
      {
        removable && (
          <XMarkIcon
            className={classNames(
              'mx-1',
              'w-4',
              'h-4',
              'z-10',
              {
                'cursor-pointer': value,
                'opacity-25': !value
              }
            )}
            onClick={onRemoveAll}
          />
        )
      }
      {
        !removable && (
          <ChevronDownIcon className={classNames('mx-1', 'w-3', 'h-3', 'z-10', 'transition-transform', {
            'rotate-180': open,
            transform: open,
          })} />
        )
      }
    </div>
  );
}

function SelectMultipleInput<Item>(props: SelectInputProps<Item>) {
  const {
    className,
    style,
    children,
    values,
    filter,
    open,
    onClick,
    onBlur,
    onRemove,
    onRemoveAll,
    onChangeFilter,
    onEnter,
    onDownKey,
    onUpKey,
  } = props;
  const input = useRef<HTMLInputElement | null>(null);
  useEffect(() => {
    if (input.current && open) {
      input.current.focus();
    } else if (input.current) {
      input.current.blur();
    }
  }, [input, open]);
  const displayValues = useMemo(() => values
    .map((v) => getSelectOptionProps(children, v))
    .filter(Boolean)
    .map((props, id) => (
      <div key={`display-value-${id}`} className="bg-slate-200 px-1 rounded inline-flex items-center mr-1 my-1 md-1">
        {props.children}
        <XMarkIcon
          className="w-3 h-3 ml-1 cursor-pointer"
          onClick={() => onRemove(props.value)}
        />
      </div>
    )), [children, values, onRemove]);
  const onKey = useCallback((event: KeyboardEvent) => {
    switch ((event.key ?? '').toLowerCase()) {
      case 'backspace':
        if (filter.length === 0 && values.length > 0) {
          onRemove(values[values.length - 1]);
        }
        break;
      case 'escape':
        (event.target as HTMLInputElement).blur();
        break;
      case 'enter':
        onEnter();
        break;
      case 'arrowup':
        if (typeof onUpKey === 'function') {
          onUpKey();
        }
        break;
      case 'arrowdown':
        if (typeof onDownKey === 'function') {
          onDownKey();
        }
        break;
      default:
        break;
    }
  }, [
    onBlur,
    onRemove,
    onEnter,
    onDownKey,
    onUpKey,
    values,
    filter
  ]);
  const onInputChange = useCallback((event: TargetedEvent<HTMLInputElement, Event>) => {
    onChangeFilter((event.target as HTMLInputElement).value);
  }, [onChangeFilter]);
  return (
    <div className={classNames('flex items-center justify-start', className)} style={style}>
      <div className="flex flex-1 flex-wrap items-center justify-start px-1">
        {displayValues}
        <div className="flex-1 z-10 basis-1/2">
          <input
            className="w-full outline-0 border-0 h-6 px-1 bg-transparent"
            ref={input}
            onClick={onClick}
            onBlur={onBlur}
            onKeyDown={onKey}
            value={filter}
            onChange={onInputChange}
            style={{minWidth: '50%'}}
          />
        </div>
      </div>
      {
        values.length > 0 && (
          <XMarkIcon
            className={classNames(
              'ml-1',
              'w-4',
              'h-4',
              'z-10',
              'cursor-pointer',
            )}
            onClick={onRemoveAll}
          />
        )
      }
      <ChevronDownIcon className={classNames('mx-1', 'w-3', 'h-3', 'transition-transform', {
        'rotate-180': open,
        transform: open,
      })} />
    </div>
  );
}

function SelectInput<Item>(props: SelectInputProps<Item> & {multiple?: boolean;}) {
  const {multiple = false} = props;
  if (multiple) {
    return React.createElement(SelectMultipleInput, props);
  }
  return React.createElement(SelectSingleInput, props);
}

function FixedPosition(props: CommonParentProps) {
  const {className, style, children} = props;
  const containerRef = useRef<HTMLDivElement | null>(null);
  const childRef = useRef<HTMLDivElement | null>(null);
  useEffect(() => {
    let x, y, w, raf;
    const check = () => {
      if (containerRef.current && childRef.current) {
        const box = containerRef.current.getBoundingClientRect();
        if (x !== box.left || y !== box.top || w !== box.width) {
          x = box.left;
          y = box.top;
          w = box.width;
          childRef.current.style.display = 'block';
          childRef.current.style.top = `${y}px`;
          childRef.current.style.left = `${x}px`;
          childRef.current.style.width = `${w}px`;
        }
      } else {
        x = undefined;
        y = undefined;
      }
      raf = requestAnimationFrame(check);
    };
    check();
    return () => cancelAnimationFrame(raf);
  }, [containerRef, childRef]);
  return (
    <div ref={containerRef} className={classNames(className)} style={style}>
      <div ref={childRef} className="fixed hidden">
        {children}
      </div>
    </div>
  );
}

function useSliced<T>(array: T[], chunk = 50): {
  sliced: T[];
  hasMore: boolean;
  loadMore: () => void;
  onScroll: (event: UIEvent) => void;
  reset: () => void;
} {
  const [slice, setSlice] = useState(chunk);
  const loadMore = useCallback(() => {
    setSlice((curr) => Math.min(curr + chunk, array.length));
  }, [chunk, setSlice, array]);
  useEffect(() => {
    setSlice(chunk);
  }, [array, chunk, setSlice]);
  const sliced = useMemo(() => array.slice(0, slice), [array, slice]);
  const hasMore = sliced.length < array.length;
  const onScroll = useCallback((event: UIEvent) => {
    if (event.target instanceof HTMLDivElement) {
      const div = event.target;
      if (div.scrollTop + div.clientHeight + 15 >= div.scrollHeight && hasMore) {
        loadMore();
      }
    }
  }, [loadMore, hasMore]);
  const reset = useCallback(() => setSlice(chunk), [setSlice, chunk]);
  return useMemo(() => ({
    sliced,
    hasMore,
    onScroll,
    loadMore,
    reset,
  }), [
    sliced,
    hasMore,
    onScroll,
    loadMore,
    reset
  ]);
}

function Select<Item>(props: SelectProps<Item>) {
  const {
    className,
    style,
    children,
    multiple,
    onChange,
    filterProps,
    bordered,
  } = props;
  const [isOpen, setIsOpen] = useState(false);
  const [filter, setFilter] = useState('');
  const [hovered, onHover] = useState<Item | undefined>(undefined);
  const onChangeFilter = setFilter;
  const value = multiple === true ? props.values : props.value;
  const values = useMemo(() => asArray(value), [value]);
  const list = useMemo(() => getItems<Item>(children), [children]);
  const onInputClick = useCallback(() => setIsOpen(!isOpen), [setIsOpen, isOpen]);
  const onInputBlur = useCallback(() => setIsOpen(false), [setIsOpen]);
  const filterItem = useCallback((item: Item): boolean => {
    if (filter.length === 0) {
      return true;
    }
    if (isUndefined(item)) {
      return false;
    }
    if (typeof filterProps === 'function') {
      return filterProps(item, filter);
    }
    if (
      typeof item === 'object' &&
      isLinkValue(item)
    ) {
      return (item.value as string).toLowerCase().includes(filter.toLowerCase());
    }
    if (typeof item === 'object') {
      return false;
    }
    return `${item}`.toLowerCase().includes(filter.toLowerCase());
  }, [filter, filterProps]);
  const filtered = useMemo(() => list.filter(filterItem), [filterItem, list]);
  const scroller = useRef<HTMLDivElement | null>(null);
  const {
    sliced,
    hasMore,
    onScroll,
    reset,
  } = useSliced(filtered);
  useEffect(() => {
    onHover((curr) => sliced.includes(curr) ? curr : undefined);
  }, [sliced, onHover]);
  const onChangeValues = useCallback((newValues: Item[]): void => {
    if (multiple === true) {
      onChange(newValues);
    } else {
      onChange(newValues.length === 1 ? newValues[0] : undefined);
    }
  }, [onChange, multiple]);
  const toggleItem = useCallback((item: Item): void => {
    if (multiple === true && values.includes(item)) {
      onChangeValues(values.filter((i) => i !== item));
    } else if (multiple === true) {
      onChangeValues([...values, item]);
    } else {
      onChangeValues([item]);
      setIsOpen(false);
    }
    setFilter('');
  }, [values, onChangeValues, multiple, setIsOpen, setFilter]);
  const onEnter = useCallback(() => {
    if (hovered) {
      toggleItem(hovered);
    } else if (filtered.length === 1) {
      if (multiple) {
        const newValues = [...(new Set([...values, ...filtered]))];
        onChangeValues(newValues);
      } else {
        onChangeValues(filtered);
      }
    }
    onChangeFilter('');
  }, [
    onChangeValues,
    multiple,
    filterItem,
    values,
    filtered,
    onChangeFilter,
    setIsOpen,
    hovered,
    toggleItem,
  ]);
  const onRemove = useCallback((item: Item) => {
    onChangeValues(values.filter((i) => i !== item));
  }, [values, onChangeValues]);
  useEffect(() => {
    if (!isOpen) {
      onChangeFilter('');
      reset();
    }
  }, [onChangeFilter, isOpen, reset]);
  const onDown = useCallback(() => {
    if (sliced.length === 0) {
      onHover(undefined);
      return;
    }
    const index = sliced.indexOf(hovered);
    if (index >= 0 && index < sliced.length - 1) {
      onHover(sliced[index + 1]);
    } else {
      onHover(sliced[0]);
    }
  }, [hovered, sliced, onHover]);
  const onUp = useCallback(() => {
    if (sliced.length === 0) {
      onHover(undefined);
      return;
    }
    const index = sliced.indexOf(hovered);
    if (index >= 1) {
      onHover(sliced[index - 1]);
    } else {
      onHover(sliced.slice().pop());
    }
  }, [hovered, sliced, onHover]);
  useEffect(() => {
    if (scroller.current) {
      const index = sliced.indexOf(hovered);
      if (index >= 0) {
        for (const child of scroller.current.children) {
          if (child instanceof HTMLElement && child.dataset && Number(child.dataset.index) === index) {
            scroller.current.scrollTo(0, child.offsetTop - scroller.current.clientHeight / 2.0);
          }
        }
      }
    }
  }, [scroller.current, hovered, sliced]);
  const onRemoveAll = useCallback(() => onChangeValues([]), [onChangeValues]);
  return (
    <div className="relative">
      <SelectInput
        className={classNames(
          className,
          {
            'border': bordered,
            'rounded-t': bordered,
            'rounded-b': bordered && !isOpen,
            'border-b-transparent': bordered && isOpen,
          }
        )}
        style={style}
        values={values}
        multiple={multiple}
        filter={filter}
        open={isOpen}
        hovered={hovered}
        removable={multiple === true || props.allowClear}
        onClick={onInputClick}
        onBlur={onInputBlur}
        onChangeFilter={onChangeFilter}
        onEnter={onEnter}
        onRemove={onRemove}
        onRemoveAll={onRemoveAll}
        onDownKey={onDown}
        onUpKey={onUp}
      >
        {children}
      </SelectInput>
      {
        isOpen && (
          <FixedPosition className="relative w-full z-20">
            <div
              className={classNames('max-h-60 bg-white overflow-auto py-1 shadow-lg border-t-0', {
                border: bordered,
                'rounded-b': bordered,
              })}
              ref={scroller}
              onScroll={onScroll}
            >
              {
                getOptionsItems(children, sliced, {
                  selected: values,
                  onClick: toggleItem,
                  hovered,
                  onHover: () => onHover(undefined),
                })
              }
              {
                hasMore && (
                  <div className="h-6 flex items-center justify-center italic text-gray-500">
                    Loading...
                  </div>
                )
              }
            </div>
          </FixedPosition>
        )
      }
    </div>
  );
}

Select.Option = Option;

export default Select;
