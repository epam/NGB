import {useMemo} from 'react';
import type {TableColumn} from '../../common/table/types';
import type {SequencesItem} from '../../model/types';

const columns: TableColumn<SequencesItem>[] = [
  {key: 'target', title: 'target', sortable: false},
  {key: 'transcript', title: 'transcript', sortable: false},
  {key: 'mrnaLength', title: 'Length (nt)', sortable: false},
  {key: 'protein', title: 'protein', sortable: false},
  {key: 'proteinLength', title: 'Length (aa)', sortable: false},
  {key: 'proteinName', title: 'Protein name', sortable: false},
];

export function useSequencesColumns(): TableColumn<SequencesItem>[] {
  return useMemo(() => {
    return columns as TableColumn<SequencesItem>[];
  }, []);
}
