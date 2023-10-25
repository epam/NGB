import {TableFilterProps} from "./types";
import {FilterType, ListFilter, NumberFilter} from "../../model/filtered-data";
import {useCallback, useMemo} from "react";
import {ItemSimpleValue} from "../../model/types";
import {isNumber} from "../../model/utilities";
import Select from "../select";
import DisplayValue from "../display-value";
import classNames from "classnames";

const empty = [];

export default function TableColumnFilter<Item>(props: TableFilterProps<Item>) {
  const {
    className,
    style,
    filter,
    filters,
  } = props;
  const {
    setNumberFilterTo,
    setNumberFilterFrom,
    setListFilterValues,
  } = filters;
  const {
    numberFilter,
    listFilter,
  } = useMemo(() => {
    if (filter && filter.type === FilterType.number) {
      return {numberFilter: filter as NumberFilter<Item>};
    }
    if (filter && filter.type === FilterType.list) {
      return {listFilter: filter as ListFilter<Item>};
    }
    return {
    };
  }, [filter]);
  const onListValuesChange = useCallback((values: ItemSimpleValue[]) => {
    if (listFilter) {
      setListFilterValues(listFilter, values);
    }
  }, [listFilter, setListFilterValues]);
  const onFromChange = useCallback((event) => {
    if (numberFilter) {
      if (`${event.target.value}` === '') {
        setNumberFilterFrom(numberFilter);
      } else if (isNumber(event.target.value)) {
        setNumberFilterFrom(numberFilter, Number(event.target.value as string));
      }
    }
  }, [numberFilter, setNumberFilterFrom]);
  const onToChange = useCallback((event) => {
    if (numberFilter) {
      if (`${event.target.value}` === '') {
        setNumberFilterTo(numberFilter);
      } else if (isNumber(event.target.value)) {
        setNumberFilterTo(numberFilter, Number(event.target.value as string));
      }
    }
  }, [numberFilter, setNumberFilterTo]);
  const {
    from,
    to,
  } = numberFilter ?? {};
  const {
    values = empty,
    list = empty,
  } = listFilter ?? {};
  const listChildren = useMemo(() => {
    return list.map((value, idx) => (
      <Select.Option key={`item-${idx}`} value={value}>
        <DisplayValue value={value} />
      </Select.Option>
    ));
  }, [list]);
  const inputClassName = classNames(
    'flex-1 basis-20 grow shrink mx-1 px-1 py-1 outline-0 bg-white border rounded',
  );
  return (
    <div
      className={classNames(className, 'relative')}
      style={style}
    >
      {
        (!filter || filter.type === FilterType.unknown) && '\u00A0'
      }
      {
        filter && filter.type === FilterType.list && (
          <Select
            multiple
            className="w-full bg-white"
            values={values}
            onChange={onListValuesChange}
            bordered
          >
            {listChildren}
          </Select>
        )
      }
      {
        filter && filter.type === FilterType.number && (
          <div className="w-full flex items-center">
            <span>From</span>
            <input
              className={inputClassName}
              type="number"
              onChange={onFromChange}
              value={from}
            />
            <span>To</span>
            <input
              className={inputClassName}
              type="number"
              onChange={onToChange}
              value={to}
            />
          </div>
        )
      }
    </div>
  );
}
