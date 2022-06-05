import {Node, utilities} from './internal';

export const SORT_MODE_BY_NAME_ASCENDING = 'sortByNameAsc';
export const SORT_MODE_BY_NAME_DESCENDING = 'sortByNameDesc';
const toPlainList = utilities.toPlainList;
export {toPlainList};

const dummyReferenceNames = utilities.dummyReferenceNames;

export default class ngbDataSetsService {
    static instance(dispatcher, projectContext, projectDataService) {
        return new ngbDataSetsService(dispatcher, projectContext, projectDataService);
    }

    dispatcher;
    projectContext;
    projectDataService;

    constructor(dispatcher, projectContext, projectDataService) {
        this.dispatcher = dispatcher;
        this.projectContext = projectContext;
        this.projectDataService = projectDataService;
        this.filter = utilities.treeFilter;
    }

    async getDatasets() {
        const datasets = this.applyGenomeFilter(this.projectContext.datasets);
        ngbDataSetsService.sort(datasets);
        await this.updateSelectionFromState(datasets);
        return datasets;
    }

    isDummyReference (referenceName) {
        return dummyReferenceNames
            .filter(o => o.test(referenceName))
            .length > 0;
    }

    applyGenomeFilter(projects) {
        const genomeFilter = this.projectContext.datasetsFilter;
        const filter = utilities.getGenomeFilter(genomeFilter);
        return projects.filter(filter);
    }

    getSelectedTracks(datasets, forceReference) {
        const items = utilities.findSelectedItems(datasets);
        const trackIsDisplayedInTreeFn = (track, _datasets) => {
            for (let i = 0; i < _datasets.length; i ++) {
                const dataset = _datasets[i];
                if (dataset.name.toLowerCase() === track.projectId.toLowerCase()) {
                    const items = dataset._lazyItems || dataset.items;
                    for (let j = 0; j < items.length; j++) {
                        if (items[j] && items[j].isTrack && items[j].name.toLowerCase() === track.name.toLowerCase()) {
                            return true;
                        }
                    }
                } else if (dataset.nestedProjects && trackIsDisplayedInTreeFn(track, dataset.nestedProjects)) {
                    return true;
                }
            }
            return false;
        };

        if (forceReference && this.projectContext.reference && forceReference.name.toLowerCase() === this.projectContext.reference.name.toLowerCase()) {
            const localTracks = (this.projectContext.tracks).filter(t => t.isLocal);
            for (let i = 0; i < localTracks.length; i++) {
                const isDisplayed = trackIsDisplayedInTreeFn(localTracks[i], datasets);
                if (!isDisplayed) {
                    items.push(localTracks[i]);
                }
            }
        }

        const reference = items.map(track => track.reference)[0] || forceReference;
        const referenceIsSelected = items.filter(track => track.format === 'REFERENCE').length > 0;

        let tracks = [];
        if (referenceIsSelected) {
            tracks = [
                reference,
                ...(items || []).filter(track => !reference || track.format !== 'REFERENCE')
            ].filter(Boolean);
        }

        return {
            reference,
            tracks
        };
    }

    async updateSelectionFromState(datasets) {
        if (!datasets || this.__lockStatesUpdate) {
            return;
        }
        if (!this.projectContext.reference) {
            this.deselectAll(datasets);
            return;
        }
        const datasetsIds = this.projectContext.tracksState.reduce((ids, track) => {
            if (ids.filter(t => t === track.projectId).length === 0) {
                return [...ids, track.projectId];
            }
            return ids;
        }, []);
        for (let i = 0; i < datasetsIds.length; i++) {
            utilities.expandToProject(datasets, {name: datasetsIds[i]});
        }
        this.updateDatasetState(datasets);
    }

    updateDatasetState(datasets) {
        for (let i = 0; i < datasets.length; i++) {
            datasets[i].isTrack
                ? this.updateTerminalNodeStateFn(datasets[i])
                : this.updateParentNodeStateFn(datasets[i]);
        }
    }

    getSelectedDatasets (datasets) {
        const selected = datasets.filter(dataset => dataset.isProject && dataset.__selected);
        return selected.concat(
            selected
                .map(dataset => this.getSelectedDatasets(dataset.nestedProjects || []))
                .reduce((r, c) => ([...r, ...c]), [])
        );
    }

    updateTerminalNodeStateFn(item: Node) {
        const tracksState = this.projectContext.tracksState;
        let selected = false;
        if (item.isTrack && !item.isPlaceholder) {
            selected = tracksState
                .filter(s => s.bioDataItemId.toString().toLowerCase() === item.name.toLowerCase() &&
                    s.format === item.format &&
                    (s.projectId === item.projectId)
                ).length > 0;
        }
        item.__selected = selected;
        return {selected: selected, indeterminate: selected};
    }


