import type {Gene} from './types';
import useInjectedData from './base/use-injected-data';
import {
  TotalItem,
  TotalCount,
  KnownDrugsCount,
  KnownDrugsItem,
  KnownDrugsSource,
  DiseasesItem,
  DiseasesSource,
} from "./types";
import {Publication} from "./types/bibliography";

export function useIdentificationName(): string | undefined {
  return useInjectedData().name;
}

const empty = [];
const object = {};

export function useGenesOfInterest(): Gene[] {
  return useInjectedData().interest ?? empty;
}
export function useTranslationalGenes(): Gene[] {
  return useInjectedData().translational ?? empty;
}

function useTotalCount(): TotalCount {
  return useInjectedData().totalCounts;
}

export function useTotalCountDrugs(): KnownDrugsCount {
  return useTotalCount()[TotalItem.knownDrugs];
}

export function useTotalCountDiseases(): number {
  return useTotalCount()[TotalItem.diseases];
}

export function useKnownDrugs(source: KnownDrugsSource): KnownDrugsItem[] {
  const {knownDrugs = empty} =  useInjectedData();
  const sourcedData = knownDrugs.find((o) => o.source === source);
  return sourcedData ? sourcedData.data : empty;
}

export function useAssociatedDiseases(source: DiseasesSource): DiseasesItem[] {
  const {associatedDiseases = empty} =  useInjectedData();
  const sourcedData = associatedDiseases.find((o) => o.source === source);
  return sourcedData ? sourcedData.data : empty;
}

export function usePublications(): Publication[] {
  const {publications = empty} = useInjectedData();
  return publications;
}
