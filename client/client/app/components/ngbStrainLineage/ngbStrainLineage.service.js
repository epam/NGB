import {mapTrackFn} from '../ngbDataSets/internal/utilities';

const LOCAL_STORAGE_KEY = 'strain-lineage-state';
const MAX_TITLE_LENGTH = 15;
const MAXIMUM_TRACKS_TO_OPEN = 20;

export default class ngbStrainLineageService {

    referenceLineageTrees = [];
    allLineageTrees = [];
    currentReferenceId = null;
    _selectedTreeId = null;

    constructor(genomeDataService, dispatcher) {
        this.dispatcher = dispatcher;
        this.genomeDataService = genomeDataService;
        this.initEvents();
        const savedState = this.loadState();
        this._selectedTreeId = savedState.selectedTree ? savedState.selectedTree.id : null;
    }

    initEvents() {
        // this.dispatcher.on('reference:show:lineage', data => {
        //     this.currentReferenceId = data.referenceId;
        // });
    }

    get selectedTreeId() {
        return this._selectedTreeId;
    }

    set selectedTreeId(value) {
        this._selectedTreeId = value;
        const [selectedTree] = this.getCurrentStrainLineageAsList();
        this.saveState({selectedTree: selectedTree});
    }

    get localStorageKey() {
        return LOCAL_STORAGE_KEY;
    }

    static instance(genomeDataService, dispatcher) {
        return new ngbStrainLineageService(genomeDataService, dispatcher);
    }

    async loadAllStrainLineages() {
        try {
            const data = await this.genomeDataService.getAllLineageTrees();
            return data ? data.map(this._formatCutListToClient) : [];
        } catch (e) {
            return [];
        }
    }

    async loadStrainLineages(referenceId) {
        const treeSortFn = (a, b) => {
            const aName = a.prettyName || a.name || '';
            const bName = b.prettyName || b.name || '';
            return aName.localeCompare(bName);
        };
        if (!this.allLineageTrees.length) {
            this.allLineageTrees = (await this.loadAllStrainLineages()).sort(treeSortFn);
            if (this.selectedTreeId && !this.allLineageTrees.some(tree => tree.id === this.selectedTreeId)) {
                this.selectedTreeId = null;
            }
        }
        if (!referenceId) {
            if (!this.selectedTreeId) {
                this.selectedTreeId = this.allLineageTrees.length ? this.allLineageTrees[0].id : null;
            }
            return {
                allData: this.allLineageTrees,
                referenceData: [],
                error: false
            };
        }
        try {
            const data = await this.genomeDataService.getLineageTreesByReference(referenceId);
            if (data) {
                this.referenceLineageTrees = data
                    .map(this._formatCutListToClient)
                    .sort(treeSortFn);
                if (!this.selectedTreeId) {
                    this.selectedTreeId = this.referenceLineageTrees.length ? this.referenceLineageTrees[0].id : null;
                }
                const filteredAllLineageTrees = this.allLineageTrees
                    .filter(tree => !this.referenceLineageTrees.some(refTree => refTree.id === tree.id))
                    .sort(treeSortFn);
                return {
                    allData: filteredAllLineageTrees,
                    referenceData: this.referenceLineageTrees,
                    error: false
                };
            } else {
                if (!this.selectedTreeId) {
                    this.selectedTreeId = this.allLineageTrees.length ? this.allLineageTrees[0].id : null;
                }
                return {
                    allData: this.allLineageTrees,
                    referenceData: [],
                    error: false
                };
            }
        } catch (e) {
            if (!this.selectedTreeId) {
                this.selectedTreeId = this.allLineageTrees.length ? this.allLineageTrees[0].id : null;
            }
            return {
                allData: this.allLineageTrees,
                referenceData: [],
                error: e.message
            };
        }
    }

    _formatCutListToClient(tree) {
        return {
            id: tree.lineageTreeId,
            name: tree.name,
            prettyName: tree.prettyName,
        };
    }

