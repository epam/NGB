export enum TotalItem {
  knownDrugs = 'knownDrugs',
  diseases = 'diseases',
  sequences = 'sequences',
  genomics = 'genomics',
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
export type GenomicsCount = number;
export type PublicationsCount = number;

export type TotalCount = {
  [TotalItem.knownDrugs]?: KnownDrugsCount,
  [TotalItem.sequences]?: SequencesCount,
  [TotalItem.diseases]?: DiseasesCount,
  [TotalItem.genomics]?: GenomicsCount,
  [TotalItem.publications]?: PublicationsCount,
}
