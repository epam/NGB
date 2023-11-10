import {PagedData, PagedDataState} from "./types";
import {useCallback, useEffect, useMemo, useReducer} from "react";

const defaultPageSize = 10;

type PagedDataSource<Item> = Omit<PagedDataState<Item>, 'nextPageAvailable' | 'prevPageAvailable' | 'pagesCount'>

function init<Item>(pageSize: number): PagedDataSource<Item> {
  return {
    data: [],
    page: 0,
    pageSize,
  };
}

enum PageActionType {
  setData = 'set-data',
  setPage = 'set-page',
  nextPage = 'next-page',
  prevPage = 'prev-page',
}

type SetDataAction<Item> = { type: PageActionType.setData, data: Item[], pageSize: number; };
type SetPageAction = { type: PageActionType.setPage, page: number; };
type NavigateToPageAction<Type extends (PageActionType.nextPage | PageActionType.prevPage)> = {
  type: Type;
};

type PageAction<Item> = SetPageAction |
  SetDataAction<Item> |
  NavigateToPageAction<PageActionType.prevPage> |
  NavigateToPageAction<PageActionType.nextPage>;

function getPagesCount<Item>(state: PagedDataSource<Item>): number {
  return Math.ceil(state.data.length / state.pageSize);
}

function correctPage<Item>(state: PagedDataSource<Item>, page: number): number {
  const pagesCount = getPagesCount(state);
  return Math.max(0, Math.min(pagesCount - 1, page));
}

function reducer<Item>(state: PagedDataSource<Item>, action: PageAction<Item>): PagedDataSource<Item> {
  switch (action.type) {
    case PageActionType.setData:
      return {
        ...state,
        page: 0,
        pageSize: action.pageSize,
        data: action.data,
      };
    case PageActionType.setPage: {
      const pageCorrected = correctPage(state, action.page);
      if (pageCorrected === state.page) {
        return state;
      }
      return {
        ...state,
        page: pageCorrected,
      };
    }
    case PageActionType.nextPage:
    case PageActionType.prevPage: {
      const pageCorrected = correctPage(
        state,
        state.page + (action.type === PageActionType.prevPage ? -1 : 1),
      );
      if (pageCorrected === state.page) {
        return state;
      }
      return {
        ...state,
        page: pageCorrected,
      };
    }
    default:
      return state;
  }
}

export function usePagedData<Item>(data: Item[], pageSize = defaultPageSize): PagedData<Item> {
  const [state, dispatch] = useReducer<PagedDataSource<Item>, PageAction<Item>, number>(reducer, pageSize, init);
  useEffect(() => {
    dispatch({ type: PageActionType.setData, data, pageSize: pageSize > 0 ? pageSize : defaultPageSize });
  }, [data, dispatch, pageSize]);
  const setPage = useCallback((page: number) => {
    dispatch(({ type: PageActionType.setPage, page }));
  }, [dispatch]);
  const nextPage = useCallback(() => {
    dispatch(({ type: PageActionType.nextPage }));
  }, [dispatch]);
  const prevPage = useCallback(() => {
    dispatch(({ type: PageActionType.prevPage }));
  }, [dispatch]);
  return useMemo(() => {
    const {
      data: stateData,
      page: statePage,
      pageSize: statePageSize,
    } = state;
    const pagesCount = getPagesCount(state);
    return {
      data: stateData.slice(statePage * pageSize, (statePage + 1) * pageSize),
      page: statePage,
      pageSize: statePageSize,
      pagesCount,
      prevPageAvailable: statePage > 0,
      nextPageAvailable: statePage < pagesCount - 1,
      setPage,
      nextPage,
      prevPage,
    }
  }, [
    state,
    setPage,
    nextPage,
    prevPage,
    getPagesCount,
  ]);
}
