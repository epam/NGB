import {useMemo} from 'react';
import type {TableColumn} from '../../common/table/types';
import type {SequencesItem} from '../../model/types';

const columns: TableColumn<SequencesItem>[] = [
  'target',
  'transcript',
  {key: 'mrnaLength', title: 'Length (nt)'},
  'protein',
  {key: 'proteinLength', title: 'Length (aa)'},
  {key: 'proteinName', title: 'Protein name'},
];

export function useSequencesColumns(): TableColumn<SequencesItem>[] {
  return useMemo(() => {
    return columns as TableColumn<SequencesItem>[];
  }, []);
}
