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
  return useTotalCount()[TotalItem.knownDrugs];
}

export function useTotalCountDiseases(): number {
  return useTotalCount()[TotalItem.diseases];
}

export function useTotalCountSequences(): SequencesCount {
  return useTotalCount()[TotalItem.sequences];
}

export function useTotalCountStructures(): number {
  return useTotalCount()[TotalItem.structures];
}

export function useTotalCountGenomics(): number {
  return useTotalCount()[TotalItem.genomics];
}

export function useTotalCountPublications(): number {
  return useTotalCount()[TotalItem.publications];
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
  const genes = sequences.map(s => s.gene);
  return genes.length ? genes : empty;
}

export function useSequencesData(geneId: string): SequencesItem[] {
  const {sequences = empty} =  useInjectedData();
  const geneData = sequences.find((o) => o.gene.id === geneId);
  return geneData ? geneData.data : empty;
}

export function useSequencesReference(geneId: string): SequencesReference {
  const {sequences = empty} =  useInjectedData();
  const geneReference = sequences.find((o) => o.gene.id === geneId);
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
  return {
    value: `${gene.name} (${gene.species})`,
    key: gene.id
  };
}
