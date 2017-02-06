import {Node, utilities} from './internal';

export const SORT_MODE_BY_NAME_ASCENDING = 'sortByNameAsc';
export const SORT_MODE_BY_NAME_DESCENDING = 'sortByNameDesc';

export default class ngbDataSetsService {
    static instance(dispatcher, projectContext, ivhTreeviewBfs, ivhTreeviewMgr, ivhTreeviewOptions, projectDataService) {
        return new ngbDataSetsService(dispatcher, projectContext, ivhTreeviewBfs, ivhTreeviewMgr, ivhTreeviewOptions, projectDataService);
    }

    treeviewOptions;
    ivhTreeviewBfs;
    ivhTreeviewMgr;
    dispatcher;
    projectContext;
    projectDataService;

    constructor(dispatcher, projectContext, ivhTreeviewBfs, ivhTreeviewMgr, ivhTreeviewOptions, projectDataService) {
        this.dispatcher = dispatcher;
        this.projectContext = projectContext;
        this.ivhTreeviewBfs = ivhTreeviewBfs;
        this.ivhTreeviewMgr = ivhTreeviewMgr;
        this.projectDataService = projectDataService;
        this.filter = utilities.filter;

        this.treeviewOptions = Object.assign(ivhTreeviewOptions(), {
            childrenAttribute: 'items',
            expandedAttribute: '__expanded',
            idAttribute: 'id',
            labelAttribute: 'name',
            nodeTpl: require('./ngbDataSets.node.tpl.html'),
            selectedAttribute: '__selected',
            validate: false
        });
    }

    async getDatasets() {
        let projects = this.projectContext.datasets;
        if (!this.projectContext.datasetsArePrepared) {
            projects = projects.map(utilities.preprocessNode);
            this.projectContext.datasetsArePrepared = true;
        }
        const datasets = projects;
        ngbDataSetsService.sort(datasets);
        this.ivhTreeviewMgr.validate(datasets, this.treeviewOptions, false);
        await this.updateSelectionFromState(datasets);
        return datasets;
    }

