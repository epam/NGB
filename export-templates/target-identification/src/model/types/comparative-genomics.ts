import type {ItemValue} from "./base";

export type GenomicsItem = {
  target: ItemValue;
  species: ItemValue;
  homologyType: ItemValue;
  homologue?: ItemValue;
  homologyGroup?: ItemValue;
  protein?: ItemValue;
  aa?: ItemValue;
  domains?: ItemValue;
}

export type GenomicsData = GenomicsItem[];