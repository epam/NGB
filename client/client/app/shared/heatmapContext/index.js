import {storeHeatmapState} from '../../../modules/render/heatmap';

export default class HeatmapContext {
    static instance(projectContext, dispatcher) {
        return new HeatmapContext(projectContext, dispatcher);
    }

    constructor(projectContext, dispatcher) {
        this.projectContext = projectContext;
        this.dispatcher = dispatcher;
        this.refreshReferencesCallback = this.refreshHeatmapsList.bind(this);
        this.heatmaps = [];
        this.selectedHeatmap = undefined;
        this.currentState = undefined;
        this.contextInitialized = false;
        this.heatmapNavigationOccurred = false;
        dispatcher.on('ngb:init:finished', this.refreshReferencesCallback);
        dispatcher.on('tracks:state:change', this.refreshReferencesCallback);
        this.refreshHeatmapsList();
    }

    get initialized() {
        return this.contextInitialized && this.projectContext && !this.projectContext._referencesAreLoading;
    }

    selectFirst () {
        if (this.initialized && !this.selectedHeatmap && this.heatmaps.length > 0) {
            this.heatmap = this.heatmaps[0];
        }
    }

    refreshHeatmapsList() {
        if (!this.projectContext || this.projectContext._referencesAreLoading) {
            return;
        }
        const references = this.projectContext
            ? (this.projectContext.references || [])
            : [];
        const tracks = this.projectContext
            ? (this.projectContext.tracks || [])
            : [];
        const referenceId = this.projectContext && this.projectContext.reference
            ? this.projectContext.reference.id
            : undefined;
        const heatmapsFromTracks = tracks
            .filter(track => track.format === 'HEATMAP')
            .map(track => ({...track, referenceId, isTrack: true}));
        const heatmapsFromAnnotations = references
            .filter(reference => reference.id === referenceId)
            .map(r => (r.annotationFiles || []))
            .reduce((r, c) => ([...r, ...c]), [])
            .filter(track => track.format === 'HEATMAP')
            .map(track => ({...track, referenceId, isAnnotation: true}));
        const items = heatmapsFromTracks.concat(heatmapsFromAnnotations);
        const uniqueIdentifiers = Array.from(new Set(items.map(item => item.id)));
        const heatmaps = uniqueIdentifiers
            .map(id => items.filter(track => track.id === id).pop())
            .filter(Boolean);
        if (this.selectedHeatmap) {
            const exists = heatmaps.filter(track => track.id === this.selectedHeatmap.id);
            if (exists.length === 0 && this.heatmapNavigationOccurred) {
                heatmaps.push(this.selectedHeatmap);
            } else if (exists.length === 0) {
                this.selectedHeatmap = undefined;
                this.currentState = undefined;
                this.heatmapNavigationOccurred = false;
                this.report();
            } else {
                this.selectedHeatmap = exists.pop();
            }
        }
        this.heatmaps = heatmaps;
        if (!this.selectedHeatmap && this.heatmaps.length > 0) {
            this.selectedHeatmap = this.heatmaps[0];
            this.currentState = undefined;
            this.report();
        }
        this.contextInitialized = true;
    }

    get heatmap() {
        return this.selectedHeatmap;
    }

    set heatmap(heatmap) {
        if (this.selectedHeatmap !== heatmap) {
            this.selectedHeatmap = heatmap;
            this.currentState = undefined;
            this.report();
        }
    }

    get routeInfo () {
        if (this.selectedHeatmap) {
            return JSON.stringify({
                id: this.selectedHeatmap.id,
                state: this.currentState
            });
        }
        return null;
    }

    set routeInfo (routeInfo) {
        const reset = () => {
            this.selectedHeatmap = undefined;
            this.currentState = undefined;
        };
        try {
            if (routeInfo) {
                const {
                    id,
                    state
                } = JSON.parse(routeInfo);
                this.selectedHeatmap = {id};
                this.currentState = state;
                if (state) {
                    storeHeatmapState(id, state);
                }
                this.heatmapNavigationOccurred = true;
                this.dispatcher.emit('heatmap:options:update');
                this.refreshHeatmapsList();
            } else {
                reset();
            }
        } catch (_) {
            reset();
        }
    }

    setCurrentState(state) {
        this.currentState = state;
        this.report();
    }

    report() {
        this.dispatcher.emitGlobalEvent('route:change');
    }
}
