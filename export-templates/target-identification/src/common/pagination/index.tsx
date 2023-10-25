import classNames from 'classnames';
import type {CommonProps} from '../types';
import {VerticalAlign} from '../types';
import {PagedData} from '../../model/types';
import {useCallback, useMemo} from "react";

export type PaginationProps = CommonProps & {
  state: PagedData<any>;
  align?: VerticalAlign;
}

type PaginationButtonProps = {
  state: PagedData<any>;
  page: number;
}

function PaginationButton(props: PaginationButtonProps) {
  const {state, page} = props;
  const {setPage} = state;
  const onClick = useCallback(() => setPage(page), [setPage, page]);
  return (
    <button className={classNames(
      'mr-1',
      'hover:underline',
      {
        'font-bold': state.page === page,
      }
    )} onClick={onClick}>
      {page + 1}
    </button>
  );
}

export default function Pagination(props: PaginationProps) {
  const {
    state,
    className,
    style,
    align = VerticalAlign.center,
  } = props;
  const {
    page,
    pagesCount,
  } = state;
  if (pagesCount < 2) {
    return null;
  }
  const renderGoToFirst = page > 2;
  const renderGoToLast = page + 2 < pagesCount - 1;
  const pages = useMemo<number[]>(() => {
    const result: number[] = [];
    const start = Math.max(0, page - 2);
    const end = Math.min(pagesCount - 1, page + 2);
    for (let p = start; p <= end; p += 1) {
      result.push(p);
    }
    return result;
  }, [page, pagesCount]);
  return (
    <div
      className={classNames(
        className,
        'w-full',
        'flex',
        'align-center',
        {
          'justify-start': align === VerticalAlign.left,
          'justify-center': align === VerticalAlign.center,
          'justify-end': align === VerticalAlign.right
        }
      )}
      style={style}
    >
      <div className="flex text-sm">
        {
          renderGoToFirst && (
            <>
              <PaginationButton state={state} page={0} />
              <span className="mr-1">
                ...
              </span>
            </>
          )
        }
        {
          pages.map((p) => (
            <PaginationButton state={state} page={p} />
          ))
        }
        {
          renderGoToLast && (
            <>
              <span className="mr-1">...</span>
              <PaginationButton state={state} page={pagesCount - 1} />
            </>
          )
        }
      </div>
    </div>
  );
}