    getSelectedTracks(datasets) {
        const items = [];
        const findSelectedTracksFn = function (item: Node) {
            if (item && item.__selected && item.isTrack) {
                items.push(item);
            }
        };
        this.ivhTreeviewBfs(
            datasets,
            this.treeviewOptions,
            findSelectedTracksFn);
        const reference = items.map(track => track.reference)[0];
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
            this.ivhTreeviewMgr.deselectAll(datasets, this.treeviewOptions);
            return;
        }
        const datasetsIds = this.projectContext.tracksState.reduce((ids, track) => {
            if (ids.filter(t => t === +track.projectId).length === 0) {
                return [...ids, +track.projectId];
            }
            return ids;
        }, []);
        for (let i = 0; i < datasetsIds.length; i++) {
            utilities.expandToProject(datasets, {id: datasetsIds[i]}, this.ivhTreeviewMgr, this.treeviewOptions);
        }
        const mgr = this.ivhTreeviewMgr;
        const opts = this.treeviewOptions;
        const tracksState = this.projectContext.tracksState;
        const updateStateFn = function (item: Node) {
            let selected = false;
            if (item.isTrack && !item.isPlaceholder) {
                selected = tracksState.filter(s => s.bioDataItemId === item.bioDataItemId && s.projectId === item.projectId).length;
            }
            mgr.select(datasets, item, opts, selected);
            if (selected) {
                mgr.expandTo(datasets, item, opts);
            }
            return true;
        };
        this.ivhTreeviewBfs(
            datasets,
            this.treeviewOptions,
            updateStateFn);
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
        utilities.expandNode(item, this.ivhTreeviewMgr, this.treeviewOptions);
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
                utilities.expandNode(item, this.ivhTreeviewMgr, this.treeviewOptions);
                forceReference = utilities.findProjectReference(item);
            } else {
                // we should also select reference:
                if (item.reference) {
                    forceReference = item.reference;
                    item.reference.__selected = true;
                }
            }
            if (forceReference && this.projectContext.reference && forceReference.bioDataItemId !== this.projectContext.reference.bioDataItemId) {
                return false;
            }
        }
        return true;
    }

    deselectItem(item: Node, datasets) {
        item.__selected = false;
        if (item.isProject) {
            item.items.forEach(t => t.__selected = false);
        } else if (item.isTrack && item.project && item.project.items.filter(t => t.format !== 'REFERENCE' && t.__selected).length === 0){
            item.project.items.forEach(t => t.__selected = false);
        }
        this.ivhTreeviewMgr.validate(datasets, this.treeviewOptions, false);
    }

    selectItem(item: Node, isSelected, datasets) {
        if (this.__previousItem === item && item.__previousSelectedState !== undefined &&
            item.__previousSelectedState === isSelected) {
            if (isSelected) {
                this.ivhTreeviewMgr.deselect(datasets, item, this.treeviewOptions);
                isSelected = !isSelected;
            }
        }
        this.__previousItem = item;
        item.__previousSelectedState = isSelected;
        if (isSelected) {
            let forceReference = this.projectContext.reference;
            if (item.isProject) {
                utilities.expandNode(item, this.ivhTreeviewMgr, this.treeviewOptions);
                forceReference = utilities.findProjectReference(item);
            } else {
                // we should also select reference:
                if (item.reference) {
                    forceReference = item.reference;
                    item.reference.__selected = true;
                }
            }
            utilities.selectRecursively(item, isSelected);
            this.ivhTreeviewBfs(
                datasets,
                this.treeviewOptions,
                utilities.updateTracksStateFn(
                    datasets,
                    this.ivhTreeviewMgr,
                    this.treeviewOptions,
                    forceReference
                ));
        } else if (item.isTrack && item.project && item.project.items.filter(t => t.format !== 'REFERENCE' && t.__selected).length === 0) {
            item.project.items.forEach(t => t.__selected = false);
        }
        const {tracks} = this.getSelectedTracks(datasets);
        if (tracks.filter(t => t.format !== 'REFERENCE').length === 0) {
            tracks.forEach(t => t.__selected = false);
        }
        this.ivhTreeviewMgr.validate(datasets, this.treeviewOptions, false);
        this.navigateToTracks(datasets);
    }

    navigateToTracks(datasets) {
        const {tracks} = this.getSelectedTracks(datasets);
        this.__lockStatesUpdate = true;
        const [reference] = tracks.map(track => track.reference);
        if (reference) {
            const tracksState = this.projectContext.tracksState || [];
            if (tracksState.length === 0) {
                tracksState.push({
                    bioDataItemId: reference.bioDataItemId,
                    projectId: reference.projectId
                });
            }
            const tracksIds = tracks.map(track => `[${track.bioDataItemId}][${track.projectId}]`);
            const tracksStateIds = tracksState.map(track => `[${track.bioDataItemId}][${track.projectId}]`);
            const self = this;
            const mapTrackFn = function(track) {
                const state = self.projectContext.getTrackState(track.bioDataItemId, track.projectId);
                if (state) {
                    return state;
                }
                return utilities.mapTrackFn(track);
            };
            let addedTracks = tracks
                .filter(track => tracksStateIds.indexOf(`[${track.bioDataItemId}][${track.projectId}]`) === -1 && track.format !== 'REFERENCE')
                .map(mapTrackFn);
            let existedTracks = tracksState.filter(track => tracksIds.indexOf(`[${track.bioDataItemId}][${track.projectId}]`) >= 0);
            if (!existedTracks.filter(t => t.bioDataItemId === reference.bioDataItemId).length) {
                existedTracks = [utilities.mapTrackFn(reference), ...existedTracks];
            }
            const newTracksState = [...existedTracks, ...addedTracks];
            this.projectContext.changeState({reference: reference, tracks, tracksState: newTracksState});
        } else {
            this.projectContext.changeState({reference: null, tracks: null, tracksState: null});
        }
        this.__lockStatesUpdate = false;
    }


    static search(pattern, datasets) {
        return utilities.search(pattern.toLowerCase(), datasets || []);
    }
}