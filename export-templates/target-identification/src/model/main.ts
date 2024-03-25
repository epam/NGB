import {useMemo} from 'react';
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
  SequencesCount,
  SequencesItem,
  SequencesReference,
  GeneAndSpecies,
  StructuresSource,
  StructuresItem,
} from "./types";
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

function useTotalCount(): TotalCount {
  const total = useInjectedData().totalCounts;
  return useMemo<TotalCount>(() => total ?? {}, [total]);
}

export function useTotalCountDrugs(): KnownDrugsCount {
  const knownDrugs = useTotalCount()[TotalItem.knownDrugs];
  return useMemo<KnownDrugsCount>(() => knownDrugs ?? {
    drugs: 0,
    records: 0
  }, [knownDrugs]);
}

export function useTotalCountDiseases(): number {
  return useTotalCount()[TotalItem.diseases] || 0;
}

export function useTotalCountSequences(): SequencesCount {
  const sequences = useTotalCount()[TotalItem.sequences];
  return useMemo<SequencesCount>(() => sequences ?? {
    dnas: 0,
    mrnas: 0,
    proteins: 0,
  }, [sequences]);
}

export function useTotalCountStructures(): number {
  return useTotalCount()[TotalItem.structures] || 0;
}

export function useTotalCountGenomics(): number {
  return useTotalCount()[TotalItem.genomics] || 0;
}

export function useTotalCountPublications(): number {
  return useTotalCount()[TotalItem.publications] || 0;
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

export function useSequencesGenes(): Gene[] {
  const {sequences = empty} =  useInjectedData();
  const genes = [...new Set(sequences
    .map(s => JSON.stringify(s.gene))
  )].map(s =>JSON.parse(s));
  return genes.length ? genes : empty;
}

export function useSequencesData(geneId: string): SequencesItem[] {
  const {sequences = empty} =  useInjectedData();
  const geneData = sequences
    .filter((o) => o.gene.id.toLowerCase() === geneId.toLowerCase())
    .reduce((acc, s) => {
      acc.data = acc.data ? [...acc.data, ...s.data] : [...s.data];
      return acc;
    }, {})
  return geneData ? geneData.data : empty;
}

export function useSequencesReference(geneId: string): SequencesReference[] {
  const {sequences = empty} =  useInjectedData();
  const geneReference = sequences
    .filter((o) => o.gene.id.toLowerCase() === geneId.toLowerCase())
    .reduce((acc, s) => {
      if (s.reference.value || s.reference.link) {
        acc.reference = acc.reference ? [...acc.reference, s.reference] : [s.reference];
      }
      return acc;
    }, {});
  return geneReference ? geneReference.reference : empty;
}

export function useComparativeGenomics() {
  const {comparativeGenomics = empty} =  useInjectedData();
  return comparativeGenomics || empty;
}

export function useStructuresData(source: StructuresSource): StructuresItem[] {
  const {structures = empty} =  useInjectedData();
  const sourcedData = structures.find((o) => o.source === source);
  return sourcedData ? sourcedData.data : empty;
}

export function usePublications(): Publication[] {
  const {publications = empty} = useInjectedData();
  return publications;
}

export function useGeneAndSpecies(gene: Gene): GeneAndSpecies {
  return useMemo<GeneAndSpecies>(() => {
    if (!gene || !gene.id || !gene.name) {
      return {
        value: undefined,
        key: undefined,
      }
    }
    return {
      value: `${gene.species ? `${gene.name} (${gene.species})` : `${gene.name}`}`,
      key: gene.id
    }
  }, [gene]);
}
