import type {ItemValue} from "./base";

export enum StructuresSource {
  proteinDataBank = 'PROTEIN_DATA_BANK',
  localFiles = 'LOCAL_FILES'
}

export const StructuresSourceNames: Record<StructuresSource, string> = {
  [StructuresSource.proteinDataBank]: 'Protein Data Bank',
  [StructuresSource.localFiles]: 'Local Files',
};

export type StructuresPDBItem = {
  id: ItemValue;
  name: ItemValue;
  method: ItemValue;
  source: ItemValue;
  resolution: ItemValue;
  chains: ItemValue;
}

export type StructuresLocalFilesItem = {
  id: ItemValue;
  name: ItemValue;
  owner: ItemValue;
}

export type StructuresPDBData = {
  source: StructuresSource.proteinDataBank;
  data: StructuresPDBItem[];
}

export type StructuresLocalFilesData = {
  source: StructuresSource.localFiles;
  data: StructuresLocalFilesItem[];
}

export type StructuresItem = StructuresPDBItem | StructuresLocalFilesItem;

export type StructuresData = StructuresPDBData | StructuresLocalFilesData;
