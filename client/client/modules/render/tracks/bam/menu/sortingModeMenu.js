import {menu} from '../../../utilities';
import {sortTypes} from '../modes';

export default {
    fields: [
        menu.getActionButton('bam>sort>default', 'Default',
            (renderSettings) => { renderSettings.sortMode = sortTypes.defaultSortingMode; }),
        menu.getActionButton('bam>sort>strandLocation', 'By start location',
            (renderSettings) => { renderSettings.sortMode = sortTypes.sortByStartLocation; }),
        menu.getActionButton('bam>sort>base', 'By base',
            (renderSettings) => { renderSettings.sortMode = sortTypes.sortByBase; }),
        menu.getActionButton('bam>sort>strand', 'By strand',
            (renderSettings) => { renderSettings.sortMode = sortTypes.sortByStrand; }),
        menu.getActionButton('bam>sort>mappingQuality', 'By mapping quality',
            (renderSettings) => { renderSettings.sortMode = sortTypes.sortByMappingQuality; }),
        menu.getActionButton('bam>sort>insertSize', 'By insert size',
            (renderSettings) => { renderSettings.sortMode = sortTypes.sortByInsertSize; })
    ],
    label: 'Sort',
    name: 'bam>sort',
    type: 'submenu'
};