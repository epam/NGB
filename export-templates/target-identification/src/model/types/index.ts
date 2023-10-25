import type {KnownDrugsData} from './known-drugs';
import type {Publication} from "./bibliography";

export * from './base';
export * from './known-drugs';

export type Gene = {
  id: string;
  name: string;
  species: string;
  description?: string | undefined;
}

export type GlobalData = {
  name?: string;
  interest?: Gene[];
  translational?: Gene[];
  knownDrugs?: KnownDrugsData[];
  publications?: Publication[]
}

export type PagedDataState<Item> = {
  data: Item[];
  page: number;
  pagesCount: number;
  pageSize: number;
  nextPageAvailable: boolean;
  prevPageAvailable: boolean;
}

export type PagedDataActions = {
  setPage(page: number): void;
  nextPage(): void;
  prevPage(): void;
}

export type PagedData<Item> = PagedDataState<Item> & PagedDataActions;
