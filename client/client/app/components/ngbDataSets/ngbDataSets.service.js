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
        const project = items.map(track => track.project)[0];
        const reference = items.map(track => track.reference)[0];
        return {
            project,
            reference,
            tracks: items
        };
    }

    async updateSelectionFromState(datasets) {
        if (!datasets || this.__lockStatesUpdate) {
            return;
        }
        let activeTracksIds = [];
        const projectId = this.projectContext.project ? this.projectContext.project.id : null;
        if (!projectId) {
            return;
        }

        activeTracksIds = (this.projectContext
            .getActiveTracks())
            .map(track => track.bioDataItemId);
        utilities.expandToProject(datasets, {id: projectId}, this.ivhTreeviewMgr, this.treeviewOptions);
        const mgr = this.ivhTreeviewMgr;
        const opts = this.treeviewOptions;
        const updateStateFn = function (item: Node) {
            const selected = !item.isPlaceholder && item.isTrack &&
                item.project.id === projectId &&
                activeTracksIds.indexOf(item.bioDataItemId) >= 0;
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
            const projectsPath = [];
            if (item.isProject) {
                utilities.expandNode(item, this.ivhTreeviewMgr, this.treeviewOptions);
                let buffer = item;
                while (buffer) {
                    projectsPath.push(buffer.id);
                    buffer = buffer.project;
                }
            } else {
                // we should also select reference:
                if (item.reference) {
                    item.reference.__selected = true;
                }
                let buffer = item.project;
                while (buffer) {
                    projectsPath.push(buffer.id);
                    buffer = buffer.project;
                }
            }
            this.ivhTreeviewBfs(
                datasets,
                this.treeviewOptions,
                utilities.updateTracksStateFn(
                    datasets,
                    this.ivhTreeviewMgr,
                    this.treeviewOptions,
                    projectsPath
                ));
        }
        const {tracks} = this.getSelectedTracks(datasets);
        if (tracks.length === 1 && tracks[0].format === 'REFERENCE') {
            // only reference is selected - we should deselect all project
            tracks[0].__selected = false;
            this.ivhTreeviewMgr.validate(tracks[0].project, this.treeviewOptions);
        }
        this.navigateToTracks(datasets);
    }

    navigateToTracks(datasets) {
        const {project, tracks} = this.getSelectedTracks(datasets);
        this.__lockStatesUpdate = true;
        if (project) {
            const tracksState = (this.projectContext.getTracksState(project.id) || []);
            if (tracksState.length === 0) {
                const reference = project.reference;
                tracksState.push({
                    bioDataItemId: reference.bioDataItemId
                });
            }
            const tracksIds = tracks.map(track => track.bioDataItemId);
            const tracksStateIds = tracksState.filter(track => !track.hidden).map(track => track.bioDataItemId);
            const addedTracks = tracks
                .filter(track => tracksStateIds.indexOf(track.bioDataItemId) === -1)
                .map(utilities.mapVisibleTrackFn);
            const removedTracks = tracksState
                .filter(track => tracksIds.indexOf(track.bioDataItemId) === -1)
                .map(utilities.mapInvisibleTrackFn);
            const existedTracks = tracksState.filter(track => !track.hidden && tracksIds.indexOf(track.bioDataItemId) >= 0);
            const newTracksState = [...existedTracks, ...addedTracks, ...removedTracks];
            const projectInstance = Object.assign({}, project);
            projectInstance.items = project.items.filter(item => item.isTrack);
            projectInstance.loaded = true;
            this.projectContext.changeState({project: projectInstance, tracksState: newTracksState});
        } else {
            this.projectContext.changeState({project: null});
        }
        this.__lockStatesUpdate = false;
    }


    static search(pattern, datasets) {
        return utilities.search(pattern.toLowerCase(), datasets || []);
    }
}