export enum TotalItem {
  knownDrugs = 'knownDrugs',
  sequences = 'sequences',
  diseases = 'diseases',
  structures = 'structures',
  publications = 'publications'
}

export type KnownDrugsCount = {
  drugs: number;
  records: number;
}

export type SequencesCount = {
  dnas: number;
  mrnas: number;
  proteins: number;
}

export type DiseasesCount = number;
export type StructuresCount = number;
export type PublicationsCount = number;

export type TotalCount = {
  [TotalItem.knownDrugs]?: KnownDrugsCount,
  [TotalItem.sequences]?: SequencesCount,
  [TotalItem.diseases]?: DiseasesCount,
  [TotalItem.structures]?: StructuresCount,
  [TotalItem.publications]?: PublicationsCount,
}
