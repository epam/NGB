export default class ngbHeatmapPanelService {
    static get UID() {
        return 'ngbHeatmapPanelService';
    }
    static instance(projectContext, dispatcher) {
        return new ngbHeatmapPanelService(projectContext, dispatcher);
    }

    constructor(projectContext, dispatcher) {
        this.projectContext = projectContext;
        this.refreshReferencesCallback = this.refreshHeatmapsList.bind(this);
        this.heatmaps = [];
        this.selectedHeatmap = undefined;
        this.initialized = false;
        this.heatmapNavigationOccurred = false;
        dispatcher.on('ngb:init:finished', this.refreshReferencesCallback);
        dispatcher.on('tracks:state:change', this.refreshReferencesCallback);
        this.refreshHeatmapsList();
    }

    get loading() {
        return !this.initialized || !this.projectContext || this.projectContext._referencesAreLoading;
    }

    refreshHeatmapsList() {
        const references = this.projectContext
            ? (this.projectContext.references || [])
            : [];
        const tracks = this.projectContext
            ? (this.projectContext.tracks || [])
            : [];
        const getTrackReference = track => track.format === 'REFERENCE' ? track.id : track.referenceId;
        const tracksReferences = Array.from(new Set(tracks.map(getTrackReference).filter(Boolean)));
        const items = references
            .filter(reference => tracksReferences.includes(reference.id))
            .map(r => (r.annotationFiles || []))
            .reduce((r, c) => ([...r, ...c]), [])
            .concat(tracks)
            .filter(track => track.format === 'HEATMAP');
        const uniqueIdentifiers = Array.from(new Set(items.map(item => item.id)));
        const heatmaps = uniqueIdentifiers
            .map(id => items.filter(track => track.id === id).pop())
            .filter(Boolean);
        if (this.selectedHeatmap) {
            const exists = heatmaps.filter(track => track.id === this.selectedHeatmap.id).length > 0;
            if (!exists && this.heatmapNavigationOccurred) {
                heatmaps.push(this.selectedHeatmap);
            } else if (!exists) {
                this.selectedHeatmap = undefined;
                this.heatmapNavigationOccurred = false;
            }
        }
        this.heatmaps = heatmaps;
        if (!this.selectedHeatmap && this.heatmaps.length > 0) {
            this.selectedHeatmap = this.heatmaps[0];
        }
        this.initialized = true;
    }
}