    _formatTreeToClient(data) {
        const cropTitle = (title, maxLength) =>
            (title && title.length > maxLength) ? `${title.substring(0, maxLength - 1)}...` : title;
        const formatNodes = (node = []) => ({
            data: {
                id: `n_${node.lineageTreeNodeId}`,
                title: node.name,
                fullTitle: node.name,
                description: node.description,
                creationDate: node.creationDate,
                referenceId: node.referenceId,
                projectId: node.projectId,
                tooltip: node.attributes
            }
        });
        const formatEdges = (edge = []) => ({
            data: {
                id: `e_${edge.lineageTreeEdgeId.toString()}`,
                source: `n_${edge.nodeFromId.toString()}`,
                target: `n_${edge.nodeToId.toString()}`,
                label: cropTitle(edge.typeOfInteraction, MAX_TITLE_LENGTH),
                fullLabel: edge.typeOfInteraction,
                tooltip: edge.attributes
            }
        });
        return {
            id: data.lineageTreeId,
            metadata: {
                description: data.description,
                name: data.name,
                prettyName: data.prettyName,
            },
            nodes: data.nodes.map(formatNodes),
            edges: data.edges.map(formatEdges)
        };
    }

    async getLineageTreeById(id) {
        try {
            const data = await this.genomeDataService.getLineageTreeById(id);
            if (data) {
                return {
                    tree: this._formatTreeToClient(data),
                    error: false
                };
            } else {
                return {
                    tree: null,
                    error: false
                };
            }
        } catch (e) {
            return {
                tree: null,
                error: e.message
            };
        }
    }

    getCurrentStrainLineageAsList() {
        return this.allLineageTrees.filter(tree => tree.id === this.selectedTreeId);
    }

    getOpenReferencePayload(context, referenceObj) {
        if (referenceObj && context.datasets) {
            // we'll open first dataset of this reference
            const tree = context.datasets || [];
            const find = (items = []) => {
                const projects = items.filter(item => item.isProject);
                const [dataset] = projects.filter(item => item.reference && item.reference.id === referenceObj.id);
                if (dataset) {
                    return dataset;
                }
                for (const project of projects) {
                    const nested = find(project.nestedProjects);
                    if (nested) {
                        return nested;
                    }
                }
                return null;
            };
            const dataset = find(tree);
            if (dataset) {
                const tracks = [dataset.reference];
                const tracksState = [mapTrackFn(dataset.reference)];
                return {
                    tracks,
                    tracksState,
                    reference: dataset.reference,
                    shouldAddAnnotationTracks: true
                };
            }
        }
        return null;
    }

    getOpenDatasetPayload(tree, projectId) {
        const find = (items = []) => {
            const projects = items.filter(item => item.isProject);
            let match = projects.filter(item => item.id === projectId);
            for (const project of projects) {
                match = match.concat(find(project.nestedProjects));
            }
            return match;
        };
        const raw = find(tree);
        const [referenceDataset] = raw
            .filter(dataset => dataset.reference);
        if (referenceDataset) {
            const reference = referenceDataset.reference;
            const datasetsToNavigate = raw
                .filter(dataset => dataset.reference && dataset.reference.id === reference.id);
            let tracks = [reference];
            let tracksState = [mapTrackFn(reference)];
            const addDatasetItems = (dataset) => {
                const items = dataset._lazyItems || dataset.items;
                for (const item of items) {
                    if (item.isProject) {
                        addDatasetItems(item);
                    } else if (item.format !== 'REFERENCE') {
                        tracks.push(item);
                        tracksState.push(mapTrackFn(item));
                    }
                }
            };
            for (const dataset of datasetsToNavigate) {
                addDatasetItems(dataset);
            }
            tracks = tracks.slice(0, MAXIMUM_TRACKS_TO_OPEN);
            tracksState = tracksState.slice(0, MAXIMUM_TRACKS_TO_OPEN);
            return {
                tracks,
                tracksState,
                reference
            };
        }
        return null;
    }


    saveState(state) {
        const savedState = JSON.parse(localStorage.getItem(this.localStorageKey) || '{}');
        localStorage.setItem(this.localStorageKey, JSON.stringify({...savedState, ...state}));
    }

    loadState() {
        return JSON.parse(localStorage.getItem(this.localStorageKey) || '{}');
    }

    recoverLocalState(state) {
        if (state) {
            const {
                selectedTree,
                layout = {}
            } = state;
            const {
                layout: currentLayout = {}
            } = this.loadState() || {};
            const newLayout = Object.assign(currentLayout, layout);
            this.saveState(
                {
                    selectedTree,
                    layout: newLayout
                }
            );
            this._selectedTreeId = selectedTree ? selectedTree.id : null;
        }
    }
}
