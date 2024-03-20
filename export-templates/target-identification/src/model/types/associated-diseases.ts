import type {ItemValue} from "./base";

export enum DiseasesSource {
  openTargets = 'OPEN_TARGETS',
  pharmGKB = 'PHARMGKB',
  ttd = 'TTD',
}

export const DiseasesSourceNames: Record<DiseasesSource, string> = {
  [DiseasesSource.openTargets]: 'Open Targets',
  [DiseasesSource.pharmGKB]: 'PharmGKB',
  [DiseasesSource.ttd]: 'TTD',
};

export type DiseasesOpenTargetsItem = {
  target: ItemValue;
  disease: ItemValue;
  overallScore?: ItemValue;
  geneticAssociation?: ItemValue;
  somaticMutations?: ItemValue;
  drugs?: ItemValue;
  pathwaysSystems?: ItemValue;
  textMining?: ItemValue;
  animalModels?: ItemValue;
  RNAExpression?: ItemValue;
}

export type DiseasesPharmGKBItem = {
  target: ItemValue;
  disease: ItemValue;
}

export type DiseasesTTDItem = {
  target: ItemValue;
  ttdTarget: ItemValue;
  disease: ItemValue;
  clinicalStatus?: ItemValue;
}

export type DiseasesOpenTargetsData = {
  source: DiseasesSource.openTargets;
  data: DiseasesOpenTargetsItem[];
}

export type DiseasesPharmGKBData = {
  source: DiseasesSource.pharmGKB;
  data: DiseasesPharmGKBItem[];
}

export type DiseasesTTDData = {
  source: DiseasesSource.ttd;
  data: DiseasesTTDItem[];
}

export type DiseasesItem = DiseasesOpenTargetsItem | DiseasesPharmGKBItem | DiseasesTTDItem;

export type DiseasesData = DiseasesOpenTargetsData | DiseasesPharmGKBData | DiseasesTTDData;
