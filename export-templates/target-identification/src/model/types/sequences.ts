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

export type SequencesReference = {
  value: string;
  link?: string;
}

export type SequencesData = {
  gene: Gene;
  reference?: SequencesReference;
  data: SequencesItem[];
};
