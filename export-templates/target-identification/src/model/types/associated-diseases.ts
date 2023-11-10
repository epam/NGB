import type {ItemValue} from "./base";

export enum DiseasesSource {
  openTargets = 'OPEN_TARGETS',
  pharmGKB = 'PHARMGKB'
}

export const DiseasesSourceNames: Record<DiseasesSource, string> = {
  [DiseasesSource.openTargets]: 'Open Targets',
  [DiseasesSource.pharmGKB]: 'PharmGKB',
};

export type DiseasesOpenTargetsItem = {
  target: ItemValue;
  disease: ItemValue;
  overallScore: ItemValue;
  geneticAssociation: ItemValue;
  somaticMutations: ItemValue;
  drugs: ItemValue;
  pathwaysSystems: ItemValue;
  textMining: ItemValue;
  animalModels: ItemValue;
  RNAExpression: ItemValue;
}

export type DiseasesPharmGKBItem = {
  target: ItemValue;
  disease: ItemValue;
}

export type DiseasesOpenTargetsData = {
  source: DiseasesSource.openTargets;
  data: DiseasesOpenTargetsItem[];
}

export type DiseasesPharmGKBData = {
  source: DiseasesSource.pharmGKB;
  data: DiseasesPharmGKBItem[];
}

export type DiseasesItem = DiseasesOpenTargetsItem | DiseasesPharmGKBItem;

export type DiseasesData = DiseasesOpenTargetsData | DiseasesPharmGKBData;
