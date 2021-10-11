export default class ngbHeatmapPanelController {
    static get UID() {
        return 'ngbHeatmapPanelController';
    }

    constructor($scope, appLayout, dispatcher, heatmapContext) {
        this.appLayout = appLayout;
        this.dispatcher = dispatcher;
        this.heatmapContext = heatmapContext;
        this.onHeatmapNavigationCallback = this.onHeatmapNavigation.bind(this);
        this.onHeatmapOptionsChangeCallback = this.onHeatmapOptionsChange.bind(this);
        this.heatmapContext.selectFirst();
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

    get heatmap() {
        return this.heatmapContext.heatmap;
    }

    set heatmap(heatmap) {
        this.heatmapContext.heatmap = heatmap;
        this.heatmapContext.heatmapNavigationOccurred = false;
        this.heatmapContext.refreshHeatmapsList();
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

    onHeatmapNavigation() {
        this.heatmapContext.heatmapNavigationOccurred = true;
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
        }
    }
}
