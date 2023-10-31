import {useMemo} from 'react';
import type {TableColumn} from '../../common/table/types';
import type {GenomicsItem} from '../../model/types';

const columns: TableColumn<GenomicsItem>[] = [
  'target',
  'species',
  {key: 'homologyType', title: 'Homology type'},
  'homologue',
  {key: 'homologyGroup', title: 'Homology group'},
  'protein',
  'aa',
  'domains',
];

export function useGenomicsColumns(): TableColumn<GenomicsItem>[] {
  return useMemo(() => {
    return columns as TableColumn<GenomicsItem>[];
  }, []);
}
