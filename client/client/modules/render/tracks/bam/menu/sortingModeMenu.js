import {menu} from '../../../utilities';
import {sortTypes} from '../modes';

function mapTrackFn(fn) {
    return function (tracks) {
        (tracks || []).forEach(fn);
    };
}

export default {
    fields: [
        menu.getActionButton('bam>sort>default', 'Default',
            mapTrackFn((track) => { track.bamRenderSettings.sortMode = sortTypes.defaultSortingMode; })),
        menu.getActionButton('bam>sort>strandLocation', 'By start location',
            mapTrackFn((track) => { track.bamRenderSettings.sortMode = sortTypes.sortByStartLocation; })),
        menu.getActionButton('bam>sort>base', 'By base',
            mapTrackFn((track) => { track.bamRenderSettings.sortMode = sortTypes.sortByBase; })),
        menu.getActionButton('bam>sort>strand', 'By strand',
            mapTrackFn((track) => { track.bamRenderSettings.sortMode = sortTypes.sortByStrand; })),
        menu.getActionButton('bam>sort>mappingQuality', 'By mapping quality',
            mapTrackFn((track) => { track.bamRenderSettings.sortMode = sortTypes.sortByMappingQuality; })),
        menu.getActionButton('bam>sort>insertSize', 'By insert size',
            mapTrackFn((track) => { track.bamRenderSettings.sortMode = sortTypes.sortByInsertSize; }))
    ],
    label: 'Sort',
    name: 'bam>sort',
    type: 'submenu'
};