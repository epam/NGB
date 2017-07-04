import {Node, utilities} from './internal';

export const SORT_MODE_BY_NAME_ASCENDING = 'sortByNameAsc';
export const SORT_MODE_BY_NAME_DESCENDING = 'sortByNameDesc';

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
        let projects = this.projectContext.datasets;
        if (!this.projectContext.datasetsArePrepared) {
            projects = projects.map(utilities.preprocessNode);
            this.projectContext.datasetsArePrepared = true;
        }
        const datasets = this.applyGenomeFilter(projects);
        ngbDataSetsService.sort(datasets);
        await this.updateSelectionFromState(datasets);
        return datasets;
    }

    applyGenomeFilter(projects) {
        const genomeFilter = this.projectContext.datasetsFilter;
        const filter = utilities.getGenomeFilter(genomeFilter);
        return projects.filter(filter);
    }

    getSelectedTracks(datasets, forceReference) {
        const items = [];
        const findSelectedTracksFn = function (item: Node) {
            if (item.items) {
                for (let i = 0; i < item.items.length; i++) {
                    if (item.items[i] && item.items[i].__selected && item.items[i].isTrack) {
                        items.push(item.items[i]);
                    }
                    findSelectedTracksFn(item.items[i]);
                }
            }
        };
        for (let i = 0; i < datasets.length; i++) {
            findSelectedTracksFn(datasets[i]);
        }

        const trackIsDisplayedInTreeFn = (track, _datasets) => {
            for (let i = 0; i < _datasets.length; i ++) {
                const dataset = _datasets[i];
                if (dataset.name.toLowerCase() === track.projectId.toLowerCase()) {
                    const items = dataset._lazyItems || dataset.items;
                    for (let j = 0; j < items.length; i++) {
                        if (items[i] && items[i].isTrack && items[i].name.toLowerCase() === track.name.toLowerCase()) {
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

        const reference = items.map(track => track.reference)[0] | forceReference;

        return {
            reference,
            tracks: items
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
            datasets[i].isTrack ?  this.updateTerminalNodeStateFn(datasets[i]) :  this.updateParentNodeStateFn(datasets[i]);
        }
    }

    updateTerminalNodeStateFn(item: Node) {
        const tracksState = this.projectContext.tracksState;
        let selected = false;
        if (item.isTrack && !item.isPlaceholder) {
            selected = !!tracksState.filter(s => s.bioDataItemId.toString().toLowerCase() === item.name.toLowerCase() && s.projectId === item.projectId).length;
        }
        item.__selected = selected;
        return {selected: selected, indeterminate: selected};
    }


    updateParentNodeStateFn(item: Node) {
        let allChildSelected = true;
        let indeterminate = false;

        if (item.items) {
            for (let j = 0; j < item.items.length; j++) {
                if (item.items[j].isTrack && item.items[j].format === 'REFERENCE') {
                    continue;
                }
                const childObj = item.items[j].isTrack ? this.updateTerminalNodeStateFn(item.items[j]) : this.updateParentNodeStateFn(item.items[j]);
                allChildSelected = allChildSelected && childObj.selected;
                indeterminate = indeterminate || childObj.indeterminate || childObj.selected;
            }
        }
        const selected = !!(allChildSelected && item.items && item.items.length > 0);

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
        } else if (item.isTrack && item.project && item.project.items.filter(t => t.format !== 'REFERENCE' && t.__selected).length === 0) {
            item.project.items.forEach(t => t.__selected = false);
        }
    }

    deselectAll(datasets) {
        for (let i = 0; i < datasets.length; i++) {
            datasets[i].__selected = false;
            datasets[i].__indeterminate = false;
            if (datasets[i].items) this.deselectAll(datasets[i].items);
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
            } else {
                // we should also select reference:
                if (item.reference) {
                    forceReference = item.reference;
                    item.reference.__selected = true;
                }
            }
            utilities.selectRecursively(item, isSelected);
            this.updateTracksState(datasets, forceReference);

        } else if (item.isTrack && item.project && item.project.items.filter(t => t.format !== 'REFERENCE' && t.__selected).length === 0) {
            item.project.items.filter(t => t.format !== 'REFERENCE').forEach(t => t.__selected = false);
        }
        const {tracks} = this.getSelectedTracks(datasets, forceReference);
        if (tracks.filter(t => t.format !== 'REFERENCE').length === 0) {
            tracks.forEach(t => t.__selected = false);
        }
        this.navigateToTracks(datasets, forceReference);
    }

    updateTracksState(datasets, forceReference) {
        for (let i = 0; i < datasets.length; i++) {
            utilities.updateTracksStateFn(datasets[i], forceReference);
            if (datasets[i].items) this.updateTracksState(datasets[i].items, forceReference);
        }
    }

    navigateToTracks(datasets, forceReference) {
        const {tracks} = this.getSelectedTracks(datasets, forceReference);
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
        if (reference && tracks.filter(t => t.format !== 'REFERENCE').length > 0) {
            const tracksState = this.projectContext.tracksState || [];
            if (tracksState.length === 0) {
                tracksState.push({
                    bioDataItemId: reference.name,
                    projectId: reference.projectId,
                    format: 'REFERENCE'
                });
            }
            const shouldAddAnnotationTracks = this.projectContext.reference === null || this.projectContext.reference.name.toLowerCase() !== reference.name.toLowerCase();
            const tracksIds = tracks.map(track => `[${track.name.toLowerCase()}][${track.projectId.toLowerCase()}]`);
            const tracksStateIds = tracksState.map(track => `[${track.bioDataItemId.toLowerCase()}][${track.projectId.toLowerCase()}]`);
            const self = this;
            const mapTrackFn = function (track) {
                const state = self.projectContext.getTrackState(track.name.toLowerCase(), track.projectId.toLowerCase());
                if (state) {
                    return Object.assign(state, {index: track.indexPath, name: track.name, format: track.format, isLocal: track.isLocal});
                }
                return utilities.mapTrackFn(track);
            };
            const addedTracks = tracks
                .filter(track => tracksStateIds.indexOf(`[${track.name.toLowerCase()}][${track.projectId.toLowerCase()}]`) === -1 && track.format !== 'REFERENCE')
                .map(mapTrackFn);
            let existedTracks = tracksState.filter(track => tracksIds.indexOf(`[${track.bioDataItemId.toLowerCase()}][${track.projectId.toLowerCase()}]`) >= 0);
            if (!existedTracks.filter(t => t.bioDataItemId.toString().toLowerCase() === reference.name.toLowerCase()).length) {
                existedTracks = [utilities.mapTrackFn(reference), ...existedTracks];
            }
            const newTracksState = [...existedTracks, ...addedTracks];
            this.projectContext.changeState({reference: reference, tracks, tracksState: newTracksState, shouldAddAnnotationTracks});
        } else {
            this.projectContext.changeState({reference: null, tracks: null, tracksState: null});
        }
        this.__lockStatesUpdate = false;
    }

    static search(pattern, datasets) {
        return utilities.search(pattern.toLowerCase(), datasets || []);
    }
}