    updateParentNodeStateFn(item: Node) {
        let someChildSelected = false;
        let indeterminate = false;
        let referenceSelected = false;

        for (let j = 0; j < (item.items || []).length; j++) {
            if (item.items[j].isTrack && item.items[j].format === 'REFERENCE') {
                const track = item.items[j];
                const {selected: _referenceSelected} = this.updateTerminalNodeStateFn(track);
                referenceSelected = _referenceSelected;
                continue;
            }
            const childObj = item.items[j].isTrack
                ? this.updateTerminalNodeStateFn(item.items[j])
                : this.updateParentNodeStateFn(item.items[j]);
            someChildSelected = someChildSelected || childObj.selected;
            indeterminate = indeterminate || childObj.indeterminate || childObj.selected;
        }
        const selected = referenceSelected || someChildSelected;

        item.__selected = selected;
        item.__indeterminate = indeterminate && !selected;

        const expanded = item.__indeterminate || item.__selected;
        if (expanded) {
            item.__expanded = expanded;
        }
        return {selected: selected,  indeterminate: item.__indeterminate};
    }

    static sort(datasets, mode) {
        let fn = null;
        switch (mode) {
            case SORT_MODE_BY_NAME_ASCENDING: fn = utilities.sortByNameAsc; break;
            case SORT_MODE_BY_NAME_DESCENDING: fn = utilities.sortByNameDesc; break;
        }
        utilities.sortDatasets(datasets, fn);
    }

    toggle(item: Node) {
        utilities.expandNodeWithChilds(item);
    }

    getItemReference(item: Node) {
        if (item.isTrack) {
            return item.reference;
        } else {
            return utilities.findProjectReference(item);
        }
    }

    checkSelectionAvailable(item: Node, isSelected) {
        if (isSelected) {
            let forceReference = null;
            if (item.isProject) {
                utilities.expandNodeWithChilds(item);
                forceReference = utilities.findProjectReference(item);
            } else {
                // we should also select reference:
                if (item.reference) {
                    forceReference = item.reference;
                    item.reference.__selected = true;
                }
            }
            if (forceReference && this.projectContext.reference && forceReference.name.toLowerCase() !== this.projectContext.reference.name.toLowerCase()) {
                return false;
            }
        }
        return true;
    }

    deselectItem(item: Node) {
        item.__selected = false;
        if (item.isProject) {
            item.items.forEach(t => {t.__selected = false; t.__indeterminate = false; this.deselectItem(t);});
        }
    }

    deselectAll(datasets) {
        for (let i = 0; i < datasets.length; i++) {
            datasets[i].__selected = false;
            datasets[i].__indeterminate = false;
            if (datasets[i].items) this.deselectAll(datasets[i].items);
        }
    }

    checkDatasetSelection(dataset) {
        if (!dataset.__selected || !dataset.isProject) {
            return;
        }
        if (dataset.nestedProjects && dataset.nestedProjects.length) {
            dataset.nestedProjects
                .forEach(nestedProject => this.checkDatasetSelection(nestedProject));
        }
        if ((dataset.items || []).filter(child => child.__selected).length === 0) {
            dataset.__selected = false;
        }
    }

    selectItem(item: Node, isSelected, datasets) {
        if (this.__previousItem === item && item.__previousSelectedState !== undefined &&
            item.__previousSelectedState === isSelected) {
            if (isSelected) {
                this.deselectItem(item);
                isSelected = !isSelected;
            }
        }
        this.__previousItem = item;
        item.__previousSelectedState = isSelected;
        let forceReference = this.projectContext.reference;
        if (isSelected) {
            if (item.isProject) {
                utilities.expandNodeWithChilds(item);
                forceReference = utilities.findProjectReference(item);
                if (forceReference) {
                    utilities.deSelectAllProjectReferences(datasets);
                    forceReference.__selected = true;
                }
            } else {
                // we should also select reference:
                if (item.reference) {
                    forceReference = item.reference;
                    item.reference.__selected = true;
                }
            }
            utilities.selectRecursively(item, isSelected);
            this.updateTracksState(datasets, forceReference);

        // } else if (item.isTrack && item.project && item.project.items.filter(t => t.format !== 'REFERENCE' && t.__selected).length === 0) {
        //     item.project.items.filter(t => t.format !== 'REFERENCE').forEach(t => t.__selected = false);
        } else {
            // not selected
            datasets.forEach(this.checkDatasetSelection.bind(this));
            const currentDataset = item.isProject ? item : item.project;
            const otherSelectedDatasets = this.getSelectedDatasets(datasets)
                .filter(d => !currentDataset || currentDataset.id !== d.id);
            const currentDatasetHasNoSelectedTracks = (currentDataset.items || [])
                .filter(i => i.format !== 'REFERENCE' && i.__selected)
                .length === 0;
            const otherDatasetsHasSelectedTracks = utilities
                .findSelectedItems(otherSelectedDatasets)
                .filter(o => o.format !== 'REFERENCE')
                .length > 0;
            if (
                currentDatasetHasNoSelectedTracks &&
                otherSelectedDatasets.length > 0 &&
                otherDatasetsHasSelectedTracks
            ) {
                this.deselectItem(currentDataset);
                [forceReference] = otherSelectedDatasets.map(utilities.findProjectReference);
                if (forceReference) {
                    forceReference.__selected = true;
                }
            }
        }
        this.navigateToTracks(datasets, forceReference);
    }

