import type {Gene} from './types';
import useInjectedData from './base/use-injected-data';
import {KnownDrugsItem, KnownDrugsSource} from "./types";
import {Publication} from "./types/bibliography";

export function useIdentificationName(): string | undefined {
  return useInjectedData().name;
}

const empty = [];

export function useGenesOfInterest(): Gene[] {
  return useInjectedData().interest ?? empty;
}
export function useTranslationalGenes(): Gene[] {
  return useInjectedData().translational ?? empty;
}

export function useKnownDrugs(source: KnownDrugsSource): KnownDrugsItem[] {
  const {knownDrugs = empty} =  useInjectedData();
  const sourcedData = knownDrugs.find((o) => o.source === source);
  return sourcedData ? sourcedData.data : empty;
}

export function usePublications(): Publication[] {
  const {publications = empty} = useInjectedData();
  return publications;
}
