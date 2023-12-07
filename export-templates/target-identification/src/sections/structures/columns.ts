import {useMemo} from 'react';
import type {TableColumn} from '../../common/table/types';
import type {
  StructuresItem,
  StructuresPDBItem,
  StructuresLocalFilesItem,
} from '../../model/types';
import {
  StructuresSource,
} from '../../model/types';

const pdbColumns: TableColumn<StructuresPDBItem>[] = [
  {key: 'id', title: 'id', showFilter: true, sortable: false},
  {key: 'name', title: 'name', showFilter: true, sortable: false},
  {key: 'method', title: 'method', showFilter: false, sortable: false},
  {key: 'source', title: 'source', showFilter: false, sortable: false},
  {key: 'resolution', title: 'resolution', showFilter: false, sortable: false},
  {key: 'chains', title: 'chains', showFilter: false, sortable: false},
];

const localFilesColumns: TableColumn<StructuresLocalFilesItem>[] = [
  {key: 'id', title: 'id', showFilter: true, sortable: true},
  {key: 'name', title: 'name', showFilter: true, sortable: true},
  {key: 'owner', title: 'owner', showFilter: true, sortable: true},
];

export function useColumnsForSource(source: StructuresSource): TableColumn<StructuresItem>[] {
  return useMemo(() => {
    if (source === StructuresSource.proteinDataBank) {
      return pdbColumns as TableColumn<StructuresItem>[];
    }
    if (source === StructuresSource.localFiles) {
      return localFilesColumns as TableColumn<StructuresItem>[];
    }
    return [];
  }, [source]);
}
