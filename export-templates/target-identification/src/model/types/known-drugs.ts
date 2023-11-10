import type {ItemValue} from "./base";

export enum KnownDrugsSource {
  openTargets = 'OPEN_TARGETS',
  dgIdb = 'DGIDB',
  pharmGKB = 'PHARMGKB'
}

export const KnownDrugsSourceNames: Record<KnownDrugsSource, string> = {
  [KnownDrugsSource.openTargets]: 'Open Targets',
  [KnownDrugsSource.dgIdb]: 'DGIdb',
  [KnownDrugsSource.pharmGKB]: 'PharmGKB',
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

export type KnownDrugsItem = KnownDrugsOpenTargetsItem | KnownDrugsDGIdbItem | KnownDrugsPharmGKBItem;

export type KnownDrugsData = KnownDrugsOpenTargetsData | KnownDrugsDGIdbData | KnownDrugsPharmGKBData;
