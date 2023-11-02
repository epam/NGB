import {useMemo} from 'react';
import type {TableColumn} from '../../common/table/types';
import type {GenomicsItem} from '../../model/types';

const columns: TableColumn<GenomicsItem>[] = [
  {key: 'target', title: 'target', showFilter: true, sortable: false},
  {key: 'species', title: 'species', showFilter: true, sortable: false},
  {key: 'homologyType', title: 'Homology type', showFilter: true, sortable: false},
  {key: 'homologue', title: 'homologue', showFilter: false, sortable: false},
  {key: 'homologyGroup', title: 'Homology group', showFilter: false, sortable: false},
  {key: 'protein', title: 'protein', showFilter: false, sortable: false},
  {key: 'aa', title: 'aa', showFilter: false, sortable: false},
];

export function useGenomicsColumns(): TableColumn<GenomicsItem>[] {
  return useMemo(() => {
    return columns as TableColumn<GenomicsItem>[];
  }, []);
}
