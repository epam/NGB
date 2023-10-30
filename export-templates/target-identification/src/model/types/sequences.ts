import type {ItemValue} from "./base";
import type {Gene} from './index';

export type SequencesItem = {
  target: ItemValue;
  transcript?: ItemValue;
  mrnaLength?: ItemValue;
  protein?: ItemValue;
  proteinLength?: ItemValue;
  proteinName?: ItemValue;
}

export type SequencesData = {
  gene: Gene;
  data: SequencesItem[];
};
