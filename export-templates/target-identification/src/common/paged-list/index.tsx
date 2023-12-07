import type {ReactNode} from 'react';
import classNames from 'classnames';
import {CommonProps} from '../types';
import {usePagedData} from '../../model/paged-data';
import Pagination from '../pagination';

export type PagedListProps<Item> = CommonProps & {
  pageSize?: number;
  data: Item[];
  itemRenderer: (item: Item) => ReactNode;
}

export default function PagedList<Item>(props: PagedListProps<Item>) {
  const {
    className,
    style,
    data,
    itemRenderer,
    pageSize,
  } = props;
  const pagedData = usePagedData(data, pageSize);
  const {
    data: array,
    page,
    pageSize: pagedDataPageSize,
  } = pagedData;
  const firstItemIndex = page * pagedDataPageSize;
  return (
    <div
      className={classNames(className)}
      style={style}
    >
      {
        array.map((item, idx) => (
          <div key={`item-${idx + firstItemIndex}`}>
            {itemRenderer(item)}
          </div>
        ))
      }
      {
        data.length === 0 && (
          <div className="w-full text-center my-1 text-slate-500">
            No data
          </div>
        )
      }
      <div className="w-full flex items-center justify-center">
        <Pagination state={pagedData} />
      </div>
    </div>
  );
}
