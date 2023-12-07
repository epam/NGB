import type {TotalCount} from './total-counts';
import type {KnownDrugsData} from './known-drugs';
import type {DiseasesData} from './associated-diseases';
import type {SequencesData} from './sequences';
import type {GenomicsData} from './comparative-genomics';
import type {StructuresData} from './structures';
import type {Publication} from "./bibliography";

export * from './base';
export * from './known-drugs';
export * from './associated-diseases';
export * from './sequences';
export * from './comparative-genomics';
export * from './structures';
export * from './total-counts';

export type Gene = {
  id: string;
  name: string;
  species?: string;
  description?: string | undefined;
}

export type GeneAndSpecies = {
  value: string,
  key: string,
}

export type GlobalData = {
  name?: string;
  interest?: Gene[];
  translational?: Gene[];
  totalCounts?: TotalCount;
  knownDrugs?: KnownDrugsData[];
  associatedDiseases?: DiseasesData[];
  sequences?: SequencesData[];
  comparativeGenomics?: GenomicsData[];
  structures?: StructuresData[];
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
