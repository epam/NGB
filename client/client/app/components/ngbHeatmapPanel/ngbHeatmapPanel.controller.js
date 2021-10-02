export default class ngbHeatmapPanelController {
    static get UID() {
        return 'ngbHeatmapPanelController';
    }
    constructor($scope, ngbHeatmapPanelService) {
        this.service = ngbHeatmapPanelService;
        this.onHeatmapNavigationCallback = this.onHeatmapNavigation.bind(this);
    }

    get loading() {
        return !this.service || this.service.loading;
    }

    get heatmaps() {
        return this.service.heatmaps;
    }

    get heatmap() {
        return this.service.selectedHeatmap;
    }

    set heatmap(heatmap) {
        this.service.selectedHeatmap = heatmap;
        this.service.heatmapNavigationOccurred = false;
        this.service.refreshHeatmapsList();
    }

    get heatmapId() {
        if (this.heatmap) {
            return this.heatmap.id;
        }
        return undefined;
    }

    get heatmapProjectId() {
        if (this.heatmap) {
            return this.heatmap.project ? this.heatmap.project.id : this.heatmap.projectIdNumber;
        }
        return undefined;
    }

    onHeatmapNavigation() {
        this.service.heatmapNavigationOccurred = true;
    }
}
