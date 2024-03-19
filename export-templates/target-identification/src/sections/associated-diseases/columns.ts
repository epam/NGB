import {useMemo} from 'react';
import type {TableColumn} from '../../common/table/types';
import type {
  DiseasesItem,
  DiseasesOpenTargetsItem,
  DiseasesPharmGKBItem,
  DiseasesTTDItem,
} from '../../model/types';
import {
  DiseasesSource,
} from '../../model/types';

const openTargetsColumns: TableColumn<DiseasesOpenTargetsItem>[] = [
  'target',
  'disease',
  {key: 'overallScore', title: 'overall score'},
  {key: 'geneticAssociation', title: 'genetic association'},
  {key: 'somaticMutations', title: 'somatic mutations'},
  'drugs',
  {key: 'pathwaysSystems', title: 'pathways systems'},
  {key: 'textMining', title: 'text mining'},
  {key: 'animalModels', title: 'animal models'},
  {key: 'RNAExpression', title: 'RNA expression'},
];

const pharmGKBColumns: TableColumn<DiseasesPharmGKBItem>[] = [
  'target',
  'disease',
];

const ttdColumns: TableColumn<DiseasesTTDItem>[] = [
  'target',
  {key: 'ttdTarget', title: 'TTD target'},
  'disease',
  {key: 'clinicalStatus', title: 'clinical status'},
];

export function useColumnsForSource(source: DiseasesSource): TableColumn<DiseasesItem>[] {
  return useMemo(() => {
    if (source === DiseasesSource.openTargets) {
      return openTargetsColumns as TableColumn<DiseasesItem>[];
    }
    if (source === DiseasesSource.pharmGKB) {
      return pharmGKBColumns as TableColumn<DiseasesItem>[];
    }
    if (source === DiseasesSource.ttd) {
      return ttdColumns as TableColumn<DiseasesItem>[];
    }
    return [];
  }, [source]);
}
