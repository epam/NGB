import type {ItemValue} from "./base";

export enum KnownDrugsSource {
  openTargets = 'OPEN_TARGETS',
  dgIdb = 'DGIDB',
  pharmGKB = 'PHARMGKB',
  ttd = 'TTD',
}

export const KnownDrugsSourceNames: Record<KnownDrugsSource, string> = {
  [KnownDrugsSource.openTargets]: 'Open Targets',
  [KnownDrugsSource.dgIdb]: 'DGIdb',
  [KnownDrugsSource.pharmGKB]: 'PharmGKB',
  [KnownDrugsSource.ttd]: 'TTD',
};

export type KnownDrugsOpenTargetsItem = {
  target: ItemValue;
  drug: ItemValue;
  type?: ItemValue;
  mechanism?: ItemValue;
  action?: ItemValue;
  disease?: ItemValue;
  phase?: ItemValue;
  status?: ItemValue;
  source?: ItemValue;
}

export type KnownDrugsDGIdbItem = {
  target: ItemValue;
  drug: ItemValue;
  interactionSource?: ItemValue;
  interactionType?: ItemValue;
}

export type KnownDrugsPharmGKBItem = {
  target: ItemValue;
  drug: ItemValue;
  source?: ItemValue;
}

export type KnownDrugsTTDItem = {
  target: ItemValue;
  ttdTarget: ItemValue;
  drug: ItemValue;
  company?: ItemValue;
  type?: ItemValue;
  therapeuticClass?: ItemValue;
  inChI?: ItemValue;
  inChIKey?: ItemValue;
  canonicalSmiles?: ItemValue;
  status?: ItemValue;
  compoundClass?: ItemValue;
}

export type KnownDrugsOpenTargetsData = {
  source: KnownDrugsSource.openTargets;
  data: KnownDrugsOpenTargetsItem[];
}

export type KnownDrugsDGIdbData = {
  source: KnownDrugsSource.dgIdb;
  data: KnownDrugsDGIdbItem[];
}

export type KnownDrugsPharmGKBData = {
  source: KnownDrugsSource.pharmGKB;
  data: KnownDrugsPharmGKBItem[];
}

export type KnownDrugsTTDData = {
  source: KnownDrugsSource.ttd;
  data: KnownDrugsTTDItem[];
}

export type KnownDrugsItem = KnownDrugsOpenTargetsItem | KnownDrugsDGIdbItem | KnownDrugsPharmGKBItem | KnownDrugsTTDItem;

export type KnownDrugsData = KnownDrugsOpenTargetsData | KnownDrugsDGIdbData | KnownDrugsPharmGKBData | KnownDrugsTTDData;
