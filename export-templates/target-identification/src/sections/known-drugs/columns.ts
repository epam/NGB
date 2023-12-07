import {useMemo} from 'react';
import type {TableColumn} from '../../common/table/types';
import type {
  KnownDrugsDGIdbItem,
  KnownDrugsItem,
  KnownDrugsOpenTargetsItem,
  KnownDrugsPharmGKBItem,
} from '../../model/types';
import {
  KnownDrugsSource,
} from '../../model/types';

const knownDrugsColumns: TableColumn<KnownDrugsOpenTargetsItem>[] = [
  'target',
  'drug',
  'type',
  {key: 'mechanism', title: 'mechanism of action'},
  {key: 'action', title: 'action type'},
  'disease',
  'phase',
  'status',
  'source',
];

const dgIdbColumns: TableColumn<KnownDrugsDGIdbItem>[] = [
  'target',
  'drug',
  {key: 'interactionSource', title: 'interaction claim source'},
  {key: 'interactionType', title: 'interaction types'},
];

const pharmGKBColumns: TableColumn<KnownDrugsPharmGKBItem>[] = [
  'target',
  'drug',
  'source',
];

export function useColumnsForSource(source: KnownDrugsSource): TableColumn<KnownDrugsItem>[] {
  return useMemo(() => {
    if (source === KnownDrugsSource.openTargets) {
      return knownDrugsColumns as TableColumn<KnownDrugsItem>[];
    }
    if (source === KnownDrugsSource.dgIdb) {
      return dgIdbColumns as TableColumn<KnownDrugsItem>[];
    }
    if (source === KnownDrugsSource.pharmGKB) {
      return pharmGKBColumns as TableColumn<KnownDrugsItem>[];
    }
    return [];
  }, [source]);
}
