import {getActionButton} from './actionButton';
import {sortTypes} from '../modes';

export default {
    fields: [
        getActionButton('bam>sort>default', 'Default',
            (renderSettings) => { renderSettings.sortMode = sortTypes.defaultSortingMode; }),
        getActionButton('bam>sort>strandLocation', 'By start location',
            (renderSettings) => { renderSettings.sortMode = sortTypes.sortByStartLocation; }),
        getActionButton('bam>sort>base', 'By base',
            (renderSettings) => { renderSettings.sortMode = sortTypes.sortByBase; }),
        getActionButton('bam>sort>strand', 'By strand',
            (renderSettings) => { renderSettings.sortMode = sortTypes.sortByStrand; }),
        getActionButton('bam>sort>mappingQuality', 'By mapping quality',
            (renderSettings) => { renderSettings.sortMode = sortTypes.sortByMappingQuality; }),
        getActionButton('bam>sort>insertSize', 'By insert size',
            (renderSettings) => { renderSettings.sortMode = sortTypes.sortByInsertSize; })
    ],
    label: 'Sort',
    name: 'bam>sort',
    type: 'submenu'
};