    updateTracksState(datasets, forceReference) {
        for (let i = 0; i < datasets.length; i++) {
            utilities.updateTracksStateFn(datasets[i], forceReference);
            if (datasets[i].items) {
                this.updateTracksState(datasets[i].items, forceReference);
            }
        }
    }

    navigateToTracks(datasets, forceReference) {
        const {tracks} = this.getSelectedTracks(datasets, forceReference);
        const currentTracks = (this.projectContext.tracks || []).slice();
        this.__lockStatesUpdate = true;
        let [reference] = tracks.filter(t => t.format === 'REFERENCE');
        if (!reference) {
            const [refTrack] = tracks.filter(t => t.reference);
            if (refTrack) {
                reference = refTrack.reference;
                reference.projectId = refTrack.projectId;
                reference.isLocal = refTrack.isLocal;
                tracks.push(reference);
            }
        }
        if (reference) {
            const [currentReference] = currentTracks
                .filter(track => track.format === 'REFERENCE');
            if (!currentReference) {
                currentTracks.push(reference);
            } else if (
                `${currentReference.bioDataItemId || ''}`.toLowerCase() !==
                `${reference.bioDataItemId || ''}`.toLowerCase() ||
                currentReference.projectId !== reference.projectId
            ) {
                currentTracks.splice(currentTracks.indexOf(currentReference), 1, reference);
            }
        }
        if (reference && tracks.filter(t => t.format === 'REFERENCE').length > 0) {
            const tracksState = this.projectContext.tracksState || [];
            if (tracksState.length === 0) {
                tracksState.push({
                    bioDataItemId: reference.name,
                    duplicateId: reference.duplicateId,
                    projectId: reference.projectId,
                    format: 'REFERENCE'
                });
            }
            const getTrackId = track => `[${track.name.toLowerCase()}][${(track.projectId || '').toLowerCase()}]`;
            const getTrackStateId = track => `[${track.bioDataItemId.toLowerCase()}][${(track.projectId || '').toLowerCase()}]`;
            const shouldAddAnnotationTracks = !this.projectContext.reference || this.projectContext.reference.name.toLowerCase() !== reference.name.toLowerCase();
            const tracksIds = tracks.map(getTrackId);
            const tracksStateIds = tracksState.map(getTrackStateId);
            const self = this;
            const mapTrackFn = function (track) {
                const state = self.projectContext.getTrackState(track.name.toLowerCase(), (track.projectId || '').toLowerCase());
                if (state) {
                    const o = Object.assign(state, {index: track.indexPath, name: track.name, format: track.format, isLocal: track.isLocal});
                    o.projectId = o.projectId || '';
                    return o;
                }
                return utilities.mapTrackFn(track);
            };
            const addedTracks = tracks
                .filter(track => tracksStateIds.indexOf(getTrackId(track)) === -1 &&
                    track.format !== 'REFERENCE'
                );
            const addedTrackStates = addedTracks.map(mapTrackFn);
            let existedTracks = tracksState
                .filter(track => tracksIds.indexOf(getTrackStateId(track)) >= 0);
            if (!existedTracks.filter(t => t.bioDataItemId.toString().toLowerCase() === reference.name.toLowerCase()).length) {
                existedTracks = [mapTrackFn(reference), ...existedTracks];
            }
            const newTracksState = [...existedTracks, ...addedTrackStates];
            const newTracks = currentTracks
                .filter(track => track.format === 'REFERENCE' ||
                    tracksIds.indexOf(getTrackId(track)) >= 0
                )
                .concat(addedTracks);
            this.projectContext.changeState({
                reference: reference,
                tracks: newTracks,
                tracksState: newTracksState,
                shouldAddAnnotationTracks
            });
        } else {
            this.projectContext.changeState({reference: null, tracks: null, tracksState: null});
        }
        this.__lockStatesUpdate = false;
    }

    static search(pattern, datasets) {
        return utilities.search(pattern.toLowerCase(), datasets || []);
    }
}
