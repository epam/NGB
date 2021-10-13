function sortByName(a, b) {
    if (a && b && a.name && b.name) {
        const nameA = (a.prettyName || a.name).toLowerCase();
        const nameB = (b.prettyName || b.name).toLowerCase();
        return nameA >  nameB ? 1 : -1;
    }
    return 0;
}
export default class ngbHeatmapPanelController {
    static get UID() {
        return 'ngbHeatmapPanelController';
    }
    _sortedHeatMaps = {};

    constructor($scope, appLayout, dispatcher, heatmapContext) {
        this.appLayout = appLayout;
        this.dispatcher = dispatcher;
        this.heatmapContext = heatmapContext;
        this.onHeatmapNavigationCallback = this.onHeatmapNavigation.bind(this);
        this.onHeatmapOptionsChangeCallback = this.onHeatmapOptionsChange.bind(this);
        this.heatmapContext.selectFirst();
        this.sortedHeatMaps = this.sortHeatmaps(this.heatmaps);
        $scope.$on('$destroy', () => {
            this.heatmapContext.heatmap = undefined;
        });
    }

    get loading() {
        return !this.heatmapContext || !this.heatmapContext.initialized;
    }

    get heatmaps() {
        return this.heatmapContext.heatmaps || [];
    }

    sortHeatmaps(heatmaps) {
        if (heatmaps) {
            let selectedAnnotation, selectedTrack;
            const formattedHeatmaps = heatmaps.reduce((sortedHeatMaps, file) => {
                const [annotation] = this.annotationFiles.filter(item => item.id === file.id && item.format === file.format);
                const isSelectedAnnotation = annotation && annotation.id === this.heatmapId;
                const isSelectedTrack = !annotation && file.id === this.heatmapId;
                const nonSelectedAnnotation = annotation && annotation.id !== this.heatmapId;
                const nonSelectedTrack = !annotation && file.id !== this.heatmapId;
                if (nonSelectedAnnotation) {
                    sortedHeatMaps.annotations.push(annotation);
                } else if (nonSelectedTrack){
                    sortedHeatMaps.tracks.push(file);
                } else if (isSelectedTrack){
                    selectedTrack = file;
                } else if (isSelectedAnnotation) {
                    selectedAnnotation = annotation;
                }
                return sortedHeatMaps;
            }, {tracks: [], annotations: []});
            const annotations = formattedHeatmaps.annotations.sort(sortByName);
            const tracks = formattedHeatmaps.tracks.sort(sortByName);
            formattedHeatmaps.annotations = selectedAnnotation ? [selectedAnnotation, ...annotations] : annotations;
            formattedHeatmaps.tracks = selectedTrack ? [selectedTrack, ...tracks] : tracks;
            return formattedHeatmaps;
        } else {
            return {};
        }
    }

    get heatmap() {
        return this.heatmapContext.heatmap;
    }
    get sortedHeatMaps() {
        return this._sortedHeatMaps;
    }

    set sortedHeatMaps(heatmaps) {
        this._sortedHeatMaps = heatmaps;
    }

    set heatmap(heatmap) {
        this.heatmapContext.heatmap = heatmap;
        this.heatmapContext.heatmapNavigationOccurred = false;
        this.heatmapContext.refreshHeatmapsList();
        this.sortedHeatMaps = this.sortHeatmaps(this.heatmaps);
    }

    get heatmapId() {
        if (this.heatmap) {
            return this.heatmap.id;
        }
        return undefined;
    }

    set heatmapId(heatmapId) {
        const [heatmap] = this.heatmaps.filter(h => h.id === heatmapId);
        this.heatmap = heatmap;
    }

    get heatmapProjectId() {
        if (this.heatmap) {
            return this.heatmap.project ? this.heatmap.project.id : this.heatmap.projectIdNumber;
        }
        return undefined;
    }

    get heatmapReferenceId() {
        if (this.heatmap) {
            return this.heatmap.referenceId;
        }
        return undefined;
    }

    get annotationFiles() {
        if (
            this.heatmapContext &&
            this.heatmapContext.projectContext &&
            this.heatmapContext.projectContext.reference
        ) {
            return this.heatmapContext.projectContext.reference.annotationFiles;
        }
        return [];
    }

    onHeatmapNavigation() {
        this.heatmapContext.heatmapNavigationOccurred = true;
        this.sortedHeatMaps = this.sortHeatmaps(this.heatmaps);
    }

    /**
     *
     * @param {HeatmapViewOptions} options
     */
    onHeatmapOptionsChange(options) {
        if (
            options &&
            this.heatmapContext &&
            this.heatmapContext.heatmap &&
            options.dataConfig &&
            Number(options.dataConfig.id) === Number(this.heatmapContext.heatmap.id)
        ) {
            this.heatmapContext.setCurrentState(options.serialize());
            this.sortedHeatMaps = this.sortHeatmaps(this.heatmaps);
        }
    }
}
